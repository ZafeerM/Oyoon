package oyoon.app.live

import android.util.Log
import oyoon.core.gate.GateConfig
import oyoon.core.gate.GateResult
import oyoon.core.gate.GateState
import oyoon.core.gate.gate
import oyoon.core.geometry.HeadPose
import oyoon.core.measure.irisDiameterPxA
import oyoon.core.measure.irisDiameterPxB
import oyoon.core.measure.measure
import oyoon.core.model.FrameSample
import oyoon.core.model.Snapshot
import java.util.Locale

/**
 * Runs each [FrameSample] through :core's gate and measurement, and keeps the
 * live PD: the median of the last [windowSize] accepted frames.
 *
 * Not thread-safe: call [process] and [reset] from the same thread
 * (MediaPipe's result thread).
 */
class LiveMeasurer(
    private val cfg: GateConfig = GateConfig(),
    private val windowSize: Int = cfg.stabilityFrames,
) {
    private val gateState = GateState(cfg)
    private val accepted = ArrayDeque<FrameSample>(windowSize)

    /** Result of one frame. [pdMm] is null until at least one frame has been accepted. */
    data class Update(val gate: GateResult, val pdMm: Double?)

    fun process(sample: FrameSample): Update {
        val result = gate(sample, cfg, gateState)
        if (result == GateResult.Accepted) {
            if (accepted.size == windowSize) accepted.removeFirst()
            accepted.addLast(sample)
        }
        val pdMm = if (accepted.isEmpty()) null else measure(Snapshot(accepted.toList())).pdMm
        log(sample, result, pdMm)
        return Update(result, pdMm)
    }

    /** Forgets history, e.g. after a resolution change makes old iris sizes meaningless. */
    fun reset() {
        gateState.clear()
        accepted.clear()
    }

    /** One Logcat line per frame (S2-AND-1). Logging only; nothing here feeds back into the result. */
    private fun log(sample: FrameSample, result: GateResult, pdMm: Double?) {
        val pose = sample.transformMatrix?.let(HeadPose::fromMatrix)
        val gateText = when (result) {
            GateResult.Accepted -> "ACCEPTED"
            is GateResult.Rejected -> "REJECTED(${result.reason})"
        }
        Log.d(
            TAG,
            String.format(
                Locale.US,
                "FrameSample %dx%d irisA=%.1fpx irisB=%.1fpx yaw=%.1f pitch=%.1f roll=%.1f " +
                    "brightness=%.0f gate=%s livePd=%s",
                sample.imageWidth, sample.imageHeight,
                irisDiameterPxA(sample), irisDiameterPxB(sample),
                pose?.yawDeg ?: Double.NaN, pose?.pitchDeg ?: Double.NaN, pose?.rollDeg ?: Double.NaN,
                sample.brightness ?: Double.NaN,
                gateText,
                pdMm?.let { String.format(Locale.US, "%.1fmm", it) } ?: "-",
            ),
        )
    }

    companion object {
        const val TAG = "OyoonFrame"
    }
}
