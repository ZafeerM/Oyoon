package oyoon.core.geometry

import kotlin.math.hypot

/** A point in 2D, in whatever unit the caller uses (pixels, millimetres, …). */
data class Point2(val x: Double, val y: Double)

/** Euclidean distance between [a] and [b]. */
fun distance(a: Point2, b: Point2): Double = hypot(b.x - a.x, b.y - a.y)
