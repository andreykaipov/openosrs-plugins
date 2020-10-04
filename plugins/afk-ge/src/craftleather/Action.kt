package com.kaipov.plugins.craftleather

import com.kaipov.plugins.AFKGE
import com.kaipov.plugins.AfkStates.*
import com.kaipov.plugins.common.bot.DetailedStates.Stop
import com.kaipov.plugins.common.bot.State
import com.kaipov.plugins.common.bot.States.Start
import com.kaipov.plugins.common.bot.extras.wait
import com.kaipov.plugins.common.bot.extras.waitUntilPlayerHasBeenIdleForMoreThan
import com.kaipov.plugins.extensions.client.hasAtLeastInInventory
import com.kaipov.plugins.extensions.client.hasInInventory
import com.kaipov.plugins.extensions.client.pressKey
import com.kaipov.plugins.extensions.menuoption.MenuOption.Companion.Quantity.ALL
import java.awt.event.KeyEvent
import net.runelite.api.ItemID

fun AFKGE.craftLeather() {
    val input = config.craftLeatherInput()
    val output = config.craftLeatherOutput()

    state(Start) {
        check(input)?.let { return@state it }
        bankOpen()
        return@state Withdraw
    }

    state(Withdraw) {
        bankWithdraw(input.id, ALL)
        check(input)?.let { return@state it }
        bankClose()
        wait(600..800)
        return@state Act
    }

    state(Act) {
        useItemOnItem(ItemID.NEEDLE, input.id)
        wait(1500..2000)
        client.pressKey(KeyEvent.VK_SPACE)
        waitUntilPlayerHasBeenIdleForMoreThan(1400..1600, 20000L)
        return@state Deposit
    }

    state(Deposit) {
        bankOpen()
        bankDeposit(output.id, ALL)
        return@state Withdraw
    }
}

fun AFKGE.check(input: CraftLeatherInputs): State? {
    if (!client.hasInInventory(ItemID.NEEDLE)) {
        return Stop.with("We need a needle in our inventory")
    }

    if (!client.hasInInventory(ItemID.THREAD)) {
        return Stop.with("We need thread in our inventory")
    }

    if (state == Withdraw && !client.hasAtLeastInInventory(input.id, 3)) {
        return Stop.with("We need at least 3 pieces of $input in our inventory")
    }

    return null
}
