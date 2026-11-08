package oyoon.core.gate

import oyoon.core.fixtures.SyntheticSnapshot
import oyoon.core.model.FrameSample
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GateTest {

    private val cfg = GateConfig()

    /** A state already holding a full history of steady frames of [irisPx]. */
    private fun steadyState(irisPx: Double = SyntheticSnapshot.IRIS_PX) =
        GateState(cfg).apply { repeat(cfg.stabilityFrames) { record(irisPx) } }

    private fun check(sample: FrameSample, state: GateState = steadyState()) = gate(sample, cfg, state)

    @Test
    fun `straight, steady, close and lit frame is accepted`() {
        assertEquals(GateResult.Accepted, check(SyntheticSnapshot.frame()))
    }

    @Test
    fun `head turned sideways is rejected as YAW`() {
        assertEquals(GateResult.Rejected(RejectReason.YAW), check(SyntheticSnapshot.frame(yawDeg = 15.0)))
    }

    @Test
    fun `head tilted up is rejected as PITCH`() {
        assertEquals(GateResult.Rejected(RejectReason.PITCH), check(SyntheticSnapshot.frame(pitchDeg = -15.0)))
    }

    @Test
    fun `head tilted towards a shoulder is rejected as ROLL`() {
        assertEquals(GateResult.Rejected(RejectReason.ROLL), check(SyntheticSnapshot.frame(rollDeg = 8.0)))
    }

    @Test
    fun `small irises are rejected as TOO_FAR`() {
        assertEquals(
            GateResult.Rejected(RejectReason.TOO_FAR),
            check(SyntheticSnapshot.frame(irisPx = 15.0), steadyState(15.0)),
        )
    }

    @Test
    fun `irises of different size are rejected as IRIS_MISMATCH`() {
        // 50 vs 40 px is 20% of the larger; the limit is 10%.
        assertEquals(
            GateResult.Rejected(RejectReason.IRIS_MISMATCH),
            check(SyntheticSnapshot.frame(irisPx = 50.0, irisPxB = 40.0), steadyState(45.0)),
        )
    }

    @Test
    fun `iris size jump is rejected as UNSTABLE`() {
        // History steady at 50 px, then 52 px: about 4% spread; the limit is 2%.
        assertEquals(GateResult.Rejected(RejectReason.UNSTABLE), check(SyntheticSnapshot.frame(irisPx = 52.0)))
    }

    @Test
    fun `dark frame is rejected as TOO_DARK`() {
        assertEquals(GateResult.Rejected(RejectReason.TOO_DARK), check(SyntheticSnapshot.frame(brightness = 30.0)))
    }

    @Test
    fun `UNSTABLE until the history is full, then accepted`() {
        val state = GateState(cfg)
        repeat(cfg.stabilityFrames - 1) {
            assertEquals(GateResult.Rejected(RejectReason.UNSTABLE), gate(SyntheticSnapshot.frame(), cfg, state))
        }
        assertEquals(GateResult.Accepted, gate(SyntheticSnapshot.frame(), cfg, state))
    }

    @Test
    fun `missing brightness skips the TOO_DARK check`() {
        assertEquals(GateResult.Accepted, check(SyntheticSnapshot.frame(brightness = null)))
    }

    @Test
    fun `first failing rule in guide order is reported`() {
        assertEquals(
            GateResult.Rejected(RejectReason.YAW),
            check(SyntheticSnapshot.frame(yawDeg = 20.0, rollDeg = 20.0, brightness = 10.0)),
        )
    }

    @Test
    fun `sample without matrix fails clearly`() {
        val e = assertFailsWith<IllegalArgumentException> {
            check(SyntheticSnapshot.frame().copy(transformMatrix = null))
        }
        assertEquals("gate needs the transformation matrix", e.message)
    }
}
