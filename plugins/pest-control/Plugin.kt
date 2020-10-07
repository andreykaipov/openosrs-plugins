package com.kaipov.plugins

import com.kaipov.common.bot.BotConfig
import com.kaipov.common.bot.BotOverlay
import com.kaipov.common.bot.BotPlugin
import com.kaipov.common.extensions.client.*
import com.kaipov.common.extensions.menuoption.overwriteWith
import com.kaipov.plugins.PestControl.Regions.PEST_CONTROL_ISLAND
import com.kaipov.plugins.PestControl.Regions.VOID_KNIGHT_OUTPOST
import javax.inject.Inject
import javax.inject.Singleton
import net.runelite.api.*
import net.runelite.api.NpcID.*
import net.runelite.api.events.MenuOptionClicked
import net.runelite.api.queries.NPCQuery
import net.runelite.api.widgets.WidgetInfo
import net.runelite.client.config.ConfigGroup
import net.runelite.client.config.ConfigItem
import net.runelite.client.plugins.PluginDescriptor
import net.runelite.client.plugins.PluginType
import org.pf4j.Extension

@ConfigGroup("com.kaipov.plugins.pestcontrol")
interface Config : BotConfig {
    @JvmDefault
    @ConfigItem(keyName = "boat", name = "Boat", position = 3, description = "")
    fun boatGangplank() = BoatGangplanks.VETERAN
}

@Singleton
class Overlay @Inject constructor(k: Client, plugin: PestControl, c: Config) : BotOverlay(k, plugin, c)

enum class State {
    UNKNOWN,
    ACTING,
    ON_OUTPOST,
    ON_BOAT,
    ON_ISLAND,
    FIGHTING,
    ;

    override fun toString(): String {
        return name.replace("_", " ").toLowerCase()
    }
}

enum class BoatGangplanks(val ID: Int) {
    NOVICE(14315),
    INTERMEDIATE(25631),
    VETERAN(25632),
}

@Extension
@PluginDescriptor(name = "Pest Control", description = "Does Pest Control for you", type = PluginType.UTILITY)
class PestControl : BotPlugin<Config, Overlay>(PestControl::class, Config::class) {
    object Regions {
        const val VOID_KNIGHT_OUTPOST = 10537
        const val PEST_CONTROL_ISLAND = 10536
    }

    var state: State = State.UNKNOWN

    override fun act(player: Player) {
        if (client.interacting()) return
        if (client.inRegion(VOID_KNIGHT_OUTPOST)) return actOnOutpost()
        if (client.inRegion(PEST_CONTROL_ISLAND)) return actOnIsland(player)
    }

    private fun actOnOutpost() {
        state = client.getWidget(WidgetInfo.PEST_CONTROL_BOAT_INFO)?.let { State.ON_BOAT } ?: State.ON_OUTPOST

        if (state == State.ON_BOAT) {
            // Assuming I've got piety on my quick prayers
            if (!client.isPrayerActive(Prayer.PIETY)) {
                client.getWidget(WidgetInfo.MINIMAP_PRAYER_ORB_TEXT)?.let { it ->
                    if (it.text != "0") {
                        client.getWidget(WidgetInfo.MINIMAP_QUICK_PRAYER_ORB)?.let {
                            client.singleClick(it.bounds)
                        }
                    }
                }
            }
            return
        }

        client.findNearest<GameObject>(config.boatGangplank().ID)?.let { gangplank ->
            events.subscribe<MenuOptionClicked>(1) { it.overwriteWith(gangplank) }
            client.singleClickCenterScreenRandom()
            return
        }
    }

    private fun actOnIsland(player: Player) {
        state = State.ON_ISLAND

        val findEnemy = { enemies: List<String> ->
            NPCQuery()
                .filter { enemies.contains(it.name) }
                .filter { it.healthRatio != 0 }
                .isWithinDistance(player.worldLocation, 25)
                .result(client)
                .nearestTo(player)
        }

        NPCQuery().idEquals(PORTAL, PORTAL_1740, PORTAL_1741, PORTAL_1742)
            .isWithinDistance(player.worldLocation, 25)
            .result(client)
            .nearestTo(player)?.let { portal ->
                println("Attacking ${portal.name}")
                events.subscribe<MenuOptionClicked>(1) { it.overwriteWith(portal) }
                client.singleClickCenterScreenRandom()
                return
            }

        findEnemy(listOf("Brawler", "Spinner"))?.let { npc ->
            println("Attacking ${npc.name}")
            events.subscribe<MenuOptionClicked>(1) { it.overwriteWith(npc) }
            client.singleClickCenterScreenRandom()
            return
        }

        findEnemy(listOf("Defiler", "Torcher", "Ravager", "Shifter", "Splatter"))?.let { npc ->
            println("Attacking ${npc.name}")
            events.subscribe<MenuOptionClicked>(1) { it.overwriteWith(npc) }
            client.singleClickCenterScreenRandom()
            return
        }

        // closed gate IDs
        client.findNearestWithin<WallObject>(player.worldLocation, 35, 14235, 14233)?.let { gate ->
            events.subscribe<MenuOptionClicked>(1) { it.overwriteWith(gate) }
            client.singleClickCenterScreenRandom()
            return
        }

    }
//            .result(this).nearestTo(localPlayer)
//
//        client.findNearestAttackableNPC()?.let
//        {
//                npc ->
//            println(npc.definition)
//            return
//        }
//
//        log.info("no targets found")

}

//
//@Subscribe
//fun onMenuEntryAdded(event: MenuEntryAdded) {
//    if ((1..20).random() != 1) return
//    if (event.option == "Cancel" || event.option == "Walk here") return
//
//    if (config.logParams()) {
//        log.info("option=${event.option}; target=${event.target}; id=${event.identifier}; opcode=${event.opcode}; params=(${event.param0}, ${event.param1})")
//    }
//}
//}
