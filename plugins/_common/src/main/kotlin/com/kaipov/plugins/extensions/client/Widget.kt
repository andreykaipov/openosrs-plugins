package com.kaipov.plugins.extensions.client

import net.runelite.api.Client
import net.runelite.api.widgets.Widget
import net.runelite.api.widgets.WidgetID

interface WidgetInfo {
    val groupId: Int
    val childId: Int
}

/**
 * The following allows us to define our own WidgetInfo enums and use the
 * client#getWidget(WidgetInfo) function instead of getWidget(int, int) for a
 * more consistent interface.
 */
fun Client.getWidget(wi: WidgetInfo): Widget? {
    return getWidget(wi.groupId, wi.childId)
}

enum class ExtraWidgets(override val groupId: Int, override val childId: Int) : WidgetInfo {

    MINIMAP_HEALTH_ORB_TEXT(WidgetID.MINIMAP_GROUP_ID, 5),

    BANK_QUANTITY_ONE(WidgetID.BANK_GROUP_ID, 27),
    BANK_QUANTITY_FIVE(WidgetID.BANK_GROUP_ID, 29),
    BANK_QUANTITY_TEN(WidgetID.BANK_GROUP_ID, 31),
    BANK_QUANTITY_X(WidgetID.BANK_GROUP_ID, 33),
    BANK_QUANTITY_ALL(WidgetID.BANK_GROUP_ID, 35),

    NPC_CONTACT_DARK_MAGE(75, 12),

    ;

    val id get() = groupId shl 16 or childId
}