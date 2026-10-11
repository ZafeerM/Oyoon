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
 * examples/face_landmarker/android. Live-stream scaling only, no clearing
 * from inside draw(), colours defined in code.
 */
package oyoon.app.landmarker

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import kotlin.math.max

/**
 * Draws the face mesh on top of a CameraX PreviewView in its default
 * FILL_CENTER scale type.
 */
class OverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private var results: FaceLandmarkerResult? = null
    private var imageWidth = 1
    private var imageHeight = 1

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(0x00, 0x7F, 0x8B)
        strokeWidth = STROKE_WIDTH
        style = Paint.Style.STROKE
    }
    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.YELLOW
        strokeWidth = STROKE_WIDTH
        style = Paint.Style.FILL
    }

    fun setResults(result: FaceLandmarkerResult, imageWidth: Int, imageHeight: Int) {
        results = result
        this.imageWidth = imageWidth
        this.imageHeight = imageHeight
        invalidate()
    }

    fun clear() {
        results = null
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val faces = results?.faceLandmarks().orEmpty()
        if (faces.isEmpty()) return

        // PreviewView FILL_CENTER: scale up to cover the view, then centre.
        val scale = max(width.toFloat() / imageWidth, height.toFloat() / imageHeight)
        val offsetX = (width - imageWidth * scale) / 2f
        val offsetY = (height - imageHeight * scale) / 2f
        fun x(l: NormalizedLandmark) = l.x() * imageWidth * scale + offsetX
        fun y(l: NormalizedLandmark) = l.y() * imageHeight * scale + offsetY

        for (face in faces) {
            for (connector in FaceLandmarker.FACE_LANDMARKS_CONNECTORS) {
                val start = face.getOrNull(connector.start()) ?: continue
                val end = face.getOrNull(connector.end()) ?: continue
                canvas.drawLine(x(start), y(start), x(end), y(end), linePaint)
            }
            for (landmark in face) {
                canvas.drawPoint(x(landmark), y(landmark), pointPaint)
            }
        }
    }

    companion object {
        private const val STROKE_WIDTH = 4f
    }
}
