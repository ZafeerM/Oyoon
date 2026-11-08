package oyoon.core.model

import oyoon.core.geometry.Point2

/**
 * Landmarks of one camera frame, in image pixels.
 *
 * Distances must only be measured in pixels: on a non-square image the
 * normalized 0-to-1 x and y units differ, so normalized distances are wrong.
 */
data class FrameSample(
    val imageWidth: Int,
    val imageHeight: Int,
    /** Landmark index to position in pixels. */
    val landmarksPx: Map<Int, Point2>,
) {
    init {
        require(imageWidth > 0 && imageHeight > 0) {
            "image size must be positive, was ${imageWidth}x$imageHeight"
        }
    }

    /** Pixel position of landmark [index]; throws if the frame does not have it. */
    fun px(index: Int): Point2 =
        landmarksPx[index] ?: throw IllegalArgumentException("frame is missing landmark $index")

    companion object {
        /**
         * Builds a sample from normalized (0-to-1) landmarks, where the list
         * position is the landmark index: xPx = x * imageWidth, yPx = y * imageHeight.
         */
        fun fromNormalized(normalized: List<Point2>, imageWidth: Int, imageHeight: Int) =
            FrameSample(
                imageWidth = imageWidth,
                imageHeight = imageHeight,
                landmarksPx = normalized.withIndex().associate { (i, p) ->
                    i to Point2(p.x * imageWidth, p.y * imageHeight)
                },
            )
    }
}
