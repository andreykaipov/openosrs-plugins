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
import com.kaipov.plugins.extensions.menuoption.MenuOption.Companion.WITHDRAW
import com.kaipov.plugins.extensions.menuoption.overwriteWith
import java.time.Duration
import java.time.Instant
import java.util.function.Supplier
import javax.inject.Inject
import kotlin.reflect.KClass
import net.runelite.api.*
import net.runelite.api.AnimationID.IDLE
import net.runelite.api.coords.LocalPoint
import net.runelite.api.events.GameTick
import net.runelite.api.events.MenuOptionClicked
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
    }

    // Order doesn't really matter but just inverse stop
    fun stop() {
        teardown()
        stopSubscribers()
        overlayManager.remove(overlay)
        startTime = null
        running = false
        log.info("Stopped")
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

    fun bankDeposit(id: Int, q: Quantity = Quantity.ONE) = client.findFirstInInventory(id).takeIf { it >= 0 }?.let { click(DEPOSIT(it, q)) }
    fun bankWithdraw(id: Int, q: Quantity = Quantity.ONE) = client.findFirstInBank(id).takeIf { it >= 0 }?.let { click(WITHDRAW(it, q)) }

    fun bankWear(id: Int) = clickNinthBankDepositOptionOf(id)
    fun bankFill(id: Int) = clickNinthBankDepositOptionOf(id)
    fun bankEmpty(id: Int) = clickNinthBankDepositOptionOf(id)

    fun drink(id: Int) = clickInventoryFirstOptionOf(id)
    fun eat(id: Int) = clickInventoryFirstOptionOf(id)
    fun fill(id: Int) = clickInventoryFirstOptionOf(id)
    fun drop(id: Int) = client.findFirstInInventory(id).takeIf { it >= 0 }?.let { click(DROP(it, id)) }
    fun empty(id: Int) = clickInventorySecondOptionOf(id)

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

    fun waitUntil(max: Long = 4000L, predicate: () -> Boolean) {
        var timeSlept: Long = 0
        while (!predicate()) {
            val t = (500..1000L).random()
            Thread.sleep(t)
            timeSlept += t

            if (timeSlept > max) break
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
