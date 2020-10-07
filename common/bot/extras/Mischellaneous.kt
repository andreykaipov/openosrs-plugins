package com.kaipov.common.bot.extras

import com.kaipov.common.bot.BotConfig
import com.kaipov.common.bot.BotPlugin
import net.runelite.api.AnimationID
import net.runelite.api.ChatMessageType
import net.runelite.client.chat.ChatColorType
import net.runelite.client.chat.ChatMessageBuilder
import net.runelite.client.chat.QueuedMessage
import net.runelite.client.ui.overlay.OverlayPanel

fun <C : BotConfig, O : OverlayPanel> BotPlugin<C, O>.sendGameMessage(message: String?) {
    chatMessageManager.queue(
        QueuedMessage
            .builder()
            .type(ChatMessageType.CONSOLE)
            .runeLiteFormattedMessage(ChatMessageBuilder().append(ChatColorType.HIGHLIGHT).append(message).build())
            .build()
    )
}

fun <C : BotConfig, O : OverlayPanel> BotPlugin<C, O>.wait(r: IntRange) {
    Thread.sleep((r.first..r.last.toLong()).random())
}

/**
 * Waits for max amount of milliseconds until the predicate is true.
 * If it times out, runs the final function.
 */
fun <C : BotConfig, O : OverlayPanel> BotPlugin<C, O>.waitUntil(max: Long = 4000L, final: () -> Unit = {}, predicate: () -> Boolean) {
    var timeSlept: Long = 0
    while (!predicate()) {
        val t = (500..1000L).random()
        Thread.sleep(t)
        timeSlept += t

        if (timeSlept > max) {
            final()
            break
        }
    }
}

/**
 * Lol
 * Returns when the player has been idle for more than the given window of time
 */
fun <C : BotConfig, O : OverlayPanel> BotPlugin<C, O>.waitUntilPlayerHasBeenIdleForMoreThan(window: IntRange, timeout: Long) {
    waitUntil(timeout) {
        if (client.localPlayer?.animation == AnimationID.IDLE) {
            wait(window)
            return@waitUntil client.localPlayer?.animation == AnimationID.IDLE
        }
        return@waitUntil false
    }
}
