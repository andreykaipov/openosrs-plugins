package com.kaipov.common.extensions.client

import java.awt.Dimension
import net.runelite.api.*
import net.runelite.api.InventoryID.INVENTORY
import net.runelite.api.coords.WorldPoint
import net.runelite.api.events.Event
import net.runelite.api.queries.*
import net.runelite.client.eventbus.EventBus

// Assumes the item is in our
//fun Client.deposit(item: ItemDefinition, quantity: Int) {
//    return getEquipment().takeIf { index <= it.size }?.get(index)
//}

//    return InventoryWidgetItemQuery().result(this).toList().map { getItemDefinition(it.id) }


fun Client.getWorn(slot: EquipmentInventorySlot): Item? {
    val index = slot.slotIdx
    return getEquipment().takeIf { index <= it.size }?.get(index)
}

// For some reason, even if the player is not wearing an item in the specified
// slot, the item will sometimes not be null, but it will have an ID of -1. So,
// to check if we are wearing something, we should check against both of these
// conditions.
fun Client.isWearing(slot: EquipmentInventorySlot): Boolean {
    val item = getWorn(slot)
    return item != null && item.id != -1
}

/**
 * The size of the returned list will always be 28. This makes things more
 * consistent as normally the size varies depending on if an item has not yet
 * occupied a slot since the game started.
 *
 * Empty slots in the inventory have ID -1 and quantity 0.
 *
 * @see net.runelite.api.ItemContainer
 */
fun Client.getInventory(): List<Item> {
    val inventory = getItemContainer(INVENTORY)?.items.orEmpty().toMutableList()
    inventory.addAll(List(28 - inventory.size) { Item(-1, 0) })
    return inventory
}

/**
 * Checks the inventory for the specified item with an optional quantity.
 * If the quantity is 0 or negative, this always returns true. If the quantity
 * is less than the count, we're ok.
 */
fun Client.hasAtLeastInInventory(itemID: Int, quantity: Int = 1): Boolean {
    val count = getItemContainer(INVENTORY)?.count(itemID) ?: 0
    return count >= quantity
}

fun Client.hasNoMoreThanInInventory(itemID: Int, quantity: Int = 1): Boolean {
    val count = getItemContainer(INVENTORY)?.count(itemID) ?: 0
    return count <= quantity
}

fun Client.hasExactlyInInventory(itemID: Int, quantity: Int = 1): Boolean {
    val count = getItemContainer(INVENTORY)?.count(itemID) ?: 0
    return count == quantity
}

fun Client.hasInInventory(itemID: Int, quantity: Int = 1) = hasAtLeastInInventory(itemID, quantity)

fun Client.hasEmptyInventory() = getInventory().all { it.id == -1 && it.quantity == 0 }

/**
 * Returns the first or last index of the item matching the given item ID in our
 * inventory, or -1 if there's no such item.
 */
fun Client.findInInventory(itemID: Int) = findFirstInInventory(itemID)
fun Client.findFirstInInventory(itemID: Int) = getInventory().indexOfFirst { it.id == itemID }
fun Client.findLastInInventory(itemID: Int) = getInventory().indexOfLast { it.id == itemID }

fun Client.hasBankOpen(): Boolean {
    return getItemContainer(InventoryID.BANK) != null
}

fun Client.getEquipment(): List<Item> {
    return getItemContainer(InventoryID.EQUIPMENT)?.items.orEmpty().toList()
}

fun Client.getBank(): List<Item> {
    return getItemContainer(InventoryID.BANK)?.items.orEmpty().toList()
}

// Returns the index of the first item matching the given item ID in our
// inventory, or -1 if there's no such item.
fun Client.findFirstInBank(itemID: Int): Int {
    return getBank().indexOfFirst { it.id == itemID }
}

fun Client.hasInBank(itemID: Int): Boolean {
    return findFirstInBank(itemID) != -1
}


//        run {
//            print(this.size)
//
//            if (items.size <= EquipmentInventorySlot.CAPE.slotIdx) {
//                false
//            } else Graceful.CAPE.ids.contains(items[EquipmentInventorySlot.CAPE.slotIdx].id)
//        }
//    }

fun Client.inventoryFull(): Boolean {
    return getInventory().count { it.id != -1 } == 28
}


//    return getWidget(WidgetInfo.INVENTORY)?.let {
//        it.getWidgetItems()
//    } ?: listOf()

//    if (inventoryWidget != null) {
//        return inventoryWidget.getWidgetItems()
//    }
//
//    val inventoryItems: Collection<WidgetItem> = getAllInventoryItems()
//    if (inventoryItems != null) {
//        val inventoryIDs: MutableSet<Int> = HashSet()
//        for (item in inventoryItems) {
//            if (inventoryIDs.contains(item.id)) {
//                continue
//            }
//            inventoryIDs.add(item.id)
//        }
//        return inventoryIDs
//    }
//    return null
//}

fun Client.dimensions(): Dimension = if (isStretchedEnabled) {
    stretchedDimensions
} else {
    realDimensions
}

fun Client.interacting(): Boolean {
    assert(isClientThread)
    return localPlayer?.let { it.interacting != null }!!
}

fun Client.hasRunOn(): Boolean {
    assert(isClientThread)
    return getVarpValue(173) == 1
}

/**
 * Ideally we can always just look at the local player's world location and use
 * that. However, sometimes we're in an instance like the Pest Control map, and
 * must use the player's local location to resolve this.
 */
fun Client.inRegion(vararg ids: Int): Boolean {
    assert(isClientThread)
    if (localPlayer == null) return false
    return if (isInInstancedRegion) {
        ids.any { it == WorldPoint.fromLocalInstance(this, localPlayer!!.localLocation)?.regionID }
    } else {
        ids.any { it == localPlayer!!.worldLocation.regionID }
    }
}

fun Client.inRegion(ids: List<Int>): Boolean {
    return inRegion(*ids.toIntArray())
}

/**
 * The upper bound is a radial distance of 200 between the local player and
 * whatever we're looking for.
 */
inline fun <reified T> Client.findNearest(vararg ids: Int, distance: Int = 100): T? {
    assert(isClientThread)
    if (localPlayer == null) return null
    return findNearestWithin(localPlayer!!.worldLocation, distance, *ids)
}

/**
 * The Runelite API query classes aren't that well abstracted, so we have to
 * duplicate some code in each when-clause unfortunately.
 */
inline fun <reified T> Client.findNearestWithin(p: WorldPoint, distance: Int, vararg ids: Int): T? {
    assert(isClientThread)
    if (localPlayer == null) return null

    val result = when (T::class) {
        NPC::class -> NPCQuery().idEquals(*ids).result(this)
        GameObject::class -> GameObjectQuery().isWithinDistance(p, distance).idEquals(*ids).result(this)
        WallObject::class -> WallObjectQuery().filter { it.worldLocation.distanceTo2D(p) <= distance }.idEquals(*ids).result(this)
        GroundObject::class -> GroundObjectQuery().isWithinDistance(p, distance).idEquals(*ids).result(this)
        DecorativeObject::class -> DecorativeObjectQuery().isWithinDistance(p, distance).idEquals(*ids).result(this)
        else -> return null
    }

    return result.nearestTo(localPlayer) as T?
}

fun MenuEntry.equals(m: MenuEntry): Boolean {
    return option == m.option &&
        target == m.target &&
        identifier == m.identifier &&
        opcode == m.opcode &&
        param0 == m.param0 &&
        param1 == m.param1 &&
        isForceLeftClick == m.isForceLeftClick
}

val CancelMenuEntryEvent = MenuEntry(
    "Cancel",
    "",
    0,
    MenuOpcode.CANCEL.id,
    0,
    0,
    false
)

//fun <T : Event> EventBus.subscribe(eventClass: Class<T>, ID: String, take: Int, action: (T) -> Unit) {
//    subscribe(eventClass, ID, action, take)
//}

/**
 * Reorganizes the parameters so that the lambda is last so we don't have to
 * include it within parentheses.
 */
inline fun <reified T : Event> EventBus.subscribe(lifecycle: Any, noinline action: (T) -> Unit) {
    subscribe(T::class.java, lifecycle, action)
}

/**
 * Here we don't have to worry about a lifecycle, so long as we provide a finite
 * amount of events our disposable observable will listen for. Doing so allows
 * us to not worry about unregistering the subscription from the EventBus. The
 * observable will clean itself up via its doFinally clause. See the appropriate
 * subscribe method from @{link net.runelite.client.eventbus.EventBus}.
 */
inline fun <reified T : Event> EventBus.subscribe(n: Int, noinline action: (T) -> Unit) {
    subscribe(T::class.java, Any(), action, n)
}

/**
 * This is just a convenience method for when we want to observe an event only
 * one time. In addition, the action re-scopes the event on `this`, so that we
 * get cool almost English-like code as follows:
 *
 * ```
 * events.once<MenuOptionClicked> { overwriteWith(MenuOption.GAMES_NECKLACE_TO_WINTERTODT) }
 * client.singleClickCenterScreenRandom()
 * ```
 */
inline fun <reified T : Event> EventBus.once(noinline action: T.() -> Unit) {
    subscribe(T::class.java, Any(), action, 1)
}

fun Player.isTeleporting(): Boolean = animation == 714
