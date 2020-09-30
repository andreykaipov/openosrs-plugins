package com.kaipov.plugins

import com.kaipov.plugins.common.bot.BotConfig
import com.kaipov.plugins.common.bot.BotOverlay
import com.kaipov.plugins.common.bot.BotPlugin
import com.kaipov.plugins.extensions.client.*
import com.kaipov.plugins.extensions.menuoption.MenuOption.Companion.BANK_CLOSE
import com.kaipov.plugins.extensions.menuoption.MenuOption.Companion.BANK_DEPOSIT_INVENTORY
import com.kaipov.plugins.extensions.menuoption.MenuOption.Companion.DIALOG_NPC_CONTINUE
import com.kaipov.plugins.extensions.menuoption.MenuOption.Companion.DIALOG_PLAYER_CONTINUE
import com.kaipov.plugins.extensions.menuoption.MenuOption.Companion.GAME_OBJECT_FIRST_OPTION
import com.kaipov.plugins.extensions.menuoption.MenuOption.Companion.NPC_CONTACT
import com.kaipov.plugins.extensions.menuoption.MenuOption.Companion.NPC_CONTACT_DARK_MAGE
import com.kaipov.plugins.extensions.menuoption.MenuOption.Companion.NPC_FIRST_OPTION
import com.kaipov.plugins.extensions.menuoption.MenuOption.Companion.OURANIA_TELPORT
import com.kaipov.plugins.extensions.menuoption.MenuOption.Companion.Quantity
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.thread
import net.runelite.api.*
import net.runelite.api.AnimationID.MAGIC_LUNAR_SHARED
import net.runelite.api.ItemID.*
import net.runelite.api.NpcID.ENIOLA
import net.runelite.api.ObjectID.LADDER_29635
import net.runelite.api.ObjectID.RUNECRAFTING_ALTAR
import net.runelite.api.widgets.WidgetInfo
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
class ZMI : BotPlugin<Config, Overlay>(ZMI::class, Config::class, everyOtherTick = 1) {

    private var staminaActive = false

    private val degradedPouches = setOf(MEDIUM_POUCH_5511, LARGE_POUCH_5513, GIANT_POUCH_5515)

    private val runes = setOf(
        FIRE_RUNE, WATER_RUNE, AIR_RUNE, EARTH_RUNE,
        MIND_RUNE, CHAOS_RUNE, DEATH_RUNE, BLOOD_RUNE,
        BODY_RUNE, NATURE_RUNE, LAW_RUNE,
        COSMIC_RUNE, SOUL_RUNE, ASTRAL_RUNE,
    )

    val hasRunes = { -> client.getInventory().count { runes.contains(it.id) } > 0 }
    val hasEssence = { -> client.getInventory().count { it.id == PURE_ESSENCE } > 0 }
    val essenceCount by lazy { -> client.getInventory().count { it.id == PURE_ESSENCE } }

    var state = "unknown"
    override fun setup() {
        state = "unknown"
    }

    override fun act(player: Player) {
        if (client.interacting()) return
        if (client.inRegion(ZMI_OUTSIDE)) return outside(player)
        if (client.inRegion(ZMI_INSIDE)) return inside(player)
        stop()
    }

    private fun outside(player: Player) {
        if (player.isMoving()) return
        state = "outside"
        client.findNearest<GameObject>(ZMI_LADDER)?.let { click(it) } ?: stop()
    }

    private fun inside(player: Player) {
        staminaActive = client.getVar(Varbits.RUN_SLOWED_DEPLETION_ACTIVE) == 1

        // Handled in their own threads, not the client thread, so just skip them
        if (state == "eniola") return
        if (state == "runecrafting") return
        if (state == "npc contact") return

        if (state == "inventory done") {
            // We've setup our inventory, but before going to altar, check if we need to contact the dark mage
            if (degradedPouches.any { client.hasInInventory(it) }) {
                println("has pouch")
                if (client.hasInInventory(COSMIC_RUNE)) return preferNPCContact(player)
                else state = "outside"
                return
            }

            state = "to altar"
            return client.findNearest<GameObject>(RUNECRAFTING_ALTAR, 200)?.let { click(it) } ?: Unit
        }

        if (state == "to altar") {
            if (player.isMoving() && client.hasInInventory(STAMINA_POTION1) && client.getVar(Varbits.RUN_SLOWED_DEPLETION_ACTIVE) == 0) {
                sendGameMessage("gotta go fast")
                return drink(STAMINA_POTION1) ?: Unit
            }

            if (!player.isMoving() && player.isIdle()) {
                return client.findNearest<GameObject>(RUNECRAFTING_ALTAR, 10)?.let { return preferRunecrafting(it, player) }
                    ?: Unit
            }
            return
        }

        if (state == "rc done") {
            state = "going out"
            return click(OURANIA_TELPORT)
        }

        if (state == "going out") return

        if (state == "outside") {
            return client.findNearest<NPC>(ENIOLA, 10)?.let { return preferEniola(it) } ?: Unit
        }

        // Try something
        if (state == "unknown") {
            if (!hasEssence()) {
                return client.findNearest<NPC>(ENIOLA, 10)?.let { state = "outside" }
                    ?: run { state = "inventory done" }
            }

            state = "inventory done"
            return
        }

        state = "unhandled"
        log.error("Unhandled state.")
        stop()

        return
    }

    private fun preferEniola(npc: NPC) {
        var count: Int

        if (state != "eniola") {
            state = "eniola"

            thread {
                click(NPC_FIRST_OPTION(npc.index))
                waitUntil { -> client.hasBankOpen() }

                click(BANK_DEPOSIT_INVENTORY)
                waitUntil { -> !hasRunes() }

                bankWithdraw(PURE_ESSENCE, Quantity.ALL)
                waitUntil { -> hasEssence() }

                count = essenceCount()
                bankFill(SMALL_POUCH)
                waitUntil { essenceCount() < count }

                count = essenceCount()
                bankFill(MEDIUM_POUCH)
                waitUntil { essenceCount() < count }

                count = essenceCount()
                bankFill(LARGE_POUCH)
                waitUntil { essenceCount() < count }

                if (degradedPouches.any { client.hasInInventory(it) }) {
                    bankWithdraw(COSMIC_RUNE)
                    waitUntil { client.hasInInventory(COSMIC_RUNE) }
                }

                client.getWidget(WidgetInfo.MINIMAP_RUN_ORB_TEXT)?.let { it ->
                    val energy = it.text.toInt()
                    if (energy < 60 && !staminaActive) {
                        sendGameMessage("Getting a stamina pot (1)")
                        bankWithdraw(STAMINA_POTION1)
                        waitUntil { client.hasInInventory(STAMINA_POTION1) }
                    }
                }

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
            waitUntil { player.isIdle() }

            if (essenceCount() > 0) {
                click(GAME_OBJECT_FIRST_OPTION(o))
                waitUntil { essenceCount() == 0 && player.isIdle() }
            }

            empty(SMALL_POUCH)
            waitUntil { essenceCount() == 3 }

            empty(MEDIUM_POUCH)
            waitUntil { essenceCount() == 9 }

            if (essenceCount() > 0) {
                click(GAME_OBJECT_FIRST_OPTION(o))
                waitUntil { essenceCount() == 0 && player.isIdle() }
            }
            empty(LARGE_POUCH)
            waitUntil { essenceCount() == 9 }

            if (essenceCount() > 0) {
                click(GAME_OBJECT_FIRST_OPTION(o))
                waitUntil { essenceCount() == 0 && player.isIdle() }
            }

            state = "rc done"
        }
    }

    // Unfortunately checking the hidden property of a widget must be done on
    // the client thread, but we also don't want to sleep on the client thread,
    // so just wait a bit between the dialogues. If there's some lag, we'd
    // retry anyway by going back to the "inventory done" state.
    fun preferNPCContact(player: Player) {
        state = "npc contact"

        thread {
            if (client.hasBankOpen()) {
                click(BANK_CLOSE)
                Thread.sleep((1000..2000L).random())
            }

            println("click np concact")
            click(NPC_CONTACT)
            Thread.sleep((1000..2000L).random())

            println("click dark mage")
            click(NPC_CONTACT_DARK_MAGE)
            Thread.sleep((1000..2000L).random())
            waitUntil { player.animation != MAGIC_LUNAR_SHARED }

            println("click npc continue")
            click(DIALOG_NPC_CONTINUE)
            Thread.sleep((3000..5000L).random())

            println("click player continue")
            click(DIALOG_PLAYER_CONTINUE)
            Thread.sleep((3000..5000L).random())

            state = "inventory done"
        }
    }

}
