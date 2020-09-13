package com.kaipov.plugins

import com.google.inject.Provides
import com.kaipov.plugins.common.WidgetInfo.INTRO_HEAD_LEFT_BUTTON
import com.kaipov.plugins.common.WidgetUtility
import javax.inject.Inject
import net.runelite.api.Client
import net.runelite.api.events.GameTick
import net.runelite.api.widgets.WidgetInfo.FAIRY_RING_LEFT_ORB_CLOCKWISE
import net.runelite.client.config.ConfigManager
import net.runelite.client.eventbus.Subscribe
import net.runelite.client.plugins.Plugin
import net.runelite.client.plugins.PluginDescriptor
import net.runelite.client.plugins.PluginType
import org.pf4j.Extension
import org.slf4j.LoggerFactory

@Extension
@PluginDescriptor(
        name = "Not Tutorial Island",
        tags = ["noob"],
        description = "Does not do tutorial island for you...",
        type = PluginType.MISCELLANEOUS
)
class Plugin : Plugin() {
    private val log = LoggerFactory.getLogger("${this::class.java.packageName}.NotTutorialIsland")

    @Inject
    private lateinit var config: Config

    @Provides
    fun provideConfig(configManager: ConfigManager): Config {
        return configManager.getConfig(Config::class.java)
    }

    @Inject
    private lateinit var client1: Client

    @Inject
    private lateinit var client2: Client

    @Inject
    private lateinit var widgetutil1: WidgetUtility

    @Inject
    private lateinit var widgetutil2: WidgetUtility

    override fun startUp() {
        log.info("Plugin starteddjghfhg")

        if (config.example()) {
            log.info("The value of 'config.example()' is ${config.example()}")
        }
    }

    override fun shutDown() {
        log.info("Plugin stopped")
    }

    @Subscribe
    fun onGameTick(gameTick: GameTick) {
        val a = widgetutil1.get(INTRO_HEAD_LEFT_BUTTON)
        val b = widgetutil1.get(FAIRY_RING_LEFT_ORB_CLOCKWISE)
        println(a)
        println(b)

        println(client1.getWidget(269, 106))

        println(System.identityHashCode(client1))
        println(System.identityHashCode(client2))

//        val button = Widget.INTRO_HEAD_LEFT_BUTTON.get()
//        val bounds = button?.bounds
//        println(bounds)
    }

}