package com.kaipov.plugins

import net.runelite.client.config.Config
import net.runelite.client.config.ConfigGroup
import net.runelite.client.config.ConfigItem

@ConfigGroup("TutorialIsland")
interface Config : Config {
    @ConfigItem(
        keyName = "example",
        name = "Example config item",
        description = "Example",
        position = 0
    )
    @JvmDefault
    fun example(): Boolean {
        return true
    }
}
