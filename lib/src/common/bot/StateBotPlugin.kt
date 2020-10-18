package com.kaipov.plugins.common.bot

import com.kaipov.plugins.common.bot.DetailedStates.Stop
import com.kaipov.plugins.common.bot.States.Start
import com.kaipov.plugins.common.bot.States.Unknown
import com.kaipov.plugins.common.bot.extras.waitUntil
import kotlin.concurrent.thread
import kotlin.properties.Delegates
import kotlin.reflect.KClass
import net.runelite.client.ui.overlay.OverlayPanel

abstract class StateBotPlugin<C : BotConfig, O : OverlayPanel>(
    pluginClass: KClass<out StateBotPlugin<C, O>>,
    configClass: KClass<C>,
    everyOtherTick: Int,
) : BotPlugin<C, O>(pluginClass, configClass, everyOtherTick) {

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

    fun waitUntil(max: Long = 4000L, predicate: () -> Boolean) {
        waitUntil(
            max,
            final = {
                state = Stop.with("Timed out waiting for a condition. Last state was $state.")
            },
            predicate
        )
    }
}

interface State

interface DetailedState : State {
    var msg: String

    fun with(msg: String): State {
        this.msg = msg
        return this
    }
}

enum class States : State {
    Unknown,
    Start,
}

enum class DetailedStates(override var msg: String) : DetailedState {
    Stop(""),
}
