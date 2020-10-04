package com.kaipov.plugins.common.bot

import com.google.inject.Provides
import com.kaipov.plugins.extensions.client.*
import com.kaipov.plugins.extensions.menuoption.MenuOption
import com.kaipov.plugins.extensions.menuoption.MenuOption.Companion.BANK_DEPOSIT_NINTH_OPTION
import com.kaipov.plugins.extensions.menuoption.MenuOption.Companion.DEPOSIT
import com.kaipov.plugins.extensions.menuoption.MenuOption.Companion.DROP
import com.kaipov.plugins.extensions.menuoption.MenuOption.Companion.INVENTORY_FIRST_OPTION
import com.kaipov.plugins.extensions.menuoption.MenuOption.Companion.INVENTORY_SECOND_OPTION
import com.kaipov.plugins.extensions.menuoption.MenuOption.Companion.Quantity
import com.kaipov.plugins.extensions.menuoption.MenuOption.Companion.USE
import com.kaipov.plugins.extensions.menuoption.MenuOption.Companion.USE_ON_ITEM
import com.kaipov.plugins.extensions.menuoption.MenuOption.Companion.WITHDRAW
import com.kaipov.plugins.extensions.menuoption.overwriteWith
import java.awt.event.KeyEvent.VK_ENTER
import java.time.Duration
import java.time.Instant
import java.util.function.Supplier
import javax.inject.Inject
import kotlin.math.min
import kotlin.reflect.KClass
import net.runelite.api.*
import net.runelite.api.AnimationID.IDLE
import net.runelite.api.coords.LocalPoint
import net.runelite.api.events.GameTick
import net.runelite.api.events.MenuOptionClicked
import net.runelite.api.widgets.WidgetInfo
import net.runelite.client.callback.ClientThread
import net.runelite.client.chat.ChatColorType
import net.runelite.client.chat.ChatMessageBuilder
import net.runelite.client.chat.ChatMessageManager
import net.runelite.client.chat.QueuedMessage
import net.runelite.client.config.ConfigManager
import net.runelite.client.eventbus.EventBus
import net.runelite.client.input.KeyManager
import net.runelite.client.menus.MenuManager
import net.runelite.client.plugins.Plugin
import net.runelite.client.ui.overlay.OverlayManager
import net.runelite.client.ui.overlay.OverlayPanel
import net.runelite.client.util.HotkeyListener
import net.runelite.rs.api.RSClient
import org.slf4j.LoggerFactory

open class BotPlugin<C : BotConfig, O : OverlayPanel>(
    private val pluginClass: KClass<out BotPlugin<C, O>>,
    private val configClass: KClass<C>,
    private val everyOtherTick: Int = 3,
) : Plugin() {
    val log = LoggerFactory.getLogger(pluginClass.java)!!

    @Inject
    lateinit var client: Client

    @Inject
    lateinit var clientThread: ClientThread

    @Inject
    lateinit var overlayManager: OverlayManager

    @Inject
    lateinit var keyManager: KeyManager

    @Inject
    lateinit var chatMessageManager: ChatMessageManager

    @Inject
    lateinit var menuManager: MenuManager

    @Inject
    lateinit var events: EventBus

    @Inject
    open lateinit var overlay: O

    @Inject
    open lateinit var config: C

    @Provides
    fun config(man: ConfigManager): C = man.getConfig(configClass.java)

    /** ----------- subscribers ----------- **/

    // @Subscribe doesn't work on open classes, so subscribe directly to the
    // eventBus within the startUp and teardown hooks.
    private val lifecycleMenuOptionClicked = Any()
    private val lifecycleGameTick = Any()

    var previousPlayerLocation = LocalPoint(0, 0)

    fun Player?.canAct(): Boolean {
        return (1..everyOtherTick).random() == 1 && // only act every other game tick (on average)
            this != null &&                         // player exists and is logged in
            localLocation == previousPlayerLocation // player is not moving ((but not necessarily idle)
    }

    fun Player.isIdle() = animation == IDLE

    // but not necessarily idle (walking/running is still an IDLE animation)
    fun Player.isMoving(): Boolean {
        return localLocation != previousPlayerLocation // player is not moving ((but not necessarily idle)
    }

    open fun act(player: Player) {}

    private fun startSubscribers() {
        events.subscribe<MenuOptionClicked>(lifecycleMenuOptionClicked) {
            if (config.logParams()) {
                println("option=${it.option}; target=${it.target}; id=${it.identifier}; opcode=${it.opcode}; params=(${it.param0}, ${it.param1})")
            }
        }

        events.subscribe<GameTick>(lifecycleGameTick) {
            if ((1..everyOtherTick).random() != 1) return@subscribe

            val player = client.localPlayer
            if (player != null) {
                act(player)
            }

            previousPlayerLocation = player?.localLocation ?: LocalPoint(0, 0)
        }
    }

    private fun stopSubscribers() {
        events.unregister(lifecycleMenuOptionClicked)
        events.unregister(lifecycleGameTick)
    }

    /** ----------- start/stop junk ----------- **/

    // To keep track how long we've been running our plugin for
    private var startTime: Instant? = null
    val timeRunning: Duration get() = if (startTime == null) Duration.ZERO else Duration.between(startTime, Instant.now())

    // If implemented plugins ever want to start or stop themselves during the game tick loop (e.g. breaks or errors)
    fun start() {
        log.info("Started")
        running = true
        startTime = Instant.now()
        overlayManager.add(overlay)
        startSubscribers()
        setup()

        clientThread.invoke(Runnable {
            VARBIT_WITHDRAW_X_AMOUNT = client.getVarbitDefinition(Varbits.WITHDRAW_X_AMOUNT.id)
                ?: return@Runnable stop("ca")
        })
    }

    // Order shouldn't really matter but just the inverse of start above for consistency
    fun stop(msg: String = "") {
        teardown()
        stopSubscribers()
        overlayManager.remove(overlay)
        startTime = null
        running = false
        log.info("Stopped")

        if (msg != "") {
            sendGameMessage(msg)
        }
    }

    // Custom setup/teardown hooks for plugins that have specific start/stop logic.
    // Note `setup` runs after the common setup logic, and `teardown` runs before the common setup logic.
    open fun setup() {}
    open fun teardown() {}

    var running = false

    // Each plugin config must have a config item for a hotkey toggle
    private var hotkeyListener: HotkeyListener = object : HotkeyListener(Supplier { config.toggleKey() }) {
        public override fun hotkeyPressed() {
            running = !running
            if (running) start()
            else stop()
        }
    }

//    fun VarbitDefinition.mask() =
//        (1 shl mostSignificantBit - leastSignificantBit + 1) - 1
//    }

    override fun startUp() {
        keyManager.registerKeyListener(hotkeyListener)
    }


    override fun shutDown() {
        if (running) stop()
        keyManager.unregisterKeyListener(hotkeyListener)
    }

    /** ----------- click utility functions ----------- **/

    /**
     * The way we click on something is by clicking on anything (e.g. in the
     * center of our screen), intercepting the `MenuOptionClicked` event
     * spawned from that click, and then overwriting it with the necessary ID,
     * opcode, and params of the event we actually want to do. The now modified
     * event is then passed and handled by the vanilla client code.
     *
     * For example, if we want to click on a banker:
     *     1. Click anywhere
     *     2. Capture the the MenuOptionClicked event
     *     3. Overwrite it with an event that is supposed to click on a banker
     *
     * Pretty sneaky.
     *
     * Based on the object we want to click on or the action we want to do, the
     * ID, opcode, and params may vary. A few things we know how to click on
     * are GameObjects, NPCs, and even our own extra menu options that we've
     * defined.
     *
     * As Kotlin does not have union types, we allow clicking on an "Any"
     * object, and will throw an exception if it's something we don't know how
     * to click on. We don't have to do this and can just define separate click
     * methods for each type of clickable thing, but that's a lot of duplicate
     * code. So, we compromise and make this particular click method private,
     * defining separate click methods from it. Before adding new public click
     * functions, make sure it's handled here!
     *
     * Note there's a possibility the click observed might not be the click we
     * send. For example, our click function has a random delay built in, so
     * the described situation might happen if we call this method twice in
     * succession. However, it doesn't really matter! So long as we overwrite
     * some click event, we're ok. Of course, that still leaves the possibility
     * of overwriting actual user clicks, but I've found that to be quite rare
     * (i.e. if you click during the ~100ms window when we've issued a random
     * click but the event subscription hasn't captured it yet). In any case,
     * why are we clicking during script execution? Just stop the plugin first.
     */
    private fun uncheckedClick(o: Any) {
        val overwrite: (MenuOptionClicked.() -> Unit) = when (o) {
            is GameObject ->
                { -> overwriteWith(o.id, MenuOpcode.GAME_OBJECT_FIRST_OPTION, o.sceneMinLocation) }
            is MenuOption ->
                { -> overwriteWith(o.id, o.opcode, o.param0, o.param1) }
            is NPC ->
                { -> overwriteWith(o.index, MenuOpcode.NPC_SECOND_OPTION, 0, 0) }
            else ->
                throw IllegalArgumentException("I don't know how to overwrite a MenuOptionClicked with a '${o::class.java}'")
        }
        events.once<MenuOptionClicked> { overwrite() }
        client.singleClickCenterScreenRandom()
    }

    // Checked clicks
    fun click(o: MenuOption) = uncheckedClick(o)
    fun click(o: GameObject) = uncheckedClick(o)
    fun click(o: NPC) = uncheckedClick(o)

    /**
     * Convenient banking methods for withdrawals and deposits. If the items
     * aren't found in the bank or player's inventory, a click won't be sent.
     *
     * Note these functions return a Unit?. We can use this determine if a
     * click actually went through or not (e.g. we wouldn't want to send clicks
     * if the item wasn't found in the inventory. Might be handy.
     *
     * All of these functions take an itemID:
     * @see ItemID
     */

    private fun clickNinthBankDepositOptionOf(id: Int) = client.findFirstInInventory(id).takeIf { it >= 0 }?.let { click(BANK_DEPOSIT_NINTH_OPTION(it)) }
    private fun clickInventoryFirstOptionOf(id: Int) = client.findFirstInInventory(id).takeIf { it >= 0 }?.let { click(INVENTORY_FIRST_OPTION(it, id)) }
    private fun clickInventorySecondOptionOf(id: Int) = client.findFirstInInventory(id).takeIf { it >= 0 }?.let { click(INVENTORY_SECOND_OPTION(it, id)) }


    private lateinit var VARBIT_WITHDRAW_X_AMOUNT: VarbitDefinition
    val presetBankAmount get() = VARBIT_WITHDRAW_X_AMOUNT.value()

    /**
     * @see net.runelite.mixins.VarbitMixin
     * #getVarbitValue(int[] varps, int varbitId)
     */
    fun VarbitDefinition.mask() = (1 shl ((mostSignificantBit - leastSignificantBit) + 1)) - 1
    fun VarbitDefinition.value() = (client.varps[index] shr leastSignificantBit) and mask()

    /**
     * Must be called after we click(WITHDRAW) or click(DEPOSIT)
     */
    fun Quantity.fixPresetHandleX(): Unit? {
        return when (this) {
            Quantity.PRESET -> value = presetBankAmount
            Quantity.X -> {
                if (value == -1) throw IllegalArgumentException("Use Quantity.X.at(n) to set an amount")
                wait(500..1500)
                client.pressKeys(value.toString())
                client.pressKey(VK_ENTER)
            }
            else -> Unit
        }
    }

    fun bankDeposit(id: Int, q: Quantity = Quantity.ONE): Unit? {
        assert(!client.isClientThread)

        val currentCount = client.getInventory().count { it.id == id }

        client.findFirstInInventory(id).takeIf { it >= 0 }?.let { click(DEPOSIT(it, q)) } ?: return null
        q.fixPresetHandleX() ?: return null

        val countExpected = currentCount - min(q.value, currentCount)
        return waitUntil(4000L, { println("timed out") }) { client.hasExactlyInInventory(id, countExpected) }
    }

    /**
     * Get the current count of the item in our inventory, takes it out of the
     * bank if it exists, waiting until it pops up in our inventory. It's like
     * a few hundred milliseconds. Only for non-stackable items.
     *
     * Added complexity to handle X and PRESET Quantities.
     *
     * Notes:
     * - Returns null if the item is not found within our bank.
     * - Assumes the bank quantity is set to 1.
     * - For quantities of X and PRESET, we read varbits or enter input.
     */
    fun bankWithdraw(id: Int, q: Quantity = Quantity.ONE): Unit? {
        assert(!client.isClientThread)

        val currentCount = client.getInventory().count { it.id == id }
        val freeInventorySlots = client.getInventory().count { it.id == -1 }

        client.findFirstInBank(id).takeIf { it >= 0 }?.let { click(WITHDRAW(it, q)) } ?: return null
        q.fixPresetHandleX() ?: return null

        val countExpected = currentCount + min(q.value, freeInventorySlots)
        return waitUntil(4000L, { println("timed out") }) { client.hasAtLeastInInventory(id, countExpected) }
    }

    /**
     * Wrapper method around invoke that will wait until whatever we wanted to
     * run in the client thread actually finishes.
     */
    fun ClientThread.run(timeout: Long = 4000L, f: () -> Unit) {
        var done = false
        invoke(Runnable {
            f()
            done = true
        })
        waitUntil(timeout) { done }
    }

    fun bankWear(id: Int) = clickNinthBankDepositOptionOf(id)
    fun bankFill(id: Int) = clickNinthBankDepositOptionOf(id)
    fun bankEmpty(id: Int) = clickNinthBankDepositOptionOf(id)

    fun drink(id: Int) = clickInventoryFirstOptionOf(id)
    fun eat(id: Int) = clickInventoryFirstOptionOf(id)
    fun fill(id: Int) = clickInventoryFirstOptionOf(id)
    fun drop(id: Int) = client.findFirstInInventory(id).takeIf { it >= 0 }?.let { click(DROP(it, id)) }
    fun empty(id: Int) = clickInventorySecondOptionOf(id)
    fun use(id: Int) = client.findFirstInInventory(id).takeIf { it >= 0 }?.let { click(USE(it, id)) }
    fun useOnItem(id: Int) = client.findFirstInInventory(id).takeIf { it >= 0 }?.let { click(USE_ON_ITEM(it, id)) }

    // Uses a on b. Does this in one click rather than two. Peep Ganom's one click.
    // https://github.com/Ganom/ExternalPlugins/blob/master/OneClick/src/main/java/net/runelite/client/plugins/externals/oneclick/OneClickPlugin.java#L422-L474
    fun useItemOnItem(a: Int, b: Int): Unit? {
        val aIndex = client.findFirstInInventory(a).takeIf { it >= 0 } ?: return null
        val bIndex = client.findFirstInInventory(b).takeIf { it >= 0 } ?: return null
        events.once<MenuOptionClicked> {
            client.selectedItemWidget = WidgetInfo.INVENTORY.id
            client.selectedItemSlot = bIndex
            client.setSelectedItemID(b)
            overwriteWith(USE_ON_ITEM(aIndex, a))
        }
        return client.singleClickCenterScreenRandom()
    }

    fun walkTo(p: LocalPoint) {
        events.subscribe<MenuOptionClicked>(1) {
            it.consume()
            (client as RSClient).selectedSceneTileX = p.sceneX
            (client as RSClient).selectedSceneTileY = p.sceneY
            (client as RSClient).setViewportWalking(true)
            (client as RSClient).isCheckClick = false
        }
        client.singleClickCenterScreenRandom()
    }

    /** ----------- miscellaneous utility functions ----------- **/


    fun sendGameMessage(message: String?) {
        chatMessageManager.queue(
            QueuedMessage
                .builder()
                .type(ChatMessageType.CONSOLE)
                .runeLiteFormattedMessage(ChatMessageBuilder().append(ChatColorType.HIGHLIGHT).append(message).build())
                .build()
        )
    }

    val wait = { r: IntRange -> Thread.sleep((r.first..r.last.toLong()).random()) }

    /**
     * Waits for max amount of milliseconds until the predicate is true.
     * If it times out, runs the final function.
     */
    fun waitUntil(max: Long = 4000L, final: () -> Unit = {}, predicate: () -> Boolean) {
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
    fun waitUntilPlayerHasBeenIdleForMoreThan(window: IntRange, timeout: Long) {
        waitUntil(timeout) {
            if (client.localPlayer?.animation == IDLE) {
                wait(window)
                return@waitUntil client.localPlayer?.animation == IDLE
            }
            return@waitUntil false
        }
    }

/*
else if (isInPestControl) {
    if (isInNewGame) {
        log.info("new game")
    }
    var target: NPC? = utils.findNearestNpcWithin(player.worldLocation, 25, "Brawler", "Defiler", "Ravager", "Shifter", "Torcher")
    // Simulate a loop because sleeps doesn't work without freezing the fkn client
    if (count < 7 && target == null && !isInNewGame) {
        log.info("Coudn't find a target, trying again: $count")
        target = utils.findNearestNpcWithin(player.worldLocation, 25, "Brawler", "Defiler", "Ravager", "Shifter", "Torcher")
        count++
        return
    }
    count = 0
    // Priority check for NPCs
    val nearbyBrawler: NPC? = utils.findNearestNpcWithin(player.worldLocation, 5, "Brawler")
    if (nearbyBrawler != null) {
        target = nearbyBrawler
    }
    //Colored portals & spinners
    val priorityTarget: NPC? = utils.findNearestNpcWithin(
        player.worldLocation, 25,
        NpcID.PORTAL, NpcID.PORTAL_1740, NpcID.PORTAL_1741, NpcID.PORTAL_1742,
        NpcID.SPINNER, NpcID.SPINNER_1710, NpcID.SPINNER_1711, NpcID.SPINNER_1712, NpcID.SPINNER_1713
    )
    if (priorityTarget != null) {
        target = priorityTarget
    }
    if (target != null) { // Found a target
        if (player.interacting != null) { // If player is interacting
            val currentNPC = player.interacting
            if (currentNPC != null && currentNPC.healthRatio == -1) {
                log.info("Interacting and NPC has no health bar, finding new NPC")
                targetMenu = MenuEntry("", target.name.toString() + "(" + target.id + ")", target.index, MenuOpcode.NPC_SECOND_OPTION.id, 0, 0, false)
                utils.delayClickRandomPointCenter(-100, 100, utils.getRandomIntBetweenRange(100, 200))
            }
        } else {
            log.info("Attacking new target")
            targetMenu = MenuEntry("", target.name.toString() + "(" + target.id + ")", target.index, MenuOpcode.NPC_SECOND_OPTION.id, 0, 0, false)
            utils.delayClickRandomPointCenter(-100, 100, utils.getRandomIntBetweenRange(100, 200))
        }
    } else { // No targets found, open gate
        val gate: WallObject? = utils.findNearestWallObject(14235, 14233)
        if (gate != null) {
            log.info("No NPCs found, opening nearest gate")
            targetMenu = MenuEntry("", "gate", gate.id, MenuOpcode.GAME_OBJECT_FIRST_OPTION.id, gate.localLocation.sceneX, gate.localLocation.sceneY, false)
            utils.delayClickRandomPointCenter(-100, 100, utils.getRandomIntBetweenRange(100, 200))
        }
    }
}

    */
}


//14233 left
//14235 right
