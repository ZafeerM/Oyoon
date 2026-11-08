package oyoon.app.landmarker

import android.graphics.Bitmap
import android.graphics.Color
import oyoon.core.geometry.Point2
import oyoon.core.model.FrameSample
import oyoon.core.model.Landmarks

/**
 * Turns a MediaPipe result into a :core [FrameSample] (S2-AND-1).
 *
 * The only work done here is what :core cannot do without Android: pixel
 * conversion (inside [FrameSample.fromNormalized]), copying the raw
 * transformation matrix, and measuring face brightness from the bitmap.
 * Iris sizes, angles, gating and measuring all happen in :core.
 */
object FrameSampler {

    /** Grid of points sampled across the face box for brightness. */
    private const val BRIGHTNESS_GRID = 32

    /** Sample of the first face, or null if the result has no face or no iris points. */
    fun from(bundle: FaceLandmarkerHelper.ResultBundle): FrameSample? {
        val face = bundle.result.faceLandmarks().firstOrNull() ?: return null
        if (face.size < Landmarks.COUNT) return null
        val matrix = bundle.result.facialTransformationMatrixes()
            .orElse(null)
            ?.firstOrNull()
            ?.copyOf()

        val sample = FrameSample.fromNormalized(
            normalized = face.map { Point2(it.x().toDouble(), it.y().toDouble()) },
            imageWidth = bundle.inputImageWidth,
            imageHeight = bundle.inputImageHeight,
            transformMatrix = matrix,
        )
        return sample.copy(brightness = faceBrightness(bundle.inputImage, sample))
    }

    /**
     * Mean luma (0-255) over the bounding box of the face landmarks, sampled on
     * a [BRIGHTNESS_GRID] x [BRIGHTNESS_GRID] grid to stay cheap.
     */
    private fun faceBrightness(bitmap: Bitmap, sample: FrameSample): Double? {
        val points = sample.landmarksPx.values
        val left = points.minOf { it.x }.toInt().coerceIn(0, bitmap.width - 1)
        val right = points.maxOf { it.x }.toInt().coerceIn(0, bitmap.width - 1)
        val top = points.minOf { it.y }.toInt().coerceIn(0, bitmap.height - 1)
        val bottom = points.maxOf { it.y }.toInt().coerceIn(0, bitmap.height - 1)
        if (right <= left || bottom <= top) return null

        var sum = 0.0
        var count = 0
        for (gy in 0 until BRIGHTNESS_GRID) {
            val y = top + (bottom - top) * gy / (BRIGHTNESS_GRID - 1)
            for (gx in 0 until BRIGHTNESS_GRID) {
                val x = left + (right - left) * gx / (BRIGHTNESS_GRID - 1)
                val c = bitmap.getPixel(x, y)
                sum += 0.299 * Color.red(c) + 0.587 * Color.green(c) + 0.114 * Color.blue(c)
                count++
            }
        }
        return sum / count
    }
}
