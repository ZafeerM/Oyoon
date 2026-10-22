package oyoon.core.geometry

import kotlin.test.Test
import kotlin.test.assertEquals

class DistanceTest {

    @Test
    fun `distance from (0,0) to (3,4) is 5`() {
        assertEquals(5.0, distance(Point2(0.0, 0.0), Point2(3.0, 4.0)), absoluteTolerance = 1e-12)
    }
}
