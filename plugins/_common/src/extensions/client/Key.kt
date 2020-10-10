package com.kaipov.plugins.extensions.client

import java.awt.event.KeyEvent
import java.awt.event.KeyEvent.CHAR_UNDEFINED
import java.awt.event.KeyEvent.VK_UNDEFINED
import net.runelite.api.Client

/**
 * Pressing and releasing a key on the keyboard results in the generating the following key events (in order):
 *   KEY_PRESSED
 *   KEY_TYPED (is only generated if a valid Unicode character could be generated.)
 *   KEY_RELEASED
 *
 * See https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/java/awt/event/KeyEvent.html
 */

private fun Client.keyEvent(id: Int, key: Char) {
    return canvas.dispatchEvent(KeyEvent(canvas, id, System.currentTimeMillis(), 0, VK_UNDEFINED, key))
}


/**
 * For keys that map to unicode characters like abc123.
 *
 * This method must be called on a new thread, if you try to call it on
 * {@link net.runelite.client.callback.ClientThread} it will result in a
 * crash/desynced thread.
 */
fun Client.pressKey(key: Char) {
    assert(!isClientThread)
    keyEvent(KeyEvent.KEY_PRESSED, key)
    keyEvent(KeyEvent.KEY_TYPED, key)
    keyEvent(KeyEvent.KEY_RELEASED, key)
}

fun Client.pressKeys(keys: String) {
    keys.forEach { pressKey(it) }
}

/**
 * For keys that don't map to unicode characters like
 * VK_ENTER, VK_ESCAPE, or VK_SPACE.
 */
private fun Client.keyEvent(id: Int, keyCode: Int) {
    return canvas.dispatchEvent(KeyEvent(canvas, id, System.currentTimeMillis(), 0, keyCode, CHAR_UNDEFINED))
}

fun Client.pressKey(keyCode: Int) {
    assert(!isClientThread)
    keyEvent(KeyEvent.KEY_PRESSED, keyCode)
    keyEvent(KeyEvent.KEY_RELEASED, keyCode)
}
