/*
 * Copyright 2023 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modified by Oyoon (2026): camera setup adapted from CameraFragment in
 * google-ai-edge/mediapipe-samples examples/face_landmarker/android. Merged
 * into a single Activity; removed navigation, settings sheet, blendshapes
 * list and back camera; uses ResolutionSelector instead of the deprecated
 * target aspect ratio; added debug controls for analysis resolution, landmark
 * labels, FPS and iris width.
 */
package oyoon.app

import android.Manifest
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.util.Size
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import oyoon.app.landmarker.FaceLandmarkerHelper
import oyoon.app.landmarker.OverlayView
import oyoon.core.geometry.Point2
import oyoon.core.measure.irisDiameterPxA
import oyoon.core.measure.irisDiameterPxB
import oyoon.core.model.FrameSample
import oyoon.core.model.Landmarks
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : ComponentActivity(), FaceLandmarkerHelper.LandmarkerListener {

    private lateinit var viewFinder: PreviewView
    private lateinit var overlay: OverlayView
    private lateinit var status: TextView
    private lateinit var resolutionButtons: Map<AnalysisResolution, Button>
    private lateinit var labelsButton: Button

    /** Single thread that creates, runs and closes the landmarker (needed for GPU). */
    private lateinit var backgroundExecutor: ExecutorService
    private lateinit var faceLandmarkerHelper: FaceLandmarkerHelper
    private var cameraProvider: ProcessCameraProvider? = null

    /** Debug overlay (FPS, resolution, iris px, controls) only in debuggable builds. */
    private var isDebugBuild = false

    // Read on the MediaPipe result thread, written on the main thread.
    @Volatile private var resolution = AnalysisResolution.VGA
    @Volatile private var actualResolution = "?"
    private val fpsCounter = FpsCounter()

    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startCamera() else status.setText(R.string.camera_permission_needed)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        viewFinder = findViewById(R.id.view_finder)
        overlay = findViewById(R.id.overlay)
        status = findViewById(R.id.status)
        isDebugBuild = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

        savedInstanceState?.let {
            resolution = AnalysisResolution.entries[it.getInt(KEY_RESOLUTION, 0)]
            overlay.showLabels = it.getBoolean(KEY_LABELS, false)
        }

        applyWindowInsets()
        if (isDebugBuild) setUpDebugControls()

        backgroundExecutor = Executors.newSingleThreadExecutor()
        faceLandmarkerHelper = FaceLandmarkerHelper(applicationContext, this)

        if (hasCameraPermission()) {
            startCamera()
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_RESOLUTION, resolution.ordinal)
        outState.putBoolean(KEY_LABELS, overlay.showLabels)
    }

    override fun onResume() {
        super.onResume()
        backgroundExecutor.execute { faceLandmarkerHelper.setup() }
    }

    override fun onPause() {
        super.onPause()
        backgroundExecutor.execute { faceLandmarkerHelper.close() }
        overlay.clear()
    }

    override fun onDestroy() {
        super.onDestroy()
        backgroundExecutor.shutdown()
    }

    /** Keeps the status label and the bottom bar clear of system bars and cutouts. */
    private fun applyWindowInsets() {
        val baseMargin = (16 * resources.displayMetrics.density).toInt()
        val controls = findViewById<View>(R.id.debug_controls)
        val controlsPadding = controls.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { _, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            status.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = bars.top + baseMargin
                leftMargin = bars.left + baseMargin
            }
            controls.updatePadding(bottom = bars.bottom + controlsPadding)
            insets
        }
    }

    private fun setUpDebugControls() {
        findViewById<View>(R.id.debug_controls).visibility = View.VISIBLE
        resolutionButtons = mapOf(
            AnalysisResolution.VGA to findViewById(R.id.res_640),
            AnalysisResolution.HD to findViewById(R.id.res_1280),
            AnalysisResolution.FULL_HD to findViewById(R.id.res_1920),
        )
        resolutionButtons.forEach { (option, button) ->
            button.setOnClickListener { selectResolution(option) }
        }
        labelsButton = findViewById(R.id.toggle_labels)
        labelsButton.setOnClickListener {
            overlay.showLabels = !overlay.showLabels
            highlightSelection()
        }
        highlightSelection()
    }

    private fun highlightSelection() {
        resolutionButtons.forEach { (option, button) ->
            button.alpha = if (option == resolution) 1f else DIMMED_ALPHA
        }
        labelsButton.alpha = if (overlay.showLabels) 1f else DIMMED_ALPHA
    }

    private fun selectResolution(option: AnalysisResolution) {
        if (option == resolution) return
        resolution = option
        highlightSelection()
        fpsCounter.reset()
        cameraProvider?.let { bindCameraUseCases(it) }
    }

    private fun hasCameraPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED

    private fun startCamera() {
        if (cameraProvider != null) return
        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({
            val provider = providerFuture.get()
            cameraProvider = provider
            bindCameraUseCases(provider)
        }, ContextCompat.getMainExecutor(this))
    }

    /** (Re)binds preview + analysis for the current [resolution]. */
    private fun bindCameraUseCases(cameraProvider: ProcessCameraProvider) {
        // Preview and analysis share one aspect ratio so they show the same field
        // of view; otherwise the overlay would not line up with the preview.
        val aspectRatioStrategy =
            AspectRatioStrategy(resolution.aspectRatio, AspectRatioStrategy.FALLBACK_RULE_AUTO)

        // The bound size is given in display orientation, so swap it in portrait.
        val portrait = resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        val boundSize =
            if (portrait) Size(resolution.height, resolution.width)
            else Size(resolution.width, resolution.height)

        val preview = Preview.Builder()
            .setResolutionSelector(
                ResolutionSelector.Builder().setAspectRatioStrategy(aspectRatioStrategy).build()
            )
            .build()
            .also { it.surfaceProvider = viewFinder.surfaceProvider }

        val imageAnalysis = ImageAnalysis.Builder()
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setAspectRatioStrategy(aspectRatioStrategy)
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            boundSize,
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                        )
                    )
                    .build()
            )
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
            .also {
                it.setAnalyzer(backgroundExecutor) { image ->
                    faceLandmarkerHelper.detectLiveStream(image, isFrontCamera = true)
                }
            }

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                this, CameraSelector.DEFAULT_FRONT_CAMERA, preview, imageAnalysis
            )
            // Buffer size as delivered by the sensor (landscape), e.g. 640x480.
            actualResolution = imageAnalysis.resolutionInfo?.resolution
                ?.let { "${it.width}×${it.height}" } ?: "?"
            overlay.clear()
        } catch (e: Exception) {
            Log.e(TAG, "Camera binding failed", e)
            status.text = getString(R.string.landmarker_failed, e.message ?: "camera unavailable")
        }
    }

    override fun onResults(resultBundle: FaceLandmarkerHelper.ResultBundle) {
        val fps = fpsCounter.tick()
        val debugText = if (isDebugBuild) debugStatus(resultBundle, fps) else null
        runOnUiThread {
            overlay.setResults(
                resultBundle.result,
                resultBundle.inputImageWidth,
                resultBundle.inputImageHeight,
            )
            if (debugText != null) status.text = debugText
        }
    }

    override fun onEmpty() {
        val fps = fpsCounter.tick()
        val debugText = if (isDebugBuild) debugStatus(null, fps) else null
        runOnUiThread {
            overlay.clear()
            if (debugText != null) status.text = debugText
        }
    }

    override fun onError(error: String) {
        Log.e(TAG, error)
        runOnUiThread { status.text = getString(R.string.landmarker_failed, error) }
    }

    /** Builds the 3-line debug label: timing, resolution, iris width. */
    private fun debugStatus(bundle: FaceLandmarkerHelper.ResultBundle?, fps: Float): String {
        val delegate = faceLandmarkerHelper.delegate?.name ?: "?"
        val timing = getString(R.string.debug_timing, delegate, bundle?.inferenceTimeMs ?: 0L, fps)
        val res = getString(
            R.string.debug_resolution, resolution.width, resolution.height, actualResolution
        )
        val face = bundle?.result?.faceLandmarks()?.firstOrNull()
        val iris = if (face != null && face.size >= Landmarks.COUNT) {
            // Same pixel conversion and iris formula :core uses for measurement.
            val sample = FrameSample.fromNormalized(
                face.map { Point2(it.x().toDouble(), it.y().toDouble()) },
                bundle.inputImageWidth,
                bundle.inputImageHeight,
            )
            val a = irisDiameterPxA(sample)
            val b = irisDiameterPxB(sample)
            getString(R.string.debug_iris, a, b, (a + b) / 2.0)
        } else {
            getString(R.string.debug_no_face)
        }
        return "$timing\n$res\n$iris"
    }

    /** Analysis resolutions selectable from the debug bar, in landscape (sensor) terms. */
    private enum class AnalysisResolution(val width: Int, val height: Int, val aspectRatio: Int) {
        VGA(640, 480, AspectRatio.RATIO_4_3),
        HD(1280, 720, AspectRatio.RATIO_16_9),
        FULL_HD(1920, 1080, AspectRatio.RATIO_16_9),
    }

    /** Processed frames per second, averaged over roughly one-second windows. */
    private class FpsCounter {
        private var windowStart = 0L
        private var frames = 0
        private var fps = 0f

        @Synchronized
        fun tick(): Float {
            val now = SystemClock.elapsedRealtime()
            if (windowStart == 0L) windowStart = now
            frames++
            val elapsed = now - windowStart
            if (elapsed >= 1000) {
                fps = frames * 1000f / elapsed
                frames = 0
                windowStart = now
            }
            return fps
        }

        @Synchronized
        fun reset() {
            windowStart = 0L
            frames = 0
            fps = 0f
        }
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val KEY_RESOLUTION = "analysis_resolution"
        private const val KEY_LABELS = "show_labels"
        private const val DIMMED_ALPHA = 0.45f
    }
}
