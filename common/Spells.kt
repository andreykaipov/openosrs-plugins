/*
package com.kaipov.plugins.extensions


import net.runelite.api.Client
import net.runelite.api.Skill
import net.runelite.api.SpriteID.SPELL_TAN_LEATHER
import net.runelite.api.SpriteID.SPELL_TAN_LEATHER_DISABLED
import net.runelite.api.Varbits
import net.runelite.api.widgets.WidgetInfo

enum class Spells(
val widgetInfo: WidgetInfo,
val enabledSpriteID: Int,
val disabledSpriteID: Int,
val requiredMagicLevel: Int,
val spellbook: Int,
) {

TAN_LEATHER(WidgetInfo.SPELL_TAN_LEATHER, SPELL_TAN_LEATHER, SPELL_TAN_LEATHER_DISABLED, 78, 2),

;

override fun toString() = name.split("_").joinToString("") { it.capitalize() }

/**
 * Returns whether the given spell Widget is a valid selection for the existing client, e.g.
 * if the client has the magic level, is on the proper spellbook, and has the runes to cast the spell.
 * @param given
 * @param client
 * @return a boolean
 */
fun isValid(client: Client): Boolean {
    val w = client.getWidget(widgetInfo) ?: return false

    return w.spriteId == enabledSpriteID
        && w.spriteId != disabledSpriteID
        && client.getBoostedSkillLevel(Skill.MAGIC) >= requiredMagicLevel
        && client.getVar(Varbits.SPELLBOOK) == spellbook
}
}

fun Client.isValid(spell: Spells): Boolean {
val w = getWidget(spell.widgetInfo) ?: return false

return w.spriteId == spell.enabledSpriteID
    && w.spriteId != spell.disabledSpriteID
    && getBoostedSkillLevel(Skill.MAGIC) >= spell.requiredMagicLevel
    && getVar(Varbits.SPELLBOOK) == spell.spellbook
}
*/
