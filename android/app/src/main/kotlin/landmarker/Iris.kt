package oyoon.app.landmarker

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.hypot

/**
 * Iris landmark indices in MediaPipe's 478-point face mesh, plus a pixel-width helper.
 *
 * Each iris is a centre followed by 4 edge points going around it:
 * centre, +1 side, +2 top, +3 other side, +4 bottom.
 *
 * Irises are named by centre index, not left/right: the front-camera frame is
 * mirrored before inference, so the model's left/right are swapped relative to
 * the person.
 */
object Iris {
    const val FIRST_CENTER = 468
    const val SECOND_CENTER = 473
    val CENTERS = intArrayOf(FIRST_CENTER, SECOND_CENTER)

    /** Number of landmarks the model returns when iris points are present. */
    const val LANDMARK_COUNT = 478

    /**
     * Edge-to-edge width (points +1 and +3) of the iris centred at [center], in
     * pixels of the [imageWidth] x [imageHeight] image the landmarks came from.
     */
    fun widthPx(
        landmarks: List<NormalizedLandmark>,
        center: Int,
        imageWidth: Int,
        imageHeight: Int,
    ): Float {
        val a = landmarks[center + 1]
        val b = landmarks[center + 3]
        return hypot((a.x() - b.x()) * imageWidth, (a.y() - b.y()) * imageHeight)
    }
}
