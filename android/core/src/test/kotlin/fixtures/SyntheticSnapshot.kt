package oyoon.core.fixtures

import oyoon.core.geometry.Point2
import oyoon.core.model.FrameSample
import oyoon.core.model.Landmarks
import oyoon.core.model.Snapshot
import kotlin.math.cos
import kotlin.math.sin

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
    const val BRIGHTNESS = 128.0

    /**
     * One frame on a 1280x720 image. Defaults give a frame that passes the gate
     * (once the gate state has enough history).
     */
    fun frame(
        pdPx: Double = PD_PX,
        irisPx: Double = IRIS_PX,
        irisPxB: Double = irisPx,
        yawDeg: Double = 0.0,
        pitchDeg: Double = 0.0,
        rollDeg: Double = 0.0,
        brightness: Double? = BRIGHTNESS,
    ): FrameSample {
        val eyeY = 300.0
        val aX = 490.0
        val bX = aX + pdPx
        val rA = irisPx / 2.0
        val rB = irisPxB / 2.0
        val faceCentreX = (aX + bX) / 2.0
        val points = mapOf(
            Landmarks.IRIS_A_CENTRE to Point2(aX, eyeY),
            Landmarks.IRIS_A_H1 to Point2(aX - rA, eyeY),
            Landmarks.IRIS_A_H2 to Point2(aX + rA, eyeY),
            Landmarks.IRIS_B_CENTRE to Point2(bX, eyeY),
            Landmarks.IRIS_B_H1 to Point2(bX - rB, eyeY),
            Landmarks.IRIS_B_H2 to Point2(bX + rB, eyeY),
            // Top/bottom iris points deliberately wrong: measurement must ignore them.
            470 to Point2(aX, eyeY - 5 * rA), 472 to Point2(aX, eyeY + 5 * rA),
            475 to Point2(bX, eyeY - 5 * rB), 477 to Point2(bX, eyeY + 5 * rB),
            Landmarks.FACE_LEFT to Point2(faceCentreX - FACE_WIDTH_PX / 2, eyeY + 40),
            Landmarks.FACE_RIGHT to Point2(faceCentreX + FACE_WIDTH_PX / 2, eyeY + 40),
            Landmarks.NOSE_BRIDGE to Point2(faceCentreX, eyeY - 10),
        )
        return FrameSample(
            imageWidth = 1280,
            imageHeight = 720,
            landmarksPx = points,
            transformMatrix = rotationMatrix(yawDeg, pitchDeg, rollDeg),
            brightness = brightness,
        )
    }

    /** A snapshot of [count] identical worked-example frames. */
    fun snapshot(count: Int = 5) = Snapshot(List(count) { frame() })

    /**
     * 4x4 column-major matrix R = Rz(roll) * Ry(yaw) * Rx(pitch), the rotation
     * order the guide's angle formulas (4.2) decode.
     */
    fun rotationMatrix(yawDeg: Double, pitchDeg: Double, rollDeg: Double): FloatArray {
        val y = Math.toRadians(yawDeg)
        val p = Math.toRadians(pitchDeg)
        val r = Math.toRadians(rollDeg)
        val rows = arrayOf(
            doubleArrayOf(
                cos(r) * cos(y),
                cos(r) * sin(y) * sin(p) - sin(r) * cos(p),
                cos(r) * sin(y) * cos(p) + sin(r) * sin(p),
            ),
            doubleArrayOf(
                sin(r) * cos(y),
                sin(r) * sin(y) * sin(p) + cos(r) * cos(p),
                sin(r) * sin(y) * cos(p) - cos(r) * sin(p),
            ),
            doubleArrayOf(-sin(y), cos(y) * sin(p), cos(y) * cos(p)),
        )
        val m = FloatArray(16)
        for (row in 0..2) for (col in 0..2) m[col * 4 + row] = rows[row][col].toFloat()
        m[15] = 1f
        return m
    }
}
