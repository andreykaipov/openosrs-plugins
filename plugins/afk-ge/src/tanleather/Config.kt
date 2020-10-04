package com.kaipov.plugins.tanleather

import net.runelite.api.ItemID
import net.runelite.client.config.ConfigItem

enum class TanLeatherInputs(val id: Int, val outputID: Int) {
    BLUE_DRAGONHIDE(ItemID.BLUE_DRAGONHIDE, ItemID.BLUE_DRAGON_LEATHER),
    RED_DRAGONHIDE(ItemID.RED_DRAGONHIDE, ItemID.RED_DRAGON_LEATHER),
}

interface TanLeatherConfig {
    @JvmDefault
    @ConfigItem(
        keyName = "tanLeatherInput", description = "",
        name = "Input", position = 2, titleSection = "afkTitle",
        hidden = true, unhide = "action", unhideValue = "TAN_LEATHER",
    )
    fun tanLeatherInput() = TanLeatherInputs.BLUE_DRAGONHIDE
}
