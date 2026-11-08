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
 * target aspect ratio; UI moved to Compose with a validity-gated live PD and
 * debug controls for analysis resolution, landmark labels, FPS and iris width.
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
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import oyoon.app.landmarker.FaceLandmarkerHelper
import oyoon.app.landmarker.FrameSampler
import oyoon.app.live.LiveMeasurer
import oyoon.app.ui.AnalysisResolution
import oyoon.app.ui.ChipStatus
import oyoon.app.ui.MeasureScreen
import oyoon.app.ui.MeasureUiState
import oyoon.core.gate.GateResult
import oyoon.core.geometry.HeadPose
import oyoon.core.measure.irisDiameterPxA
import oyoon.core.measure.irisDiameterPxB
import oyoon.core.model.FrameSample
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : ComponentActivity(), FaceLandmarkerHelper.LandmarkerListener {

    private lateinit var previewView: PreviewView

    /** Single thread that creates, runs and closes the landmarker (needed for GPU). */
    private lateinit var backgroundExecutor: ExecutorService
    private lateinit var faceLandmarkerHelper: FaceLandmarkerHelper
    private var cameraProvider: ProcessCameraProvider? = null

    /** Debug tools (FPS, resolution, iris px, labels) only in debuggable builds. */
    private var isDebugBuild = false

    // Compose state, written on the main thread only.
    private var uiState by mutableStateOf(MeasureUiState())
    private var showLabels by mutableStateOf(false)
    private var resolutionState by mutableStateOf(AnalysisResolution.VGA)

    // Read on the MediaPipe result thread, written on the main thread.
    @Volatile private var resolution = AnalysisResolution.VGA
    @Volatile private var actualResolution = "?"
    private val fpsCounter = FpsCounter()

    /** Gate + live PD. Only touched on the MediaPipe result thread. */
    private val liveMeasurer = LiveMeasurer()
    /** Set on the main thread to ask the result thread to clear [liveMeasurer]. */
    @Volatile private var resetMeasurer = false

    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startCamera()
            else uiState = uiState.copy(message = getString(R.string.camera_permission_needed))
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        isDebugBuild = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        previewView = PreviewView(this)

        savedInstanceState?.let {
            resolution = AnalysisResolution.entries[it.getInt(KEY_RESOLUTION, 0)]
            resolutionState = resolution
            showLabels = it.getBoolean(KEY_LABELS, false)
        }

        setContent {
            MeasureScreen(
                state = uiState,
                previewView = previewView,
                isDebugBuild = isDebugBuild,
                showLabels = showLabels,
                resolution = resolutionState,
                onResolutionSelected = ::selectResolution,
                onToggleLabels = { showLabels = !showLabels },
            )
        }

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
        outState.putBoolean(KEY_LABELS, showLabels)
    }

    override fun onResume() {
        super.onResume()
        backgroundExecutor.execute { faceLandmarkerHelper.setup() }
    }

    override fun onPause() {
        super.onPause()
        backgroundExecutor.execute { faceLandmarkerHelper.close() }
        resetMeasurer = true
        uiState = uiState.copy(face = null, status = ChipStatus.Searching)
    }

    override fun onDestroy() {
        super.onDestroy()
        backgroundExecutor.shutdown()
    }

    private fun selectResolution(option: AnalysisResolution) {
        if (option == resolution) return
        resolution = option
        resolutionState = option
        fpsCounter.reset()
        // Iris sizes change with resolution, so old history and PD frames no longer apply.
        resetMeasurer = true
        uiState = uiState.copy(face = null, status = ChipStatus.Searching, pdMm = null)
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
            .also { it.surfaceProvider = previewView.surfaceProvider }

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
        } catch (e: Exception) {
            Log.e(TAG, "Camera binding failed", e)
            uiState = uiState.copy(
                message = getString(R.string.landmarker_failed, e.message ?: "camera unavailable")
            )
        }
    }

    override fun onResults(resultBundle: FaceLandmarkerHelper.ResultBundle) {
        if (resetMeasurer) {
            liveMeasurer.reset()
            resetMeasurer = false
        }
        val sample = FrameSampler.from(resultBundle)?.takeIf { it.transformMatrix != null }
        val update = sample?.let(liveMeasurer::process)
        val fps = fpsCounter.tick()
        val debugLines = if (isDebugBuild) debugLines(resultBundle, sample, fps) else emptyList()
        val status = when (val gate = update?.gate) {
            null -> ChipStatus.Searching
            GateResult.Accepted -> ChipStatus.Accepted
            is GateResult.Rejected -> ChipStatus.Rejected(gate.reason)
        }
        runOnUiThread {
            uiState = uiState.copy(
                face = resultBundle.result,
                imageWidth = resultBundle.inputImageWidth,
                imageHeight = resultBundle.inputImageHeight,
                status = status,
                // Only accepted frames change the PD; otherwise keep the last value.
                pdMm = update?.pdMm ?: uiState.pdMm,
                debugLines = debugLines,
            )
        }
    }

    override fun onEmpty() {
        val fps = fpsCounter.tick()
        val debugLines = if (isDebugBuild) debugLines(null, null, fps) else emptyList()
        runOnUiThread {
            uiState = uiState.copy(face = null, status = ChipStatus.Searching, debugLines = debugLines)
        }
    }

    override fun onError(error: String) {
        Log.e(TAG, error)
        runOnUiThread { uiState = uiState.copy(message = getString(R.string.landmarker_failed, error)) }
    }

    /** Debug text: timing, resolution, iris width, head pose and brightness. */
    private fun debugLines(
        bundle: FaceLandmarkerHelper.ResultBundle?,
        sample: FrameSample?,
        fps: Float,
    ): List<String> {
        val delegate = faceLandmarkerHelper.delegate?.name ?: "?"
        val lines = mutableListOf(
            getString(R.string.debug_timing, delegate, bundle?.inferenceTimeMs ?: 0L, fps),
            getString(R.string.debug_resolution, resolution.width, resolution.height, actualResolution),
        )
        if (sample == null) {
            lines += getString(R.string.debug_no_face)
        } else {
            val a = irisDiameterPxA(sample)
            val b = irisDiameterPxB(sample)
            lines += getString(R.string.debug_iris, a, b, (a + b) / 2.0)
            sample.transformMatrix?.let(HeadPose::fromMatrix)?.let { pose ->
                lines += getString(
                    R.string.debug_pose, pose.yawDeg, pose.pitchDeg, pose.rollDeg, sample.brightness ?: Double.NaN
                )
            }
        }
        return lines
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
    }
}
