package com.kaipov.plugins

import com.kaipov.plugins.States.*
import com.kaipov.plugins.common.bot.BotConfig
import com.kaipov.plugins.common.bot.BotOverlay
import com.kaipov.plugins.common.bot.DetailedStates.Stop
import com.kaipov.plugins.common.bot.State
import com.kaipov.plugins.common.bot.StateBotPlugin
import com.kaipov.plugins.common.bot.States.Start
import com.kaipov.plugins.common.bot.extras.sendGameMessage
import com.kaipov.plugins.common.bot.extras.wait
import com.kaipov.plugins.extensions.client.findNearest
import com.kaipov.plugins.extensions.client.interacting
import com.kaipov.plugins.extensions.coords.getRandomPointInZone
import com.kaipov.plugins.extensions.coords.isWithin
import com.kaipov.plugins.extensions.menuoption.MenuOption.Companion.GAME_OBJECT_FIRST_OPTION
import com.kaipov.plugins.extensions.menuoption.MenuOption.Companion.NPC_SECOND_OPTION
import common.bot.extras.inRegion
import extensions.ifNotTrue
import javax.inject.Inject
import javax.inject.Singleton
import net.runelite.api.Client
import net.runelite.api.GameObject
import net.runelite.api.NPC
import net.runelite.api.ObjectID.*
import net.runelite.api.Player
import net.runelite.api.coords.LocalPoint
import net.runelite.api.queries.NPCQuery
import net.runelite.client.config.ConfigGroup
import net.runelite.client.config.ConfigItem
import net.runelite.client.plugins.PluginDescriptor
import net.runelite.client.plugins.PluginType
import org.pf4j.Extension

enum class BoatGangplanks(val ID: Int, val ladderID: Int) {
    NOVICE(GANGPLANK_14315, LADDER_14314),
    INTERMEDIATE(GANGPLANK_25631, LADDER_25629),
    VETERAN(GANGPLANK_25632, LADDER_25630),
}

@ConfigGroup("com.kaipov.plugins.pestcontrol")
interface Config : BotConfig {
    @JvmDefault
    @ConfigItem(keyName = "boat", name = "Boat", position = 3, description = "")
    fun boatGangplank() = BoatGangplanks.VETERAN
}

@Singleton
class Overlay @Inject constructor(k: Client, plugin: PestControl, c: Config) : BotOverlay(k, plugin, c)

enum class States : State {
    Outpost,
    Boat,
    Island,
    GoToCenter,
    AtCenter,
    Attacking,
    ;
}

const val VOID_KNIGHT_OUTPOST = 10537
const val PEST_CONTROL_ISLAND = 10536
val ISLAND_CENTER_NW = LocalPoint(5951, 4543)
val ISLAND_CENTER_SE = LocalPoint(6591, 3903)

@Extension
@PluginDescriptor(
    name = "Pest Control",
    description = "Does Pest Control for you",
    tags = ["pest", "control", "minigame"],
    type = PluginType.UTILITY
)
class PestControl : StateBotPlugin<Config, Overlay>(PestControl::class, Config::class, 1) {
    // Wait two minutes max for any condition in this plugin
    private fun waitUntil(action: () -> Boolean) = waitUntil(240 * 1000L, action)

    override fun action() {
        state(Start) {
            findState()
        }

        state(Outpost) {
            wait(1000..1500)
            waitUntil { boat != null }
            click(GAME_OBJECT_FIRST_OPTION(boat!!))
            waitUntil { ladder != null }
            Boat
        }

        state(Boat) {
            sendGameMessage("I'm on a boat bitch")
            waitUntil { inRegion(PEST_CONTROL_ISLAND) }
            Island
        }

        state(Island) {
            client.localPlayer
                ?.localLocation
                ?.isWithin(ISLAND_CENTER_NW, ISLAND_CENTER_SE)
                .ifNotTrue { return@state GoToCenter }
            AtCenter // noop
        }

        state(GoToCenter) {
            walkTo(getRandomPointInZone(ISLAND_CENTER_NW, ISLAND_CENTER_SE))
            waitUntil {
                client.localPlayer
                    ?.localLocation
                    ?.isWithin(ISLAND_CENTER_NW, ISLAND_CENTER_SE) == true
            }
            AtCenter
        }

        state(AtCenter) {
            if (!client.interacting()) {
                findEnemy("Shifter")?.let { click(NPC_SECOND_OPTION(it)) }
            }
            AtCenter
        }
    }

    var boat: GameObject? = null
    var ladder: GameObject? = null

    override fun act(player: Player) {
        if (client.interacting()) {
            if (state == Island || state == AtCenter) {
                client.localPlayer
                    ?.localLocation
                    ?.isWithin(ISLAND_CENTER_NW, ISLAND_CENTER_SE)
                    .ifNotTrue { state = GoToCenter }
            }
            return
        }
        if (player.isMoving()) return

        boat = client.findNearest<GameObject>(config.boatGangplank().ID)
        ladder = client.findNearest<GameObject>(config.boatGangplank().ladderID, distance = 1)
        state = findState() // Automatically fixes the state if necessary
    }

    private fun findState(): State {
//        if (state !in listOf(Start, Boat, Outpost, Island)) return state

        if (inRegion(VOID_KNIGHT_OUTPOST)) {
            return if (ladder != null) Boat else Outpost
        }

        if (inRegion(PEST_CONTROL_ISLAND)) {
            return Island
        }

        return Stop.with("Bro where are you?")
    }

    fun findEnemy(vararg enemies: String): NPC? {
        return NPCQuery()
            .filter { enemies.contains(it.name) }
            .filter { it.healthRatio != 0 }
            .isWithinDistance(player?.worldLocation, 5)
            .result(client)
            .nearestTo(player)
    }
}

/*
class PestControl2 : BotPlugin<Config, Overlay>(PestControl::class, Config::class) {
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
*/
