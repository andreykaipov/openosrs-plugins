package com.kaipov.plugins.common.bot

import net.runelite.client.config.Config
import net.runelite.client.config.ConfigItem
import net.runelite.client.config.Keybind


interface BotConfig : Config {
    @ConfigItem(
        keyName = "toggleHotkey",
        name = "Toggle Hotkey",
        description = "Hotkey to toggle the bot on or off",
        position = 1,
    )
    @JvmDefault
    fun toggleKey(): Keybind = Keybind.NOT_SET

    @ConfigItem(
        keyName = "logMenuOptionClickedEvents",
        name = "Log MenuOptionClicked events",
        description = "Primarily for debugging during development",
        position = 2,
    )
    @JvmDefault
    fun logParams(): Boolean = false
}
