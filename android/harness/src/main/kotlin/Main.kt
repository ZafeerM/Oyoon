package oyoon.harness

import oyoon.core.geometry.Point2
import oyoon.core.geometry.distance

fun main() {
    val a = Point2(0.0, 0.0)
    val b = Point2(3.0, 4.0)
    println("oyoon harness: distance($a, $b) = ${distance(a, b)}")
}
