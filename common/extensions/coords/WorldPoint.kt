package com.kaipov.common.extensions.coords

import java.awt.Dimension
import net.runelite.api.Point
import net.runelite.api.coords.WorldPoint

/**
 * Lowerbound should be the SW corner of the rectangle.
 * Upperbound should be the NE corner of the rectangle.
 */

object WorldPoints {
    fun getRandomPointInZone2D(lowerBound: WorldPoint, upperBound: WorldPoint): WorldPoint {
        val width = upperBound.x - lowerBound.x
        val height = upperBound.y - lowerBound.y
        return WorldPoint(
            lowerBound.x + (0..width).random(),
            lowerBound.y + (0..height).random(),
            lowerBound.plane,
        )
    }
}

/**
 * Returns a point near our point lol
 */
fun Point.withNoise(noise: Int = 50): Point {
    return Point(x + (-noise..noise).random(), y + (-noise..noise).random())
}

fun Point.withinBounds(dimension: Dimension): Boolean {
    return x <= dimension.width && y <= dimension.height
}

// In an open space with only ground objects, a specified square radius of n, returns (2n+1)^2 squares.
// However, it's unlikely all squares around are simple ground objects, so this is just an upper bound.
// .filter {
//     abs(it.worldLocation.x - player.worldLocation.x) <= 1 && abs(it.worldLocation.y - player.worldLocation.y) <= 1
// }
