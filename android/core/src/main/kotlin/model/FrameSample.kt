package oyoon.core.model

import oyoon.core.geometry.Point2

/**
 * Everything :core needs from one camera frame. Landmarks are in image pixels.
 *
 * Distances must only be measured in pixels: on a non-square image the
 * normalized 0-to-1 x and y units differ, so normalized distances are wrong.
 */
data class FrameSample(
    val imageWidth: Int,
    val imageHeight: Int,
    /** Landmark index to position in pixels. */
    val landmarksPx: Map<Int, Point2>,
    /**
     * MediaPipe facial transformation matrix, 16 floats column-major.
     * Needed by the validity gate; null when only measuring.
     */
    val transformMatrix: FloatArray? = null,
    /** Mean luma (0-255) of the face area, or null if not measured (gate then skips TOO_DARK). */
    val brightness: Double? = null,
) {
    init {
        require(imageWidth > 0 && imageHeight > 0) {
            "image size must be positive, was ${imageWidth}x$imageHeight"
        }
        require(transformMatrix == null || transformMatrix.size == 16) {
            "transformation matrix must have 16 values, had ${transformMatrix?.size}"
        }
    }

    /** Pixel position of landmark [index]; throws if the frame does not have it. */
    fun px(index: Int): Point2 =
        landmarksPx[index] ?: throw IllegalArgumentException("frame is missing landmark $index")

    // FloatArray compares by reference in a data class; compare its contents instead.
    override fun equals(other: Any?): Boolean =
        other is FrameSample &&
            imageWidth == other.imageWidth &&
            imageHeight == other.imageHeight &&
            landmarksPx == other.landmarksPx &&
            transformMatrix.contentEquals(other.transformMatrix) &&
            brightness == other.brightness

    override fun hashCode(): Int {
        var h = imageWidth
        h = 31 * h + imageHeight
        h = 31 * h + landmarksPx.hashCode()
        h = 31 * h + transformMatrix.contentHashCode()
        h = 31 * h + brightness.hashCode()
        return h
    }

    companion object {
        /**
         * Builds a sample from normalized (0-to-1) landmarks, where the list
         * position is the landmark index: xPx = x * imageWidth, yPx = y * imageHeight.
         */
        fun fromNormalized(
            normalized: List<Point2>,
            imageWidth: Int,
            imageHeight: Int,
            transformMatrix: FloatArray? = null,
            brightness: Double? = null,
        ) = FrameSample(
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            landmarksPx = normalized.withIndex().associate { (i, p) ->
                i to Point2(p.x * imageWidth, p.y * imageHeight)
            },
            transformMatrix = transformMatrix,
            brightness = brightness,
        )
    }
}
