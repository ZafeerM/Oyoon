package oyoon.core.measure

import oyoon.core.fixtures.SyntheticSnapshot
import oyoon.core.geometry.Point2
import oyoon.core.model.FrameSample
import oyoon.core.model.Landmarks
import oyoon.core.model.Snapshot
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MeasureTest {

    @ParameterizedTest(name = "iris {0} mm gives PD {1} mm")
    @CsvSource("11.7, 70.2", "11.0, 66.0", "12.6, 75.6")
    fun `PD from synthetic snapshot`(irisMm: Double, expectedPdMm: Double) {
        val result = measure(SyntheticSnapshot.snapshot(), irisMm)
        assertEquals(expectedPdMm, result.pdMm, absoluteTolerance = 0.01)
    }

    @Test
    fun `mmPerPx matches worked example`() {
        assertEquals(0.234, mmPerPx(SyntheticSnapshot.frame(), 11.7), absoluteTolerance = 1e-12)
    }

    @Test
    fun `mmPerPx uses the mean of both irises`() {
        val frame = SyntheticSnapshot.frame()
        // Widen iris B to 70 px; mean becomes 60 px.
        val wider = frame.copy(
            landmarksPx = frame.landmarksPx + mapOf(
                Landmarks.IRIS_B_H1 to frame.px(Landmarks.IRIS_B_CENTRE).let { Point2(it.x - 35, it.y) },
                Landmarks.IRIS_B_H2 to frame.px(Landmarks.IRIS_B_CENTRE).let { Point2(it.x + 35, it.y) },
            )
        )
        assertEquals(11.7 / 60.0, mmPerPx(wider, 11.7), absoluteTolerance = 1e-12)
    }

    @Test
    fun `measureFrame gives face width and zero for Sprint 3 values`() {
        val m = measureFrame(SyntheticSnapshot.frame(), 11.7)
        assertEquals(70.2, m.pdMm, absoluteTolerance = 1e-9)
        assertEquals(400 * 0.234, m.faceWidthMm, absoluteTolerance = 1e-9)
        assertEquals(0.0, m.bridgeMm)
        assertEquals(0.0, m.templeWidthMm)
    }

    @Test
    fun `measure takes the median, so one outlier frame does not move it`() {
        val frames = listOf(290.0, 300.0, 310.0, 300.0, 900.0).map { SyntheticSnapshot.frame(pdPx = it) }
        assertEquals(300 * 0.234, measure(Snapshot(frames), 11.7).pdMm, absoluteTolerance = 1e-9)
    }

    @Test
    fun `median of an even count is the mean of the two middle values`() {
        assertEquals(2.5, median(listOf(4.0, 1.0, 3.0, 2.0)))
        assertEquals(3.0, median(listOf(5.0, 1.0, 3.0)))
    }

    @Test
    fun `fromNormalized converts to pixels per axis on a non-square image`() {
        val sample = FrameSample.fromNormalized(listOf(Point2(0.0, 0.0), Point2(1.0, 1.0)), 640, 480)
        assertEquals(Point2(640.0, 480.0), sample.px(1))
        assertEquals(800.0, oyoon.core.geometry.distance(sample.px(0), sample.px(1)), absoluteTolerance = 1e-12)
    }

    @Test
    fun `empty snapshot fails with a clear message`() {
        val e = assertFailsWith<IllegalArgumentException> { measure(Snapshot(emptyList())) }
        assertContains(e.message.orEmpty(), "no frames")
    }

    @Test
    fun `missing landmark fails and names the index`() {
        val frame = SyntheticSnapshot.frame()
        val missing = frame.copy(landmarksPx = frame.landmarksPx - Landmarks.IRIS_B_CENTRE)
        val e = assertFailsWith<IllegalArgumentException> { measureFrame(missing) }
        assertContains(e.message.orEmpty(), "473")
    }

    @Test
    fun `zero iris width fails instead of dividing by zero`() {
        assertFailsWith<IllegalArgumentException> { mmPerPx(SyntheticSnapshot.frame(irisPx = 0.0)) }
    }
}
