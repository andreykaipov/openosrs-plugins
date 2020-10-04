package com.kaipov.plugins.common.bot.state

import com.kaipov.plugins.common.bot.BotConfig
import com.kaipov.plugins.common.bot.BotPlugin
import com.kaipov.plugins.common.bot.state.DetailedStates.Stop
import com.kaipov.plugins.common.bot.state.States.Start
import com.kaipov.plugins.common.bot.state.States.Unknown
import kotlin.concurrent.thread
import kotlin.properties.Delegates
import kotlin.reflect.KClass
import net.runelite.client.ui.overlay.OverlayPanel

abstract class StateBotPlugin<C : BotConfig, O : OverlayPanel>(
    pluginClass: KClass<out StateBotPlugin<C, O>>,
    configClass: KClass<C>,
) : BotPlugin<C, O>(pluginClass, configClass, everyOtherTick = 3) {

    private val threads = mutableSetOf<Thread>()

    var state: State by Delegates.observable(Unknown) { _, old, new ->
        if (old == new) return@observable

        log.info("State: $old -> $new")

        if (new == Stop) stop((new as DetailedState).msg)

        action()
    }

    abstract fun action()

    fun state(name: State, action: () -> State) {
        if (state == name) {
            threads.add(thread {
                state = action()
            })
        }
    }

    override fun setup() {
        state = Start
    }

    override fun teardown() {
        threads.forEach { it.interrupt() }
    }
}