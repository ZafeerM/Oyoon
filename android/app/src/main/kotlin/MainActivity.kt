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
 * target aspect ratio.
 */
package oyoon.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import oyoon.app.landmarker.FaceLandmarkerHelper
import oyoon.app.landmarker.OverlayView
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : ComponentActivity(), FaceLandmarkerHelper.LandmarkerListener {

    private lateinit var viewFinder: PreviewView
    private lateinit var overlay: OverlayView
    private lateinit var status: TextView

    /** Single thread that creates, runs and closes the landmarker (needed for GPU). */
    private lateinit var backgroundExecutor: ExecutorService
    private lateinit var faceLandmarkerHelper: FaceLandmarkerHelper
    private var cameraBound = false

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

        // Keep the status label below the status bar / camera cutout.
        val baseMargin = (16 * resources.displayMetrics.density).toInt()
        ViewCompat.setOnApplyWindowInsetsListener(status) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = bars.top + baseMargin
                leftMargin = bars.left + baseMargin
            }
            insets
        }

        backgroundExecutor = Executors.newSingleThreadExecutor()
        faceLandmarkerHelper = FaceLandmarkerHelper(applicationContext, this)

        if (hasCameraPermission()) {
            startCamera()
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
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

    private fun hasCameraPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED

    private fun startCamera() {
        if (cameraBound) return
        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({
            bindCameraUseCases(providerFuture.get())
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraUseCases(cameraProvider: ProcessCameraProvider) {
        // 4:3 is closest to what the face model expects.
        val resolutionSelector = ResolutionSelector.Builder()
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
            .build()

        val preview = Preview.Builder()
            .setResolutionSelector(resolutionSelector)
            .build()
            .also { it.surfaceProvider = viewFinder.surfaceProvider }

        val imageAnalysis = ImageAnalysis.Builder()
            .setResolutionSelector(resolutionSelector)
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
            cameraBound = true
        } catch (e: Exception) {
            Log.e(TAG, "Camera binding failed", e)
            status.text = getString(R.string.landmarker_failed, e.message ?: "camera unavailable")
        }
    }

    override fun onResults(resultBundle: FaceLandmarkerHelper.ResultBundle) {
        val delegate = faceLandmarkerHelper.delegate?.name ?: "?"
        runOnUiThread {
            overlay.setResults(
                resultBundle.result,
                resultBundle.inputImageWidth,
                resultBundle.inputImageHeight,
            )
            status.text = getString(R.string.status_format, delegate, resultBundle.inferenceTimeMs)
        }
    }

    override fun onEmpty() {
        runOnUiThread { overlay.clear() }
    }

    override fun onError(error: String) {
        Log.e(TAG, error)
        runOnUiThread { status.text = getString(R.string.landmarker_failed, error) }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
