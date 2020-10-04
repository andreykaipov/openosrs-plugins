package com.kaipov.plugins

import com.kaipov.plugins.Action.CRAFT_LEATHER
import com.kaipov.plugins.Action.TAN_LEATHER
import com.kaipov.plugins.common.bot.DetailedStates.Stop
import com.kaipov.plugins.common.bot.State
import com.kaipov.plugins.common.bot.StateBotPlugin
import com.kaipov.plugins.common.bot.extras.waitUntil
import com.kaipov.plugins.craftleather.craftLeather
import com.kaipov.plugins.extensions.client.findNearest
import com.kaipov.plugins.extensions.client.hasBankOpen
import com.kaipov.plugins.extensions.client.pressKey
import com.kaipov.plugins.extensions.menuoption.MenuOption
import com.kaipov.plugins.tanleather.tanLeather
import java.awt.event.KeyEvent
import net.runelite.api.ObjectID
import net.runelite.api.Player
import net.runelite.api.WallObject
import net.runelite.client.plugins.PluginDescriptor
import net.runelite.client.plugins.PluginType
import org.pf4j.Extension

enum class AfkStates : State {
    Withdraw,
    Act,
    Deposit,
}

enum class Action {
    TAN_LEATHER,
    CRAFT_LEATHER,
}

@Extension
@PluginDescriptor(
    name = "AFK GE",
    description = "Does AFK stuff at the GE for you",
    tags = ["afk", "ge", "grand", "exchange"],
    type = PluginType.UTILITY
)
class AFKGE : StateBotPlugin<Config, Overlay>(AFKGE::class, Config::class) {
    override fun action() {
        when (val action = config.action()) {
            TAN_LEATHER -> tanLeather()
            CRAFT_LEATHER -> craftLeather()
            else -> state = Stop.with("Unknown action $action")
        }
    }

    var geBooth: WallObject? = null

    override fun act(player: Player) {
        geBooth = client.findNearest<WallObject>(ObjectID.GRAND_EXCHANGE_BOOTH)
            ?: run {
                state = Stop.with("Couldn't find the GE booth :(")
                return
            }
    }

    // Idempotent. If the bank is already open, we don't do anything.
    fun bankOpen() {
        assert(!client.isClientThread)
        if (client.hasBankOpen()) return
        waitUntil { geBooth != null }
        click(MenuOption.GAME_OBJECT_SECOND_OPTION(geBooth!!))
        waitUntil { client.hasBankOpen() }
    }

    fun bankClose() {
        assert(!client.isClientThread)
        client.pressKey(KeyEvent.VK_ESCAPE)
    }
}
