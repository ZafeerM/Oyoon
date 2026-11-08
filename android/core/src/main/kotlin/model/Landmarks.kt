package oyoon.core.model

/**
 * Single place for every MediaPipe face-mesh landmark index Oyoon uses
 * (478-point model with irises). See developer guide section 4.1.
 *
 * Irises are named A and B rather than left/right: the front-camera frame is
 * mirrored before inference, so "left" would be ambiguous.
 */
object Landmarks {
    /** Landmarks per face when iris refinement is on. */
    const val COUNT = 478

    const val IRIS_A_CENTRE = 468
    /** Horizontal edge pair of iris A (side to side, not top/bottom). */
    const val IRIS_A_H1 = 469
    const val IRIS_A_H2 = 471

    const val IRIS_B_CENTRE = 473
    /** Horizontal edge pair of iris B. */
    const val IRIS_B_H1 = 474
    const val IRIS_B_H2 = 476

    /** All iris points: both centres and their 4 edge points each. */
    val IRIS_ALL: IntRange = 468..477

    /** Widest points of the face outline. */
    const val FACE_LEFT = 234
    const val FACE_RIGHT = 454

    /** Nose bridge midpoint; also the try-on anchor. */
    const val NOSE_BRIDGE = 168
}
