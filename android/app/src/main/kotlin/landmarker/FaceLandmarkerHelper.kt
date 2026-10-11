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
 * Modified by Oyoon (2026): adapted from google-ai-edge/mediapipe-samples
 * examples/face_landmarker/android. Kept only LIVE_STREAM mode, fixed to one
 * face, enabled facial transformation matrixes, disabled blendshapes, added
 * GPU-to-CPU fallback, and reuse the frame buffer between frames.
 */
package oyoon.app.landmarker

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult

/**
 * Wraps a MediaPipe [FaceLandmarker] running in [RunningMode.LIVE_STREAM].
 *
 * Create, use and close it on one background thread: the GPU delegate must be
 * used on the thread that created it.
 */
class FaceLandmarkerHelper(
    private val context: Context,
    private val listener: LandmarkerListener,
) {

    private var faceLandmarker: FaceLandmarker? = null
    private var bitmapBuffer: Bitmap? = null

    /** Delegate actually in use, or null when closed. */
    var delegate: Delegate? = null
        private set

    val isClosed: Boolean
        get() = faceLandmarker == null

    /** Tries the GPU delegate first and falls back to CPU if it cannot start. */
    fun setup() {
        if (faceLandmarker != null) return
        for (candidate in listOf(Delegate.GPU, Delegate.CPU)) {
            try {
                faceLandmarker = FaceLandmarker.createFromOptions(context, buildOptions(candidate))
                delegate = candidate
                Log.i(TAG, "Face landmarker started on $candidate")
                return
            } catch (e: RuntimeException) {
                Log.w(TAG, "Face landmarker failed on $candidate", e)
            }
        }
        listener.onError("no usable delegate (GPU and CPU both failed)")
    }

    fun close() {
        faceLandmarker?.close()
        faceLandmarker = null
        delegate = null
    }

    private fun buildOptions(delegate: Delegate): FaceLandmarker.FaceLandmarkerOptions {
        val baseOptions = BaseOptions.builder()
            .setDelegate(delegate)
            .setModelAssetPath(MODEL_ASSET)
            .build()
        return FaceLandmarker.FaceLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setNumFaces(1)
            .setOutputFacialTransformationMatrixes(true)
            .setOutputFaceBlendshapes(false)
            .setMinFaceDetectionConfidence(DEFAULT_CONFIDENCE)
            .setMinFacePresenceConfidence(DEFAULT_CONFIDENCE)
            .setMinTrackingConfidence(DEFAULT_CONFIDENCE)
            .setResultListener(this::returnLivestreamResult)
            .setErrorListener(this::returnLivestreamError)
            .build()
    }

    /**
     * Converts a CameraX RGBA_8888 frame to an upright [MPImage] and sends it to
     * the landmarker. Front-camera frames are mirrored so landmarks line up with
     * the mirrored preview. Always closes [imageProxy].
     */
    fun detectLiveStream(imageProxy: ImageProxy, isFrontCamera: Boolean) {
        val landmarker = faceLandmarker
        if (landmarker == null) {
            imageProxy.close()
            return
        }
        val frameTime = SystemClock.uptimeMillis()

        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val buffer = imageProxy.use { frame ->
            val plane = frame.planes[0]
            if (plane.rowStride == frame.width * plane.pixelStride) {
                // Tightly packed rows: copy straight into a reused bitmap.
                (bitmapBuffer
                    ?.takeIf { it.width == frame.width && it.height == frame.height }
                    ?: Bitmap.createBitmap(frame.width, frame.height, Bitmap.Config.ARGB_8888)
                        .also { bitmapBuffer = it })
                    .apply { copyPixelsFromBuffer(plane.buffer) }
            } else {
                // Some devices pad each row; let CameraX handle the stride.
                frame.toBitmap()
            }
        }

        val matrix = Matrix().apply {
            postRotate(rotationDegrees.toFloat())
            if (isFrontCamera) {
                postScale(-1f, 1f, buffer.width.toFloat(), buffer.height.toFloat())
            }
        }
        val upright = Bitmap.createBitmap(buffer, 0, 0, buffer.width, buffer.height, matrix, true)

        landmarker.detectAsync(BitmapImageBuilder(upright).build(), frameTime)
    }

    private fun returnLivestreamResult(result: FaceLandmarkerResult, input: MPImage) {
        if (result.faceLandmarks().isEmpty()) {
            listener.onEmpty()
            return
        }
        listener.onResults(
            ResultBundle(
                result = result,
                inferenceTimeMs = SystemClock.uptimeMillis() - result.timestampMs(),
                inputImageWidth = input.width,
                inputImageHeight = input.height,
            )
        )
    }

    private fun returnLivestreamError(error: RuntimeException) {
        listener.onError(error.message ?: "unknown error")
    }

    data class ResultBundle(
        val result: FaceLandmarkerResult,
        val inferenceTimeMs: Long,
        val inputImageWidth: Int,
        val inputImageHeight: Int,
    )

    interface LandmarkerListener {
        fun onResults(resultBundle: ResultBundle)
        fun onEmpty()
        fun onError(error: String)
    }

    companion object {
        private const val TAG = "FaceLandmarkerHelper"
        private const val MODEL_ASSET = "face_landmarker.task"
        private const val DEFAULT_CONFIDENCE = 0.5f
    }
}
