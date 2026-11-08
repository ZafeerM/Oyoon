package oyoon.core.gate

import oyoon.core.geometry.HeadPose
import oyoon.core.measure.irisDiameterPxA
import oyoon.core.measure.irisDiameterPxB
import oyoon.core.model.FrameSample
import kotlin.math.abs
import kotlin.math.max

/**
 * Validity gate limits (developer guide 4.2). Start values for this plan,
 * not from the literature; final values go into docs/decisions.md.
 */
data class GateConfig(
    val maxYawDeg: Double = 10.0,
    val maxPitchDeg: Double = 10.0,
    val maxRollDeg: Double = 5.0,
    /** Minimum mean iris diameter in pixels. */
    val minIrisPx: Double = 20.0,
    /** Maximum |A - B| / larger iris diameter. */
    val maxIrisMismatch: Double = 0.10,
    /** Maximum (max - min) / mean of the mean iris diameter over [stabilityFrames]. */
    val maxIrisVariation: Double = 0.02,
    /** Frames of history the stability check needs. */
    val stabilityFrames: Int = 15,
    /** Minimum mean face luma (0-255). */
    val minBrightness: Double = 60.0,
) {
    init {
        require(stabilityFrames >= 2) { "stabilityFrames must be at least 2, was $stabilityFrames" }
    }
}

/** Why a frame was rejected. Checked in this order; the first failure is reported. */
enum class RejectReason { YAW, PITCH, ROLL, TOO_FAR, IRIS_MISMATCH, UNSTABLE, TOO_DARK }

sealed interface GateResult {
    data object Accepted : GateResult
    data class Rejected(val reason: RejectReason) : GateResult
}

/** Remembers the mean iris diameter of recent frames for the stability check. */
class GateState(cfg: GateConfig = GateConfig()) {
    private val capacity = cfg.stabilityFrames
    private val recentIrisPx = ArrayDeque<Double>(capacity)

    /** Adds one frame's mean iris diameter, dropping the oldest beyond [capacity]. */
    fun record(meanIrisPx: Double) {
        if (recentIrisPx.size == capacity) recentIrisPx.removeFirst()
        recentIrisPx.addLast(meanIrisPx)
    }

    /**
     * (max - min) / mean of the recorded diameters, or null until [capacity]
     * frames have been recorded.
     */
    fun variation(): Double? {
        if (recentIrisPx.size < capacity) return null
        val mean = recentIrisPx.average()
        if (mean <= 0.0) return Double.POSITIVE_INFINITY
        return (recentIrisPx.max() - recentIrisPx.min()) / mean
    }

    fun clear() = recentIrisPx.clear()
}

/**
 * Decides whether [sample] is good enough to measure. Every frame's mean iris
 * diameter is recorded in [state] first, so head movement also breaks stability.
 * [state] must be created with the same [cfg].
 */
fun gate(sample: FrameSample, cfg: GateConfig, state: GateState): GateResult {
    val matrix = requireNotNull(sample.transformMatrix) { "gate needs the transformation matrix" }
    val pose = HeadPose.fromMatrix(matrix)
    val irisA = irisDiameterPxA(sample)
    val irisB = irisDiameterPxB(sample)
    val meanIris = (irisA + irisB) / 2.0
    state.record(meanIris)

    val reason = when {
        abs(pose.yawDeg) > cfg.maxYawDeg -> RejectReason.YAW
        abs(pose.pitchDeg) > cfg.maxPitchDeg -> RejectReason.PITCH
        abs(pose.rollDeg) > cfg.maxRollDeg -> RejectReason.ROLL
        meanIris < cfg.minIrisPx -> RejectReason.TOO_FAR
        abs(irisA - irisB) / max(irisA, irisB) > cfg.maxIrisMismatch -> RejectReason.IRIS_MISMATCH
        (state.variation() ?: Double.POSITIVE_INFINITY) > cfg.maxIrisVariation -> RejectReason.UNSTABLE
        sample.brightness != null && sample.brightness < cfg.minBrightness -> RejectReason.TOO_DARK
        else -> null
    }
    return if (reason == null) GateResult.Accepted else GateResult.Rejected(reason)
}
