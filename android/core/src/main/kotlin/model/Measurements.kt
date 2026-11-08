package oyoon.core.model

/** Face measurements in millimetres. */
data class Measurements(
    /** Pupillary distance: iris centre to iris centre. */
    val pdMm: Double,
    /** Nose bridge width. Sprint 3; 0.0 until then. */
    val bridgeMm: Double,
    /** Face width between the widest outline points. */
    val faceWidthMm: Double,
    /** Temple width. Sprint 3; 0.0 until then. */
    val templeWidthMm: Double,
)
