package com.kaipov.plugins.tanleather

import com.kaipov.plugins.AFKGE
import com.kaipov.plugins.AfkStates.*
import com.kaipov.plugins.common.bot.DetailedStates.Stop
import com.kaipov.plugins.common.bot.States.Start
import com.kaipov.plugins.common.bot.extras.wait
import com.kaipov.plugins.extensions.client.getInventory
import com.kaipov.plugins.extensions.client.hasInBank
import com.kaipov.plugins.extensions.client.hasInInventory
import com.kaipov.plugins.extensions.menuoption.MenuOption.Companion.Quantity.ALL
import com.kaipov.plugins.extensions.menuoption.MenuOption.Companion.SPELL_TAN_LEATHER
import net.runelite.api.ItemID.ASTRAL_RUNE
import net.runelite.api.ItemID.NATURE_RUNE

fun AFKGE.tanLeather() {
    val input = config.tanLeatherInput()

    state(Start) {
        checkRunes()?.let { return@state Stop.with(it) }
        checkInventory()?.let { return@state Stop.with(it) }
        bankOpen()
        check(input)?.let { return@state Stop.with(it) }
        return@state Withdraw
    }

    state(Withdraw) {
        bankWithdraw(input.id, ALL)
        check(input)?.let { return@state Stop.with(it) }
        bankClose()
        wait(300..600)
        return@state Act
    }

    state(Act) {
        repeat(5) {
            checkRunes()?.let { return@state Stop.with(it) }
            check(input)?.let { return@state Stop.with(it) }
            click(SPELL_TAN_LEATHER)
            wait(1850..2250)
        }
        return@state Deposit
    }

    state(Deposit) {
        bankOpen()
        bankDeposit(input.outputID, ALL)
        return@state Withdraw
    }
}

fun AFKGE.check(input: TanLeatherInputs): String? {
    if (!client.hasInInventory(input.id) && !client.hasInBank(input.id)) {
        return "We need $input in our inventory or bank"
    }

    if (!client.hasInInventory(ASTRAL_RUNE, 2) || !client.hasInInventory(NATURE_RUNE)) {
        return "We need astrals and natures in our inventory"
    }

    return null
}

fun AFKGE.checkRunes(): String? {
    if (!client.hasInInventory(ASTRAL_RUNE, 2) || !client.hasInInventory(NATURE_RUNE)) {
        return "We need astrals and natures in our inventory"
    }

    return null
}


fun AFKGE.checkInventory(): String? {
    if (client.getInventory().count { it.id == -1 } != 25) {
        return "We need exactly 25 free slots in our inventory"
    }

    return null
}
