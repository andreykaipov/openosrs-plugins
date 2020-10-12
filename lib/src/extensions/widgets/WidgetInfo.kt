package com.kaipov.plugins.extensions.widgets

import net.runelite.api.Client
import net.runelite.api.widgets.Widget
import net.runelite.api.widgets.WidgetID

/**
 * Same idea as @{link net.runelite.api.widgets.WidgetInfo}.
 * Not every widget is covered by that. We add in custom ones we find.
 */
enum class WidgetInfo(val groupId: Int, val childId: Int) {

    MINIMAP_HEALTH_ORB_TEXT(WidgetID.MINIMAP_GROUP_ID, 5),

    BANK_CONTAINER_BORDERS(WidgetID.BANK_GROUP_ID, 2),
    BANK_QUANTITY_ONE(WidgetID.BANK_GROUP_ID, 27),
    BANK_QUANTITY_FIVE(WidgetID.BANK_GROUP_ID, 29),
    BANK_QUANTITY_TEN(WidgetID.BANK_GROUP_ID, 31),
    BANK_QUANTITY_X(WidgetID.BANK_GROUP_ID, 33),
    BANK_QUANTITY_ALL(WidgetID.BANK_GROUP_ID, 35),

    NPC_CONTACT_DARK_MAGE(75, 12),

    SETTINGS_TOGGLE_RUN(261, 97),


    /** Tutorial Island **/

    WELCOME_TO_RUNESCAPE(269, 97),
    ACCEPT(269, 100),

    HEAD_DESIGN_LEFT(269, 106),
    JAW_DESIGN_LEFT(269, 107),
    TORSO_DESIGN_LEFT(269, 108),
    ARMS_DESIGN_LEFT(269, 109),
    HANDS_DESIGN_LEFT(269, 110),
    LEGS_DESIGN_LEFT(269, 111),
    FEET_DESIGN_LEFT(269, 112),
    HEAD_DESIGN_RIGHT(269, 113),
    JAW_DESIGN_RIGHT(269, 114),
    TORSO_DESIGN_RIGHT(269, 115),
    ARMS_DESIGN_RIGHT(269, 116),
    HANDS_DESIGN_RIGHT(269, 117),
    LEGS_DESIGN_RIGHT(269, 118),
    FEET_DESIGN_RIGHT(269, 119),

    HAIR_COLOUR_LEFT(269, 105),
    TORSO_COLOUR_LEFT(269, 123),
    LEGS_COLOUR_LEFT(269, 122),
    FEET_COLOUR_LEFT(269, 124),
    SKIN_COLOUR_LEFT(269, 125),
    HAIR_COLOUR_RIGHT(269, 121),
    TORSO_COLOUR_RIGHT(269, 127),
    LEGS_COLOUR_RIGHT(269, 129),
    FEET_COLOUR_RIGHT(269, 130),
    SKIN_COLOUR_RIGHT(269, 131),

    MALE(269, 136),
    FEMALE(269, 137),
    ;

    val id get() = groupId shl 16 or childId
}

/**
 * The following allows us to define our own WidgetInfo enums and use the
 * client#getWidget(WidgetInfo) function instead of getWidget(int, int) for a
 * more consistent interface.
 */
fun Client.getWidget(wi: WidgetInfo): Widget? {
    return getWidget(wi.groupId, wi.childId)
}
