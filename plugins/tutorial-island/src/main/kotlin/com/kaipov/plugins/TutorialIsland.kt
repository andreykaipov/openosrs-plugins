package com.kaipov.plugins

import com.google.inject.Provides
import com.kaipov.plugins.INTRO_BUTTON.ACCEPT
import com.kaipov.plugins.INTRO_BUTTON.WELCOME_TO_RUNESCAPE
import com.kaipov.plugins.extensions.client.WidgetInfo
import com.kaipov.plugins.extensions.client.doubleClick
import com.kaipov.plugins.extensions.client.getWidget
import com.kaipov.plugins.extensions.client.singleClick
import java.awt.Rectangle
import javax.inject.Inject
import net.runelite.api.Client
import net.runelite.api.GameState
import net.runelite.api.events.Event
import net.runelite.api.events.GameTick
import net.runelite.api.events.UsernameChanged
import net.runelite.client.config.ConfigManager
import net.runelite.client.eventbus.EventBus
import net.runelite.client.eventbus.Subscribe
import net.runelite.client.plugins.Plugin
import net.runelite.client.plugins.PluginDescriptor
import net.runelite.client.plugins.PluginType
import org.pf4j.Extension
import org.slf4j.LoggerFactory


@Extension
@PluginDescriptor(name = "Tutorial Island",
    description = "Does Tutorial Island for you",
    type = PluginType.UTILITY
)
class TutorialIsland : Plugin() {
    private val log = LoggerFactory.getLogger(this::class.java)

    @Inject
    private lateinit var client: Client

    @Inject
    private lateinit var eventBus: EventBus

    @Inject
    private lateinit var configManager: ConfigManager

    // Injects our config
    @Inject
    private lateinit var config: Config

    // Provides our config
    @Provides
    fun provideConfig(configManager: ConfigManager): Config {
        return configManager.getConfig(Config::class.java)
    }

    private val stateTicks = enumValues<State>().associateWithTo(mutableMapOf()) { 0 }


//    private fun fiftyTicks(t: GameTick) {
//        tick++
//        println("hello from bus")
//        //do stuff
//        if (tick == 50) {
//            eventBus.unregister(TICK)
//            tick = 0
//        }
//    }


    private fun <T : Event> EventBus.waitUntil(threshold: Int, eventClass: Class<T>, f: () -> Unit) {
        var i = 0
        val lifecycle = Any()
        subscribe(eventClass, lifecycle) {
            if (++i >= threshold) {
                unregister(lifecycle)
                f()
            }
        }
    }

    private fun <T : Event> EventBus.doUntil(threshold: Int, eventClass: Class<T>, f: () -> Unit) {
        var i = 0
        val lifecycle = Any()
        subscribe(eventClass, lifecycle) {
            f()
            if (++i >= threshold) {
                unregister(lifecycle)
            }
        }
    }

    private val lifecycles = listOf<Event>(
        GameTick.INSTANCE,
        UsernameChanged.INSTANCE,
    )

    override fun startUp() {
        log.info("Plugin started")
//        configManager.setConfiguration("KotlinExampleConfig", "example", true)
//
//
//        if (config.example()) {
//            log.info("The value of 'config.example()' is ${config.example()}")
//        }
    }

    override fun shutDown() {

    }

    private var tick = 0
    private val randomizingPlayerTickThreshold = (30..50).random()

    @Subscribe
    private fun onGameTick(@Suppress("UNUSED_PARAMETER") event: GameTick) {
        tick += 1
        if (tick % (2..3).random() == 0) {
            return
        }

        if (client.localPlayer == null || client.gameState != GameState.LOGGED_IN) {
            return
        }

        if (client.localPlayer!!.worldLocation.regionID != 12336) {
            return
        }

        when (true) {
            client.getWidget(WELCOME_TO_RUNESCAPE) != null -> {
                when {
                    tick < randomizingPlayerTickThreshold -> randomizePlayer()
                    else -> client.getWidget(ACCEPT)?.bounds?.let { client.singleClick(it) }
                }
            }
            else ->
                println("I don't know what to do now")
        }
    }

    private fun randomizePlayer() {
        println("randomizing player")

        val designOption = enumValues<INTRO_BUTTON.DESIGN>().random()
        val colourOption = enumValues<INTRO_BUTTON.COLOUR>().random()

        // 3:2 ratio of single to double clicks
        val clicks = listOf(
            listOf(1..3).map { _ -> { x: Rectangle -> client.singleClick(x) } },
            listOf(4..5).map { _ -> { x: Rectangle -> client.doubleClick(x) } },
        ).flatten()

        client.getWidget(designOption)?.bounds?.let { clicks.random()(it) }
        client.getWidget(colourOption)?.bounds?.let { clicks.random()(it) }
    }
//
//    private val state: () -> Unit
//        get() = when (true) {
//            ->
//                this::randomizePlayer
//            else ->
//                ()
//            -> {
//                {
//                    println("I don't know what to do now")
//                }
//            }
//        }

}

//fun <T : Event?> EventBus.observe(eventClass: Class<T>): Observable<T> {
//    return getSubject(eventClass).filter { obj: Any? -> Objects.nonNull(obj) }.cast(eventClass)
//}

//private val subscriptions = mutableSetOf<String>()
//
//fun <T : Event> EventBus.addSubscription(eventClass: Class<T>, ID: String) {
//    subscriptions.add(eventClass.toString() + ID)
//}
//
//fun <T : Event> EventBus.subscribe(eventClass: Class<T>, action: (T) -> Unit) {
//    subscribe(eventClass, Any(), action)
//}
//
//fun <T : Event> EventBus.subscribeIdempotently(eventClass: Class<T>, ID: String, take: Int, action: (T) -> Unit) {
//    if (!subscriptions.contains(ID)) {
//        println("subscribing")
//        subscribe(eventClass, Any(), action, take)
//    } else {
//        println("already subscriebd")
//    }
//}

enum class State {
    UNKNOWN,
    RANDOMIZING_PLAYER,
    PART_ONE,
}

enum class INTRO_BUTTON(override val groupId: Int, override val childId: Int) : WidgetInfo {
    WELCOME_TO_RUNESCAPE(269, 97),
    ACCEPT(269, 100);

    enum class DESIGN(override val groupId: Int, override val childId: Int) : WidgetInfo {
        HEAD_DESIGN_LEFT(269, 106),
        JAW_DESIGN_LEFT(269, 107),
        TORSO_DESIGN_LEFT(269, 108),
        ARMS_DESIGN_LEFT(269, 109),
        HANDS_DESIGN_LEFT(269, 110),
        LEGS_DESIGN_LEFT(269, 111),
        FEET_DESIGN_LEFT(269, 112),
        HEAD_DESIGN_RIGHT(269, 113),
        JAW_DESIGN_RIGHT(269, 114),
        TORSO_DESIGN_RIGHT(269, 115),
        ARMS_DESIGN_RIGHT(269, 116),
        HANDS_DESIGN_RIGHT(269, 117),
        LEGS_DESIGN_RIGHT(269, 118),
        FEET_DESIGN_RIGHT(269, 119),
    }

    enum class COLOUR(override val groupId: Int, override val childId: Int) : WidgetInfo {
        HAIR_COLOUR_LEFT(269, 105),
        TORSO_COLOUR_LEFT(269, 123),
        LEGS_COLOUR_LEFT(269, 122),
        FEET_COLOUR_LEFT(269, 124),
        SKIN_COLOUR_LEFT(269, 125),
        HAIR_COLOUR_RIGHT(269, 121),
        TORSO_COLOUR_RIGHT(269, 127),
        LEGS_COLOUR_RIGHT(269, 129),
        FEET_COLOUR_RIGHT(269, 130),
        SKIN_COLOUR_RIGHT(269, 131),
    }

    enum class GENDER(val groupId: Int, val childId: Int) {
        MALE(269, 136),
        FEMALE(269, 137),
    }
}
