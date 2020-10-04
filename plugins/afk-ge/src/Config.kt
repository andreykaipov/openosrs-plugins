package com.kaipov.plugins

import com.kaipov.plugins.common.bot.BotConfig
import com.kaipov.plugins.common.bot.BotOverlay
import com.kaipov.plugins.craftleather.CraftLeatherConfig
import com.kaipov.plugins.tanleather.TanLeatherConfig
import javax.inject.Inject
import javax.inject.Singleton
import net.runelite.api.Client
import net.runelite.client.config.ConfigGroup
import net.runelite.client.config.ConfigItem
import net.runelite.client.config.ConfigTitleSection
import net.runelite.client.config.Title
import net.runelite.client.ui.overlay.components.table.TableComponent

@Singleton
class Overlay @Inject constructor(k: Client, plugin: AFKGE, c: Config) : BotOverlay(k, plugin, c) {
    override fun addExtraRows(table: TableComponent) {
        val p = plugin as AFKGE
        table.addRow("State:", p.state.toString())
    }
}

@ConfigGroup("com.kaipov.plugins.afkge")
interface Config : TanLeatherConfig, CraftLeatherConfig, BotConfig {
    @JvmDefault
    @ConfigTitleSection(
        keyName = "afkTitle", description = "",
        name = "AFK", position = 1
    )
    fun actionTitle() = Title()

    @JvmDefault
    @ConfigItem(
        keyName = "action", description = "",
        name = "Action", position = 1, titleSection = "afkTitle",
    )
    fun action() = Action.TAN_LEATHER
}


