package oyoon.core.geometry

import kotlin.math.asin
import kotlin.math.atan2

/** Head rotation in degrees. The gate only uses the size of each angle, not its sign. */
data class HeadPose(val yawDeg: Double, val pitchDeg: Double, val rollDeg: Double) {

    companion object {
        /**
         * Angles from MediaPipe's facial transformation matrix (developer guide 4.2):
         * 16 floats in column-major order, so row r, column c is m[c * 4 + r].
         */
        fun fromMatrix(m: FloatArray): HeadPose {
            require(m.size == 16) { "transformation matrix must have 16 values, had ${m.size}" }
            fun r(row: Int, col: Int) = m[col * 4 + row].toDouble()
            return HeadPose(
                yawDeg = Math.toDegrees(asin(-r(2, 0).coerceIn(-1.0, 1.0))),
                pitchDeg = Math.toDegrees(atan2(r(2, 1), r(2, 2))),
                rollDeg = Math.toDegrees(atan2(r(1, 0), r(0, 0))),
            )
        }
    }
}
