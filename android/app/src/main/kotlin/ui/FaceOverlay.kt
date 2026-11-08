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
 * Modified by Oyoon (2026): adapted from OverlayView in
 * google-ai-edge/mediapipe-samples examples/face_landmarker/android.
 * Rewritten as a Compose Canvas; live-stream scaling only, highlighted
 * irises, optional landmark index labels.
 */
package oyoon.app.ui

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import oyoon.core.model.Landmarks
import kotlin.math.hypot
import kotlin.math.max

private val MeshColor = Color(0x9900C8D7)
private val PointColor = Color(0xB0FFFF00)
private val IrisColor = Color.Magenta

/** Mesh connectors, copied once instead of per frame. */
private val Connectors = FaceLandmarker.FACE_LANDMARKS_CONNECTORS.map { it.start() to it.end() }

/** Centre and horizontal edge pair of each iris, the pair :core measures. */
private val Irises = listOf(
    Triple(Landmarks.IRIS_A_CENTRE, Landmarks.IRIS_A_H1, Landmarks.IRIS_A_H2),
    Triple(Landmarks.IRIS_B_CENTRE, Landmarks.IRIS_B_H1, Landmarks.IRIS_B_H2),
)

/** Landmarks labelled in debug mode: both irises, nose bridge, face sides. */
private val LabelledLandmarks: List<Int> = Landmarks.IRIS_ALL +
    listOf(Landmarks.NOSE_BRIDGE, Landmarks.FACE_LEFT, Landmarks.FACE_RIGHT)

/**
 * Draws the face mesh over a CameraX PreviewView in its default FILL_CENTER
 * scale type. Irises get a circle and larger dots.
 */
@Composable
fun FaceOverlay(
    result: FaceLandmarkerResult?,
    imageWidth: Int,
    imageHeight: Int,
    showLabels: Boolean,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current.density
    val labelPaint = remember(density) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 11f * density
            setShadowLayer(2f * density, 0f, 0f, android.graphics.Color.BLACK)
        }
    }

    Canvas(modifier) {
        val face = result?.faceLandmarks()?.firstOrNull() ?: return@Canvas

        // PreviewView FILL_CENTER: scale up to cover the view, then centre.
        val scale = max(size.width / imageWidth, size.height / imageHeight)
        val offsetX = (size.width - imageWidth * scale) / 2f
        val offsetY = (size.height - imageHeight * scale) / 2f
        val points = face.map { Offset(it.x() * imageWidth * scale + offsetX, it.y() * imageHeight * scale + offsetY) }

        for ((start, end) in Connectors) {
            val a = points.getOrNull(start) ?: continue
            val b = points.getOrNull(end) ?: continue
            drawLine(MeshColor, a, b, strokeWidth = 1.5f * density)
        }
        drawPoints(points, PointMode.Points, PointColor, strokeWidth = 2f * density, cap = StrokeCap.Round)

        if (points.size >= Landmarks.COUNT) {
            for ((centre, h1, h2) in Irises) {
                val diameter = hypot(points[h1].x - points[h2].x, points[h1].y - points[h2].y)
                drawCircle(IrisColor, diameter / 2f, points[centre], style = Stroke(2f * density))
            }
            for (i in Landmarks.IRIS_ALL) {
                drawCircle(IrisColor, 2.5f * density, points[i])
            }
        }

        if (showLabels) {
            drawIntoCanvas { canvas ->
                for (i in LabelledLandmarks) {
                    val p = points.getOrNull(i) ?: continue
                    canvas.nativeCanvas.drawText(i.toString(), p.x + 4f * density, p.y - 4f * density, labelPaint)
                }
            }
        }
    }
}
