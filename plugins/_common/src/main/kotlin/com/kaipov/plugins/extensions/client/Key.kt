package com.kaipov.plugins.extensions.client

import java.awt.event.KeyEvent
import net.runelite.api.Client

private fun Client.keyEvent(id: Int, key: Char) {
    return canvas.dispatchEvent(KeyEvent(canvas, id, System.currentTimeMillis(), 0, KeyEvent.VK_UNDEFINED, key))
}

/**
 * Unlike the mouse events, I can't really confirm whether this should be the
 * correct order of events. From Ganom.
 */
fun Client.pressKey(key: Char) {
    keyEvent(KeyEvent.KEY_TYPED, key)
    keyEvent(KeyEvent.KEY_PRESSED, key)
    keyEvent(KeyEvent.KEY_RELEASED, key)
}

/**
 * This method must be called on a new thread, if you try to call it on
 * {@link net.runelite.client.callback.ClientThread} it will result in a
 * crash/desynced thread.
 */
fun Client.pressKeys(keys: String) {
    assert(!isClientThread)
    keys.forEach { pressKey(it) }
}
