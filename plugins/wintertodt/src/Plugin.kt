package com.kaipov.plugins

import com.kaipov.plugins.State.*
import com.kaipov.plugins.Wintertodt.Braziers.BROKEN
import com.kaipov.plugins.Wintertodt.Braziers.BURNING
import com.kaipov.plugins.Wintertodt.Braziers.UNLIT
import com.kaipov.plugins.Wintertodt.Regions.FEROX_ENCLAVE
import com.kaipov.plugins.Wintertodt.Regions.FFA_PORTAL
import com.kaipov.plugins.Wintertodt.Regions.WINTERTODT_INSIDE
import com.kaipov.plugins.Wintertodt.Regions.WINTERTODT_OUTSIDE
import com.kaipov.plugins.common.bot.BotConfig
import com.kaipov.plugins.common.bot.BotOverlay
import com.kaipov.plugins.common.bot.BotPlugin
import com.kaipov.plugins.common.bot.extras.sendGameMessage
import com.kaipov.plugins.extensions.client.*
import com.kaipov.plugins.extensions.menuoption.MenuOption
import com.kaipov.plugins.extensions.menuoption.MenuOption.Companion.Quantity
import com.kaipov.plugins.extensions.widgets.WidgetInfo.MINIMAP_HEALTH_ORB_TEXT
import com.kaipov.plugins.extensions.widgets.getWidget
import java.lang.Thread.sleep
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.thread
import net.runelite.api.*
import net.runelite.api.AnimationID.IDLE
import net.runelite.api.AnimationID.WOODCUTTING_DRAGON
import net.runelite.api.EquipmentInventorySlot.AMULET
import net.runelite.api.EquipmentInventorySlot.RING
import net.runelite.api.ItemID.*
import net.runelite.api.ObjectID.*
import net.runelite.api.coords.WorldPoint
import net.runelite.api.events.GameTick
import net.runelite.client.config.ConfigGroup
import net.runelite.client.config.ConfigItem
import net.runelite.client.plugins.PluginDescriptor
import net.runelite.client.plugins.PluginType
import net.runelite.client.ui.overlay.components.table.TableComponent
import org.pf4j.Extension

@ConfigGroup("com.kaipov.plugins.wintertodt")
interface Config : BotConfig {
    @ConfigItem(keyName = "boat", name = "Boat", position = 3, description = "")
    @JvmDefault
    fun boatGangplank() = 12
}

@Singleton
class Overlay @Inject constructor(k: Client, plugin: Wintertodt, c: Config) : BotOverlay(k, plugin, c) {
    override fun addExtraRows(table: TableComponent) {
        val p = plugin as Wintertodt
        table.addRow("Interrupted:", p.brazingInterruptedByCold.toString())
        table.addRow("HP:", p.currentHP.toString())
    }
}

@Extension
@PluginDescriptor(
    name = "Wintertodt",
    description = "Does Wintertodt for you",
    type = PluginType.UTILITY
)
class Wintertodt : BotPlugin<Config, Overlay>(Wintertodt::class, Config::class, everyOtherTick = 1) {

    // The plane seems to be 0 when inspecting the tile, but via a query it's 1.
    // In any case, just include both.
    private val WaitingAreaSWCorner = WorldPoint(1627, 3979, 0)
    private val WaitingAreaNECorner = WorldPoint(1633, 3987, 1)

    object Braziers {
        const val UNLIT = BRAZIER_29312
        const val BROKEN = BRAZIER_29313
        const val BURNING = BURNING_BRAZIER_29314
    }

    val WOODCUTTING = WOODCUTTING_DRAGON // todo make this configurable

    object Regions {
        const val WINTERTODT_INSIDE = 6462
        const val WINTERTODT_OUTSIDE = 6461
        const val FEROX_ENCLAVE_WEST = 12344
        const val FEROX_ENCLAVE_EAST = 12600
        const val FFA_PORTAL_NW = 13130
        const val FFA_PORTAL_SW = 13129
        const val FFA_PORTAL_NE = 13386
        const val FFA_PORTAL_SE = 13385
        val FEROX_ENCLAVE = listOf(FEROX_ENCLAVE_WEST, FEROX_ENCLAVE_EAST)
        val FFA_PORTAL = listOf(FFA_PORTAL_NW, FFA_PORTAL_SW, FFA_PORTAL_NE, FFA_PORTAL_SE)
        // Wintertodt waiting area
        // SW corner - 1627, 3979, 0
        // NW corner - 1627, 3987, 0
        // SE corner - 1633, 3979, 0
        // NE corner - 1633, 3987, 0
    }
    // 29325 is the fallen snow from the snow attack in wintertodt
    // 26690 is the glistening snow right before it falls (animation of 7000 even though it's a game object?)

    // 	public static final int FIREMAKING = 733; // lighting brazier
    // 	public static final int LOOKING_INTO = 832; // feeding logs

    inline fun <T> Boolean?.ifTrue(supplier: () -> T) = if (this == true) supplier() else null
    inline fun <T> Boolean?.ifFalse(supplier: () -> T) = if (this == false) supplier() else null
    inline fun <T> Boolean?.ifNotTrue(supplier: () -> T) = if (this != true) supplier() else null
    inline fun <T> Boolean?.ifNotFalse(supplier: () -> T) = if (this != false) supplier() else null


    override fun act(player: Player) {
        if (client.interacting()) return
        if (client.inRegion(WINTERTODT_INSIDE)) return actInside(player)
        if (client.inRegion(WINTERTODT_OUTSIDE)) return actOutside(player)

        if (client.inRegion(FEROX_ENCLAVE)) return actInEnclave(player)
        if (client.inRegion(FFA_PORTAL)) return actInFFAPortal(player)
    }

    private var setupLoadout: Thread? = null

    /**
     * Within the Enclave, we run to the bank, deposit our shit, withdraw our shit, and go to the FFA portal.
     * Assumes we have wines, games necklaces, and rings of dueling in our bank.
     */
    fun actInEnclave(p: Player) {
        if (!client.hasBankOpen()) return client.findNearest<GameObject>(BANK_CHEST_26711)?.let { click(it) } ?: Unit

        val wait = { -> sleep((400..1100L).random()) } // time between banking actions

        // Make sure our banking thread only runs once by examining its state. Only continue when it has terminated.
        when (setupLoadout?.state) {
            null -> {
                setupLoadout = thread {
                    sleep((5000..8000L).random())
                    client.hasInInventory(SUPPLY_CRATE).ifTrue { wait(); bankDeposit(SUPPLY_CRATE, Quantity.ALL) }
                    client.hasInInventory(JUG).ifTrue { wait(); bankDeposit(JUG, Quantity.ALL) }
                    client.hasInInventory(JUG_OF_WINE).ifTrue { wait(); bankDeposit(JUG_OF_WINE, Quantity.ALL) }
                    client.hasInBank(JUG_OF_WINE).ifTrue { wait(); bankWithdraw(JUG_OF_WINE, Quantity.TEN); (1..3).forEach { wait(); bankWithdraw(JUG_OF_WINE); } } // 13 lol
                        ?: run { sendGameMessage("You ain't got no wines bruv"); stop() }
                    client.isWearing(AMULET).ifFalse { wait(); bankWithdraw(GAMES_NECKLACE8); wait();wait(); bankWear(GAMES_NECKLACE8) }
                    client.isWearing(RING).ifFalse { wait(); bankWithdraw(RING_OF_DUELING8); wait();wait(); bankWear(RING_OF_DUELING8) }
                }
                return
            }
            Thread.State.TERMINATED -> Unit // continue
            else -> return
        }

        client.findNearest<GameObject>(FREEFORALL_PORTAL)?.let { click(it) }
    }

    fun actInFFAPortal(p: Player) {
        client.getWorn(AMULET)?.let {
            val name = client.getItemDefinition(it.id).name
            if (!name.startsWith("Games necklace")) {
                sendGameMessage("Why don't we have a games necklace on?")
                stop()
            }
            if (!p.isTeleporting()) {
                return click(MenuOption.GAMES_NECKLACE_TO_WINTERTODT)
            }
        }
    }

    private var xpDropFM = false
    private var xpDropAny = false

    var state = UNKNOWN
    var brazingInterruptedByCold = false
    var currentHP = -1
    var idleFor = 0

    private val eatingLifecycle = Any()

    override fun setup() {
        setupLoadout = null
        state = UNKNOWN
        brazingInterruptedByCold = false
        client.getWidget(MINIMAP_HEALTH_ORB_TEXT)?.let { currentHP = it.text.toInt() } ?: run { stop() }

        events.subscribe<GameTick>(eatingLifecycle) here@{
            if (client.inRegion(FEROX_ENCLAVE + FFA_PORTAL)) return@here

            val player = client.localPlayer
            if (player.canAct() && client.getBoostedSkillLevel(Skill.HITPOINTS) < 60 && player?.animation != AnimationID.CONSUMING) {
                eat(JUG_OF_WINE)
            }
        }
    }

    override fun teardown() {
        events.unregister(eatingLifecycle)
    }

    fun clickBraziers(vararg braziers: Int, searchDistance: Int = 10): (Player) -> Unit? {
        return { p ->
            client.findNearestWithin<GameObject>(p.worldLocation, searchDistance, *braziers)?.let {
                brazingInterruptedByCold = false
                state = BRAZING
                click(it)
            }
        }
    }

    fun clickRoots(): (Player) -> Unit? {
        return { p ->
            client.findNearestWithin<GameObject>(p.worldLocation, 50, BRUMA_ROOTS)?.let {
                state = CHOPPING
                click(it)
            }
        }
    }

    fun actInside(player: Player) {
        if (client.getInventory().count { it.id == SUPPLY_CRATE } == 2) {
            client.getWorn(RING)?.let {
                val name = client.getItemDefinition(it.id).name
                if (!name.startsWith("Ring of dueling")) {
                    sendGameMessage("Why don't we have a dueling ring on?")
                    stop()
                }
                if (!player.isTeleporting()) {
                    return click(MenuOption.RING_OF_DUELING_TO_FEROX_ENCLAVE)
                }
            }
        }

        // Wait around until around 0-3 seconds (0-5 ticks) before the Wintertodt awakens.
        if (client.getVar(Varbits.WINTERTODT_TIMER) > (0..5).random()) {
            state = WAITING
//            clickBraziers(UNLIT)(player)
            return
        }

        // Move away from falling snow by just switching actions
        client.findNearestWithin<GameObject>(player.worldLocation, 1, 26690)?.let {
            sendGameMessage("THERE IS FALLING SNOW WATCH OUT")
            if (state == BRAZING) clickRoots()(player)?.let { return }
            if (state == CHOPPING) clickBraziers(BURNING, BROKEN, UNLIT)(player)?.let { return }
        }

        // Update previous HP to find out if we've taken damage.
        // If we have, we'll know  we've been interrupted by the Wintertodt's cold.
        // Whenever we're brazing and we take damage, we've been interrupted and will need to click a brazier.
        // Either we've been damaged by the cold or by the falling ice, in which case we can just click on the burning
        // brazier again, or we've been damaged by the broken brazier and will need to click to repair it.
        client.getWidget(MINIMAP_HEALTH_ORB_TEXT)?.let { it ->
            val hp = it.text.toInt()
            brazingInterruptedByCold = state == BRAZING && hp < currentHP
            currentHP = hp
        }

        val roots = client.getInventory().count { it.id == BRUMA_ROOT }

        if (roots == 0 && player.animation != WOODCUTTING) {
            sendGameMessage("Start chopping shit.")
            clickRoots()(player)?.let { return }
        }

        if ((roots > (8..14).random() || client.inventoryFull()) && player.animation == WOODCUTTING) {
            sendGameMessage("We have enough roots. Going to the brazier.")
            clickBraziers(BURNING, BROKEN, UNLIT)(player)?.let { return }
        }

        if (player.animation == IDLE && brazingInterruptedByCold) {
            sendGameMessage("You've been interrupted by the cold?")
            clickBraziers(BURNING, BROKEN)(player)?.let { return }
        }

        // In this case, we'll have to click on the lit brazier, but the catch all idle case will catch that for us.
        if (player.animation == IDLE && !brazingInterruptedByCold) {
            clickBraziers(UNLIT)(player)?.let { return }
        }

        // Kinda a catch all thing. Sometimes we'll be idle for a while, in which case we should click any brazier
        // within a larger radius. For example, maybe nobody has healed the pyromancer, so we can't light the one we're
        // near.
        when (player.animation) {
            IDLE -> idleFor++
            else -> idleFor = 0
        }
        if (idleFor >= 3) {
            sendGameMessage("We've been idle for a bit. Wondering what to do.")
            if (state == CHOPPING) {
                clickRoots()(player)
            } else {
                clickBraziers(BURNING, BROKEN, UNLIT, searchDistance = 50)(player)
            }
            return
        }
    }

    // Just go inside lol
    private fun actOutside(player: Player) {
        return client.findNearest<GameObject>(DOORS_OF_DINH)?.let { click(it) } ?: Unit
    }
}
