package oyoon.core.geometry

import oyoon.core.fixtures.SyntheticSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class HeadPoseTest {

    @Test
    fun `identity rotation is looking straight`() {
        val pose = HeadPose.fromMatrix(SyntheticSnapshot.rotationMatrix(0.0, 0.0, 0.0))
        assertEquals(0.0, pose.yawDeg, absoluteTolerance = 1e-9)
        assertEquals(0.0, pose.pitchDeg, absoluteTolerance = 1e-9)
        assertEquals(0.0, pose.rollDeg, absoluteTolerance = 1e-9)
    }

    @Test
    fun `combined yaw, pitch and roll are each recovered`() {
        val pose = HeadPose.fromMatrix(SyntheticSnapshot.rotationMatrix(12.0, -7.0, 4.0))
        assertEquals(12.0, pose.yawDeg, absoluteTolerance = 1e-4)
        assertEquals(-7.0, pose.pitchDeg, absoluteTolerance = 1e-4)
        assertEquals(4.0, pose.rollDeg, absoluteTolerance = 1e-4)
    }

    @Test
    fun `wrong matrix size fails`() {
        assertFailsWith<IllegalArgumentException> { HeadPose.fromMatrix(FloatArray(9)) }
    }
}
