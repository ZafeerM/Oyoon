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
 * from inside draw(), colours defined in code, highlighted irises, optional
 * landmark index labels.
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
import kotlin.math.hypot
import kotlin.math.max

/**
 * Draws the face mesh on top of a CameraX PreviewView in its default
 * FILL_CENTER scale type. Irises get a circle and larger dots so they stand
 * out from the mesh.
 */
class OverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    /** When true, draws the index number next to each landmark in [LABELLED_LANDMARKS]. */
    var showLabels = false
        set(value) {
            field = value
            invalidate()
        }

    private var results: FaceLandmarkerResult? = null
    private var imageWidth = 1
    private var imageHeight = 1

    private val density = resources.displayMetrics.density

    private val meshPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(0x99, 0x00, 0xC8, 0xD7)
        strokeWidth = 1.5f * density
        style = Paint.Style.STROKE
    }
    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(0xB0, 0xFF, 0xFF, 0x00)
        strokeWidth = 2f * density
        style = Paint.Style.FILL
    }
    private val irisCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.MAGENTA
        strokeWidth = 2f * density
        style = Paint.Style.STROKE
    }
    private val irisPointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.MAGENTA
        style = Paint.Style.FILL
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 11f * density
        setShadowLayer(2f * density, 0f, 0f, Color.BLACK)
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
                canvas.drawLine(x(start), y(start), x(end), y(end), meshPaint)
            }
            for (landmark in face) {
                canvas.drawPoint(x(landmark), y(landmark), pointPaint)
            }

            if (face.size >= Iris.LANDMARK_COUNT) {
                for (center in Iris.CENTERS) {
                    // Circle radius: mean of the two diameters (side-side, top-bottom) / 2.
                    val c = face[center]
                    val horizontal = hypot(
                        x(face[center + 1]) - x(face[center + 3]),
                        y(face[center + 1]) - y(face[center + 3]),
                    )
                    val vertical = hypot(
                        x(face[center + 2]) - x(face[center + 4]),
                        y(face[center + 2]) - y(face[center + 4]),
                    )
                    canvas.drawCircle(x(c), y(c), (horizontal + vertical) / 4f, irisCirclePaint)
                    for (i in center..center + 4) {
                        canvas.drawCircle(x(face[i]), y(face[i]), IRIS_DOT_RADIUS * density, irisPointPaint)
                    }
                }
            }

            if (showLabels) {
                for (i in LABELLED_LANDMARKS) {
                    val l = face.getOrNull(i) ?: continue
                    canvas.drawText(i.toString(), x(l) + 4f * density, y(l) - 4f * density, labelPaint)
                }
            }
        }
    }

    companion object {
        private const val IRIS_DOT_RADIUS = 2.5f

        /** Landmarks labelled in debug mode: both irises, nose bridge, face sides. */
        val LABELLED_LANDMARKS: List<Int> = (468..477) + listOf(168, 234, 454)
    }
}
