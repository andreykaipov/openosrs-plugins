package com.kaipov.plugins.extensions.coords

import java.awt.Dimension
import kotlin.math.max
import kotlin.math.min
import net.runelite.api.Point
import net.runelite.api.coords.LocalPoint
import net.runelite.api.coords.WorldPoint

/**
 * Lowerbound should be the SW corner of the rectangle.
 * Upperbound should be the NE corner of the rectangle.
 */

/**
 * The provided corners should be opposite from one another.
 */
fun getRandomPointInZone2D(corner1: WorldPoint, corner2: WorldPoint): WorldPoint {
    assert(corner1.plane == corner2.plane)

    val width = corner2.x - corner1.x
    val height = corner2.y - corner1.y

    val widthRange = if (width < 0) (width..0) else (0..width)
    val heightRange = if (height < 0) (height..0) else (0..height)

    return WorldPoint(
        corner1.x + widthRange.random(),
        corner1.y + heightRange.random(),
        corner1.plane,
    )
}

/**
 * The provided corners should be opposite from one another.
 */
fun WorldPoint.isWithinZone2D(corner1: WorldPoint, corner2: WorldPoint): Boolean {
    assert(plane == corner1.plane && plane == corner2.plane)

    val xMin = min(corner1.x, corner2.x)
    val yMin = min(corner1.y, corner2.y)
    val xMax = max(corner1.x, corner2.x)
    val yMax = max(corner1.y, corner2.y)

    return x in xMin..xMax && y in yMin..yMax
}

fun getRandomPointInZone(corner1: LocalPoint, corner2: LocalPoint): LocalPoint {
    val width = corner2.x - corner1.x
    val height = corner2.y - corner1.y

    val widthRange = if (width < 0) (width..0) else (0..width)
    val heightRange = if (height < 0) (height..0) else (0..height)

    return LocalPoint(
        corner1.x + widthRange.random(),
        corner1.y + heightRange.random(),
    )
}

fun LocalPoint.isWithin(corner1: LocalPoint, corner2: LocalPoint): Boolean {
    val xMin = min(corner1.x, corner2.x)
    val yMin = min(corner1.y, corner2.y)
    val xMax = max(corner1.x, corner2.x)
    val yMax = max(corner1.y, corner2.y)

    return x in xMin..xMax && y in yMin..yMax
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
