package oyoon.core.measure

import oyoon.core.geometry.distance
import oyoon.core.model.FrameSample
import oyoon.core.model.Landmarks
import oyoon.core.model.Measurements
import oyoon.core.model.Snapshot

/** Average human iris diameter in mm, used as the real-world scale reference. */
const val DEFAULT_IRIS_MM = 11.7

/** Horizontal diameter of iris A in pixels (top/bottom points are skipped: eyelids cover them). */
fun irisDiameterPxA(sample: FrameSample): Double =
    distance(sample.px(Landmarks.IRIS_A_H1), sample.px(Landmarks.IRIS_A_H2))

/** Horizontal diameter of iris B in pixels. */
fun irisDiameterPxB(sample: FrameSample): Double =
    distance(sample.px(Landmarks.IRIS_B_H1), sample.px(Landmarks.IRIS_B_H2))

/** Millimetres per pixel: irisMm / mean of both iris diameters in pixels. */
fun mmPerPx(sample: FrameSample, irisMm: Double = DEFAULT_IRIS_MM): Double {
    require(irisMm > 0.0) { "irisMm must be positive, was $irisMm" }
    val meanIrisPx = (irisDiameterPxA(sample) + irisDiameterPxB(sample)) / 2.0
    require(meanIrisPx > 0.0) { "iris diameter is 0 px, cannot compute scale" }
    return irisMm / meanIrisPx
}

/** Measurements of one frame. Bridge and temple width come in Sprint 3 and are 0.0. */
fun measureFrame(sample: FrameSample, irisMm: Double = DEFAULT_IRIS_MM): Measurements {
    val k = mmPerPx(sample, irisMm)
    fun mm(a: Int, b: Int) = distance(sample.px(a), sample.px(b)) * k
    return Measurements(
        pdMm = mm(Landmarks.IRIS_A_CENTRE, Landmarks.IRIS_B_CENTRE),
        bridgeMm = 0.0,
        faceWidthMm = mm(Landmarks.FACE_LEFT, Landmarks.FACE_RIGHT),
        templeWidthMm = 0.0,
    )
}

/** Measures every frame of [snapshot] and returns the median of each value. */
fun measure(snapshot: Snapshot, irisMm: Double = DEFAULT_IRIS_MM): Measurements {
    require(snapshot.frames.isNotEmpty()) { "snapshot has no frames" }
    val all = snapshot.frames.map { measureFrame(it, irisMm) }
    return Measurements(
        pdMm = median(all.map { it.pdMm }),
        bridgeMm = median(all.map { it.bridgeMm }),
        faceWidthMm = median(all.map { it.faceWidthMm }),
        templeWidthMm = median(all.map { it.templeWidthMm }),
    )
}

/** Median of a non-empty list; the mean of the two middle values for an even count. */
internal fun median(values: List<Double>): Double {
    require(values.isNotEmpty()) { "median of an empty list" }
    val sorted = values.sorted()
    val mid = sorted.size / 2
    return if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2.0
}
