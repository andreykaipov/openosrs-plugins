package com.kaipov.common.bot

import net.runelite.client.config.*

interface BotConfig : Config {
    @JvmDefault
    @ConfigTitleSection(
        keyName = "commonTitle",
        name = "Common",
        description = "",
        position = 0
    )
    fun commonTitle() = Title()

    @ConfigItem(
        keyName = "toggleHotkey",
        name = "Toggle Hotkey",
        description = "Hotkey to toggle the bot on or off",
        position = 1,
        titleSection = "commonTitle"
    )
    @JvmDefault
    fun toggleKey(): Keybind = Keybind.NOT_SET

    @ConfigItem(
        keyName = "logMenuOptionClickedEvents",
        name = "Log MenuOptionClicked events",
        description = "Primarily for debugging during development",
        position = 2,
        titleSection = "commonTitle"
    )
    @JvmDefault
    fun logParams(): Boolean = false
}
