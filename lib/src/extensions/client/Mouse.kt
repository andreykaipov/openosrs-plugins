package com.kaipov.plugins.extensions.client

import java.awt.Rectangle
import java.awt.event.MouseEvent
import kotlin.concurrent.thread
import net.runelite.api.Client
import net.runelite.api.Point


private fun Client.mouseEvent(id: Int, point: Point) {
    return canvas.dispatchEvent(
        MouseEvent(canvas, id, System.currentTimeMillis(), 0, point.x, point.y, 1, false, 1)
    )
}

/**
 * This method must be called on a new thread, if you try to call it on
 * {@link net.runelite.client.callback.ClientThread} it will result in a
 * crash/desynced thread.
 *
 * Order of the mouse event is important.
 * See https://stackoverflow.com/a/52912777/4085283.
 */
private fun Client.click(p: Point) {
    var q = p

    if (isStretchedEnabled) {
        q = Point(
            p.x * (stretchedDimensions.width / realDimensions.width),
            p.y * (stretchedDimensions.height / realDimensions.height)
        )
    }

    thread {
        mouseEvent(MouseEvent.MOUSE_ENTERED, q);
        mouseEvent(MouseEvent.MOUSE_MOVED, q);
        mouseEvent(MouseEvent.MOUSE_PRESSED, q)
        mouseEvent(MouseEvent.MOUSE_RELEASED, q)
        mouseEvent(MouseEvent.MOUSE_CLICKED, q)
        mouseEvent(MouseEvent.MOUSE_EXITED, q);
    }
}

/**
 * A rectangle's (x,y) is the upper-left coordinates of the rectangle on the
 * screen. For x and y, take half of the rectangle's width and height
 * respectively to get the center of the rectangle. Then add in some randomness
 * to get a point in the smaller rectangular center.
 */
private fun Client.click(r: Rectangle) {
    fun Rectangle.findRandomCenterPoint() = Point(
        (x + width / 2) + (-1 * width..width).map { it / 5 }.random(),
        (y + height / 2) + (-1 * height..height).map { it / 5 }.random(),
    )

    click(r.findRandomCenterPoint())
}

fun Client.singleClickCenterScreenRandom() {
    singleClick(Point(centerX + (-100..100).random(), centerY + (-100..100).random()))
}

fun Client.singleClick(p: Point) = thread {
//    Thread.sleep((50..150).random().toLong())
    click(p)
}

fun Client.singleClick(r: Rectangle) = thread {
    Thread.sleep((50..150).random().toLong())
    click(r)
}

fun Client.doubleClick(p: Point) = thread {
    Thread.sleep((50..150).random().toLong())
    click(p)
    Thread.sleep((200..400).random().toLong())
    click(p)
}

fun Client.doubleClick(r: Rectangle) = thread {
    Thread.sleep((0..100).random().toLong())
    click(r)
    Thread.sleep((200..400).random().toLong())
    click(r)
}
