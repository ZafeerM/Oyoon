package oyoon.core.fixtures

import oyoon.core.geometry.Point2
import oyoon.core.model.FrameSample
import oyoon.core.model.Landmarks
import oyoon.core.model.Snapshot

/**
 * Synthetic frames built from the worked example in developer guide 4.1:
 * both irises 50 px wide, iris centres 300 px apart, so PD = 300 * irisMm / 50
 * (70.2 mm at 11.7, 66.0 at 11.0, 75.6 at 12.6).
 *
 * Stand-in until QA publishes a fixture in contracts/fixtures/.
 */
object SyntheticSnapshot {
    const val IRIS_PX = 50.0
    const val PD_PX = 300.0
    const val FACE_WIDTH_PX = 400.0

    /** One frame on a 1280x720 image with the given iris centre distance and iris width. */
    fun frame(pdPx: Double = PD_PX, irisPx: Double = IRIS_PX): FrameSample {
        val eyeY = 300.0
        val aX = 490.0
        val bX = aX + pdPx
        val r = irisPx / 2.0
        val faceCentreX = (aX + bX) / 2.0
        val points = mapOf(
            Landmarks.IRIS_A_CENTRE to Point2(aX, eyeY),
            Landmarks.IRIS_A_H1 to Point2(aX - r, eyeY),
            Landmarks.IRIS_A_H2 to Point2(aX + r, eyeY),
            Landmarks.IRIS_B_CENTRE to Point2(bX, eyeY),
            Landmarks.IRIS_B_H1 to Point2(bX - r, eyeY),
            Landmarks.IRIS_B_H2 to Point2(bX + r, eyeY),
            // Top/bottom iris points deliberately wrong: measurement must ignore them.
            470 to Point2(aX, eyeY - 5 * r), 472 to Point2(aX, eyeY + 5 * r),
            475 to Point2(bX, eyeY - 5 * r), 477 to Point2(bX, eyeY + 5 * r),
            Landmarks.FACE_LEFT to Point2(faceCentreX - FACE_WIDTH_PX / 2, eyeY + 40),
            Landmarks.FACE_RIGHT to Point2(faceCentreX + FACE_WIDTH_PX / 2, eyeY + 40),
            Landmarks.NOSE_BRIDGE to Point2(faceCentreX, eyeY - 10),
        )
        return FrameSample(imageWidth = 1280, imageHeight = 720, landmarksPx = points)
    }

    /** A snapshot of [count] identical worked-example frames. */
    fun snapshot(count: Int = 5) = Snapshot(List(count) { frame() })
}
