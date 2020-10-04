package com.kaipov.plugins.craftleather

import net.runelite.api.ItemID
import net.runelite.client.config.ConfigItem

enum class CraftLeatherInputs(val id: Int) {
    BLUE_DRAGON_LEATHER(ItemID.BLUE_DRAGON_LEATHER),
    RED_DRAGON_LEATHER(ItemID.RED_DRAGON_LEATHER),
}

enum class CraftLeatherOutputs(val id: Int) {
    BLUE_DHIDE_BODY(ItemID.BLUE_DHIDE_BODY),
    RED_DHIDE_BODY(ItemID.RED_DHIDE_BODY),
}

interface CraftLeatherConfig {
    @JvmDefault
    @ConfigItem(
        keyName = "craftLeatherInput", description = "",
        name = "Input", position = 2, titleSection = "afkTitle",
        hidden = true, unhide = "action", unhideValue = "CRAFT_LEATHER",
        enumClass = CraftLeatherInputs::class
    )
    fun craftLeatherInput() = CraftLeatherInputs.BLUE_DRAGON_LEATHER

    @JvmDefault
    @ConfigItem(
        keyName = "craftLeatherOutput",
        description = "The most recent action that is under spacebar",
        name = "Output", position = 3, titleSection = "afkTitle",
        hidden = true, unhide = "action", unhideValue = "CRAFT_LEATHER",
    )
    fun craftLeatherOutput() = CraftLeatherOutputs.BLUE_DHIDE_BODY
}
