package com.kaipov.plugins

import com.kaipov.plugins.common.bot.BotConfig
import com.kaipov.plugins.common.bot.BotOverlay
import com.kaipov.plugins.common.bot.BotPlugin
import com.kaipov.plugins.extensions.client.*
import com.kaipov.plugins.extensions.menuoption.MenuOption.Companion.BANK_DEPOSIT_INVENTORY
import com.kaipov.plugins.extensions.menuoption.MenuOption.Companion.DIALOG_NPC_CONTINUE
import com.kaipov.plugins.extensions.menuoption.MenuOption.Companion.DIALOG_PLAYER_CONTINUE
import com.kaipov.plugins.extensions.menuoption.MenuOption.Companion.GAME_OBJECT_FIRST_OPTION
import com.kaipov.plugins.extensions.menuoption.MenuOption.Companion.NPC_CONTACT
import com.kaipov.plugins.extensions.menuoption.MenuOption.Companion.NPC_CONTACT_DARK_MARGE
import com.kaipov.plugins.extensions.menuoption.MenuOption.Companion.NPC_FIRST_OPTION
import com.kaipov.plugins.extensions.menuoption.MenuOption.Companion.OURANIA_TELPORT
import com.kaipov.plugins.extensions.menuoption.MenuOption.Companion.Quantity
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.thread
import net.runelite.api.AnimationID.IDLE
import net.runelite.api.Client
import net.runelite.api.GameObject
import net.runelite.api.ItemID.*
import net.runelite.api.NPC
import net.runelite.api.NpcID.ENIOLA
import net.runelite.api.ObjectID.LADDER_29635
import net.runelite.api.ObjectID.RUNECRAFTING_ALTAR
import net.runelite.api.Player
import net.runelite.client.config.ConfigGroup
import net.runelite.client.plugins.PluginDescriptor
import net.runelite.client.plugins.PluginType
import net.runelite.client.ui.overlay.components.table.TableComponent
import org.pf4j.Extension

const val ZMI_OUTSIDE = 9778
const val ZMI_INSIDE = 12119
const val ZMI_LADDER = LADDER_29635

@ConfigGroup("com.kaipov.plugins.zmi")
interface Config : BotConfig {}

@Singleton
class Overlay @Inject constructor(k: Client, plugin: ZMI, c: Config) : BotOverlay(k, plugin, c) {
    override fun addExtraRows(table: TableComponent) {
        val p = plugin as ZMI
        table.addRow("State:", p.state)
    }
}

@Extension
@PluginDescriptor(name = "ZMI", description = "Does ZMI for you", type = PluginType.UTILITY)
class ZMI : BotPlugin<Config, Overlay>(ZMI::class, Config::class) {

    override fun act(player: Player) {
        if (client.interacting()) return
        if (client.inRegion(ZMI_OUTSIDE)) return outside(player)
        if (client.inRegion(ZMI_INSIDE)) return inside(player)
        stop()
    }

    private fun outside(player: Player) {
        client.findNearest<GameObject>(ZMI_LADDER)?.let { click(it) } ?: stop()
    }

    private var filling = false
    private var filled = false
    private var emptied = false

    var state = "unknown"

    override fun setup() {
        state = "unknown"
        emptying = false
        emptyAgain = true
        fillAgain = true
//        clientThread.invoke(Runnable {
//            if (client.inRegion(ZMI_INSIDE)) {
//                emptying = client.hasInInventory(PURE_ESSENCE)
//                filling = !emptying
//            } else {
//            }
//        })
    }

    private val wait = { -> Thread.sleep((1000..2000L).random()) }
    private fun waitUntil(max: Long = 3000L, predicate: () -> Boolean) {
        var timeSlept: Long = 0
        do {
            val t = (500..800L).random()
            Thread.sleep(t)
            timeSlept += t

            if (timeSlept > max) break
        } while (!predicate())
    }


    val clickAltar = { ->
        clientThread.invoke(Runnable {
            client.findNearest<GameObject>(RUNECRAFTING_ALTAR)?.run { click(GAME_OBJECT_FIRST_OPTION(this)) }
        })
    }

    var emptySmallPouch = true
    var emptyMediumPouch = true
    var emptyLargePouch = true
    var inventoryBefore = 0
    var inventoryAfter = 0

    private var emptying = false
    private var emptyAgain = true
    private var fillAgain = true

    override val everyOtherTick = 2

    val DEGRADED_POUCHES = setOf(MEDIUM_POUCH_5511, LARGE_POUCH_5513, GIANT_POUCH_5515)

    val RUNES = setOf(
        FIRE_RUNE, WATER_RUNE, AIR_RUNE, EARTH_RUNE,
        MIND_RUNE, CHAOS_RUNE, DEATH_RUNE, BLOOD_RUNE,
        BODY_RUNE, NATURE_RUNE, LAW_RUNE,
        COSMIC_RUNE, SOUL_RUNE, ASTRAL_RUNE,
    )

    val hasRunes = { -> client.getInventory().count { RUNES.contains(it.id) } > 0 }
    val hasEssence = { -> client.getInventory().count { it.id == PURE_ESSENCE } > 0 }
    val essenceCount = { -> client.getInventory().count { it.id == PURE_ESSENCE } }

    private fun preferEniola(npc: NPC) {
        if (state != "eniola") {
            state = "eniola"

            thread {
                click(NPC_FIRST_OPTION(npc.index))
                waitUntil { -> client.hasBankOpen() }

                click(BANK_DEPOSIT_INVENTORY)
                waitUntil { -> !hasRunes() }

                bankWithdraw(PURE_ESSENCE, Quantity.ALL)
                waitUntil { -> hasEssence() }

                var count = essenceCount()
                bankFill(SMALL_POUCH)
                waitUntil { essenceCount() < count }

                count = essenceCount()
                bankFill(MEDIUM_POUCH)
                waitUntil { essenceCount() < count }

                count = essenceCount()
                bankFill(LARGE_POUCH)
                waitUntil { essenceCount() < count }

                count = essenceCount()
                bankWithdraw(PURE_ESSENCE, Quantity.ALL)
                waitUntil { essenceCount() > count }

                state = "inventory done"
            }
        }
    }

    private fun preferRunecrafting(o: GameObject, player: Player) {
        state = "runecrafting"
        thread {
            if (essenceCount() > 0) {
                click(GAME_OBJECT_FIRST_OPTION(o))
                waitUntil { essenceCount() == 0 && player.animation == IDLE }
            }

            empty(SMALL_POUCH)
            waitUntil { essenceCount() == 3 }

            empty(MEDIUM_POUCH)
            waitUntil { essenceCount() == 9 }

            if (essenceCount() > 0) {
                click(GAME_OBJECT_FIRST_OPTION(o))
                waitUntil { essenceCount() == 0 && player.animation == IDLE }
            }

            empty(LARGE_POUCH)
            waitUntil { essenceCount() == 9 }

            if (essenceCount() > 0) {
                click(GAME_OBJECT_FIRST_OPTION(o))
                waitUntil { essenceCount() == 0 && player.animation == IDLE }
            }

            state = "rc done"
        }
    }

    private fun inside(player: Player) {
        if (state != "npc contact" && DEGRADED_POUCHES.any { client.hasInInventory(it) }) {
            state = "npc contact"
            thread {
                click(NPC_CONTACT)
                click(NPC_CONTACT_DARK_MARGE); wait()
                click(DIALOG_NPC_CONTINUE); wait()
                click(DIALOG_PLAYER_CONTINUE); wait()
            }
            return
        }

        // Handled in their own threads, not the client thread
        if (state == "eniola") return
        if (state == "runecrafting") return

        if (state == "inventory done") {
            state = "to altar"
            return client.findNearest<GameObject>(RUNECRAFTING_ALTAR, 200)?.let { click(it) } ?: Unit
        }

        if (state == "to altar") {
            if (player.canAct()) {
                return client.findNearest<GameObject>(RUNECRAFTING_ALTAR, 10)?.let { return preferRunecrafting(it, player) }
                    ?: Unit
            }
            return
        }

        if (state == "rc done") {
            state = "outside"
            return click(OURANIA_TELPORT)
        }

        if (state == "outside") {
            return client.findNearest<NPC>(ENIOLA, 10)?.let { return preferEniola(it) } ?: Unit
        }

        // Try something
        if (state == "unknown") {
            state = if (!hasEssence()) {
                "outside"
            } else {
                "to altar"
            }
            return
        }

        state = "unhandled"
        println("UNHANDLED")
        stop()

        return
//
//        if (!client.hasInInventory(PURE_ESSENCE)) {
//            return bankWithdraw(PURE_ESSENCE, Quantity.ALL) ?: Unit
//        }
////
//        if (!client.hasInInventory(PURE_ESSENCE)) {
//            return bankWithdraw(PURE_ESSENCE, Quantity.ALL) ?: stop()
//        }
    }
}
