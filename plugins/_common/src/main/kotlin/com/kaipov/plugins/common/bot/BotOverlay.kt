package com.kaipov.plugins.common.bot

import java.awt.Dimension
import java.awt.Graphics2D
import java.awt.Rectangle
import net.runelite.api.Client
import net.runelite.api.MenuOpcode
import net.runelite.client.ui.overlay.OverlayManager
import net.runelite.client.ui.overlay.OverlayMenuEntry
import net.runelite.client.ui.overlay.OverlayPanel
import net.runelite.client.ui.overlay.OverlayPosition
import net.runelite.client.ui.overlay.components.TitleComponent
import net.runelite.client.ui.overlay.components.table.TableAlignment
import net.runelite.client.ui.overlay.components.table.TableComponent
import net.runelite.client.util.ColorUtil
import org.apache.commons.lang3.time.DurationFormatUtils

open class BotOverlay constructor(
    val client: Client,
    val plugin: BotPlugin<out BotConfig, out OverlayPanel>,
    val config: BotConfig,
) : OverlayPanel(plugin) {
    init {
        position = OverlayPosition.BOTTOM_LEFT
        menuEntries.add(OverlayMenuEntry(MenuOpcode.RUNELITE_OVERLAY_CONFIG, OverlayManager.OPTION_CONFIGURE, "${plugin.name} Overlay"))
    }

    override fun render(graphics: Graphics2D): Dimension {
        val table = TableComponent()
        table.setColumnAlignments(TableAlignment.LEFT, TableAlignment.RIGHT)
        
        table.addRow("Elapsed time:", DurationFormatUtils.formatDuration(plugin.timeRunning.toMillis(), "HH:mm:ss"))
//        table.addRow("State:", plugin.state.toString())
        addExtraRows(table)

        if (!table.isEmpty) {
            panelComponent.backgroundColor = ColorUtil.fromHex("#1a1a1a")
            panelComponent.preferredSize = Dimension(150, 200)
            panelComponent.setBorder(Rectangle(5, 5, 5, 5))
            panelComponent.children.add(TitleComponent.builder().text(plugin.name).color(ColorUtil.fromHex("#00ee00")).build())
            panelComponent.children.add(table)
        }

        return super.render(graphics)
    }

    open fun addExtraRows(table: TableComponent) {}
}
