package com.kaipov.plugins.tanleather

import com.kaipov.common.bot.DetailedStates.Stop
import com.kaipov.common.bot.State
import com.kaipov.common.bot.States.Start
import com.kaipov.common.bot.extras.wait
import com.kaipov.common.extensions.client.getInventory
import com.kaipov.common.extensions.client.hasInBank
import com.kaipov.common.extensions.client.hasInInventory
import com.kaipov.common.extensions.menuoption.MenuOption.Companion.Quantity.ALL
import com.kaipov.common.extensions.menuoption.MenuOption.Companion.SPELL_TAN_LEATHER
import com.kaipov.plugins.AFKGE
import com.kaipov.plugins.AfkStates.*
import net.runelite.api.ItemID.ASTRAL_RUNE
import net.runelite.api.ItemID.NATURE_RUNE

fun AFKGE.tanLeather() {
    val input = config.tanLeatherInput()

    state(Start) {
        checkRunes()?.let { return@state it }
        checkInventory()?.let { return@state it }
        bankOpen()
        check(input)?.let { return@state it }
        return@state Withdraw
    }

    state(Withdraw) {
        bankWithdraw(input.id, ALL)
        check(input)?.let { return@state it }
        bankClose()
        wait(300..600)
        return@state Act
    }

    state(Act) {
        repeat(5) {
            checkRunes()?.let { return@state it }
            check(input)?.let { return@state it }
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

fun AFKGE.check(input: TanLeatherInputs): State? {
    if (!client.hasInInventory(input.id) && !client.hasInBank(input.id)) {
        return Stop.with("We need $input in our inventory or bank")
    }

    if (!client.hasInInventory(ASTRAL_RUNE, 2) || !client.hasInInventory(NATURE_RUNE)) {
        return Stop.with("We need astrals and natures in our inventory")
    }

    return null
}

fun AFKGE.checkRunes(): State? {
    if (!client.hasInInventory(ASTRAL_RUNE, 2) || !client.hasInInventory(NATURE_RUNE)) {
        return Stop.with("We need astrals and natures in our inventory")
    }

    return null
}


fun AFKGE.checkInventory(): State? {
    if (client.getInventory().count { it.id == -1 } != 25) {
        return Stop.with("We need exactly 25 free slots in our inventory")
    }

    return null
}
