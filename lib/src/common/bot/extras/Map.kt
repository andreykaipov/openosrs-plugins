package common.bot.extras

import com.kaipov.plugins.common.bot.BotConfig
import com.kaipov.plugins.common.bot.BotPlugin
import net.runelite.api.coords.WorldPoint
import net.runelite.client.ui.overlay.OverlayPanel

/**
 * Ideally we can always just look at the local player's world location and use
 * that. However, sometimes we're in an instance like the Pest Control map, and
 * must use the player's local location to resolve this.
 */
fun <C : BotConfig, O : OverlayPanel> BotPlugin<C, O>.inRegion(vararg ids: Int): Boolean {
//    assert(client.isClientThread)

    val player = client.localPlayer ?: return false

    return if (client.isInInstancedRegion) {
        ids.any { it == WorldPoint.fromLocalInstance(client, player.localLocation)?.regionID }
    } else {
        ids.any { it == player.worldLocation.regionID }
    }
}

fun <C : BotConfig, O : OverlayPanel> BotPlugin<C, O>.inRegion(ids: List<Int>): Boolean {
    return inRegion(*ids.toIntArray())
}
