package com.kaipov.plugins.extensions.menuoption

import com.kaipov.plugins.extensions.menuoption.MenuOption.Companion.Quantity.*
import net.runelite.api.*
import net.runelite.api.MenuOpcode.*
import net.runelite.api.events.MenuOptionClicked
import net.runelite.api.widgets.WidgetInfo
import com.kaipov.plugins.extensions.widgets.WidgetInfo as MyWidgetInfo

/**
 * Overwrites a clicked menu option's menu entry to the given one. The option
 * and target are not sent to Jagex's servers, so they can be left blank.
 */
fun MenuOptionClicked.overwriteWith(id: Int, opcode: MenuOpcode, param0: Int, param1: Int) {
    setMenuEntry(MenuEntry("", "", id, opcode.id, param0, param1, false))
}

fun MenuOptionClicked.overwriteWith(o: MenuOption) {
    overwriteWith(o.id, o.opcode, o.param0, o.param1)
}

class MenuOption(val id: Int, val opcode: MenuOpcode, val param0: Int, val param1: Int) {
    /**
     * The getters inside these companion object are effectively an enum class for predefined MenuOptions.
     * We use this approach so that configurable menu options and static menu options can be instances of
     * the same class.
     */
    companion object {
        // Assumed we're wearing these jewellery items
        val GAMES_NECKLACE_TO_WINTERTODT     = MenuOption(6, CC_OP_LOW_PRIORITY, -1, WidgetInfo.EQUIPMENT_AMULET.id)
        val RING_OF_DUELING_TO_FEROX_ENCLAVE = MenuOption(4, CC_OP, -1, WidgetInfo.EQUIPMENT_RING.id)

        val BANK_DEPOSIT_INVENTORY  = MenuOption(1, CC_OP, -1, WidgetInfo.BANK_DEPOSIT_INVENTORY.id)
        val SPELL_OURANIA_TELELPORT = MenuOption(1, CC_OP, -1, WidgetInfo.SPELL_OURANIA_TELEPORT.id)
        val SPELL_NPC_CONTACT       = MenuOption(1, CC_OP, -1, WidgetInfo.SPELL_NPC_CONTACT.id)
        val SPELL_TAN_LEATHER       = MenuOption(1, CC_OP, -1, WidgetInfo.SPELL_TAN_LEATHER.id)
        val DIALOG_NPC_CONTINUE     = MenuOption(0, WIDGET_TYPE_6, -1, WidgetInfo.DIALOG_NPC_CONTINUE.id)
        val DIALOG_PLAYER_CONTINUE  = MenuOption(0, WIDGET_TYPE_6, -1, WidgetInfo.DIALOG_PLAYER_CONTINUE.id)

        // Menu options from our extra widgets
        val BANK_CLOSE            = MenuOption(1, CC_OP, 11, MyWidgetInfo.BANK_CONTAINER_BORDERS.id)
        val NPC_CONTACT_DARK_MAGE = MenuOption(1, CC_OP, -1, MyWidgetInfo.NPC_CONTACT_DARK_MAGE.id)
        val TOGGLE_RUN            = MenuOption(1, CC_OP, -1, MyWidgetInfo.SETTINGS_TOGGLE_RUN.id)

        /**
         * Inventory menu options need both the itemID and index (0-27) corresponding to the location of the item in
         * the inventory. The menu option's ID is the item ID.
         * @see ItemID
         */
        fun INVENTORY_FIRST_OPTION(index: Int, id: Int)  = MenuOption(id, ITEM_FIRST_OPTION, index, WidgetInfo.INVENTORY.id)
        fun INVENTORY_SECOND_OPTION(index: Int, id: Int) = MenuOption(id, ITEM_SECOND_OPTION, index, WidgetInfo.INVENTORY.id)
        fun USE(index: Int, id: Int)                     = MenuOption(id, ITEM_USE, index, WidgetInfo.INVENTORY.id)
        fun USE_ON_ITEM(index: Int, id: Int)             = MenuOption(id, ITEM_USE_ON_WIDGET_ITEM, index, WidgetInfo.INVENTORY.id)
        fun DROP(index: Int, id: Int)                    = MenuOption(id, ITEM_DROP, index, WidgetInfo.INVENTORY.id)

        /**
         * Banking inventory menu options use a different ID and opcode for the different quantities to withdraw or
         * deposit. The index (0-27) corresponds to the location of the item in the bank/inventory respectively.
         *
         * A quantity of X will open up a prompt, and won't withdraw anything until the client sends the appropriate
         * keys. PRESET corresponds to that already set quantity.
         */
        enum class Quantity(var value: Int) {
            ONE(1), FIVE(5), TEN(10), PRESET(-1), X(-1), ALL(28);

            fun at(v: Int): Quantity {
                value = when (this) {
                    PRESET -> v
                    X      -> v
                    else   -> throw IllegalArgumentException("Can only set PRESET and X Quantities")
                }
                return this
            }
        }

        // Assumes the selected quantity in the bank is set to "1" !!! If it's something else, the behavior will be
        // unexpected. For example, if the selected quantity is "10", then an id of 2 will withdraw ten, and an id
        // of 3 will withdraw one, so before calling this method, make sure we've selected "1".
        fun DEPOSIT(index: Int, quantity: Quantity): MenuOption {
            val p = when (quantity) {
                ONE    -> Pair(2, CC_OP)
                FIVE   -> Pair(4, CC_OP)
                TEN    -> Pair(5, CC_OP)
                PRESET -> Pair(6, CC_OP)
                X      -> Pair(7, CC_OP_LOW_PRIORITY)
                ALL    -> Pair(8, CC_OP_LOW_PRIORITY)
            }
            return MenuOption(id = p.first, opcode = p.second, index, WidgetInfo.BANK_INVENTORY_ITEMS_CONTAINER.id)
        }

        fun WITHDRAW(index: Int, quantity: Quantity): MenuOption {
            val p = when (quantity) {
                ONE    -> Pair(1, CC_OP)
                FIVE   -> Pair(3, CC_OP)
                TEN    -> Pair(4, CC_OP)
                PRESET -> Pair(5, CC_OP)
                X      -> Pair(6, CC_OP_LOW_PRIORITY)
                ALL    -> Pair(7, CC_OP_LOW_PRIORITY)
            }
            return MenuOption(id = p.first, opcode = p.second, index, WidgetInfo.BANK_ITEM_CONTAINER.id)
        }

        // You can wear armor or fill/empty rune pouches from the deposit inventory when the bank is open.
        // These use the ninth option.
        fun BANK_DEPOSIT_NINTH_OPTION(index: Int) = MenuOption(id = 9, CC_OP_LOW_PRIORITY, index, WidgetInfo.BANK_INVENTORY_ITEMS_CONTAINER.id)

        // Toggling the bank quantity selection for withdrawals/deposits.
        // Note that changing these potentially changes the behavior of the above functions.
        val BANK_QUANTITY_ONE   = MenuOption(id = 1, CC_OP, -1, MyWidgetInfo.BANK_QUANTITY_ONE.id)
        val BANK_QUANTITY_FIVE  = MenuOption(id = 1, CC_OP, -1, MyWidgetInfo.BANK_QUANTITY_FIVE.id)
        val BANK_QUANTITY_TEN   = MenuOption(id = 1, CC_OP, -1, MyWidgetInfo.BANK_QUANTITY_TEN.id)
        val BANK_QUANTITY_X     = MenuOption(id = 1, CC_OP, -1, MyWidgetInfo.BANK_QUANTITY_X.id) // only selects
        val BANK_QUANTITY_X_SET = MenuOption(id = 2, CC_OP, -1, MyWidgetInfo.BANK_QUANTITY_X.id) // prompt for custom amount
        val BANK_QUANTITY_ALL   = MenuOption(id = 1, CC_OP, -1, MyWidgetInfo.BANK_QUANTITY_ALL.id)

        /**
         * NPC menu options need the index of the NPC in the client's cached NPC array as the menu option's ID.
         * @see Client.getCachedNPCs
         *
         * The options differ between different NPCs (one NPC's talk option might be their first option,
         * when another NPC's talk option might their third option. Adjust your plugins accordingly.
         * May be affected by menu swapper options.
         */
        fun NPC_FIRST_OPTION(n: NPC)  = MenuOption(id = n.index, NPC_FIRST_OPTION, 0, 0)
        fun NPC_SECOND_OPTION(n: NPC) = MenuOption(id = n.index, NPC_SECOND_OPTION, 0, 0)

        fun GAME_OBJECT_FIRST_OPTION(o: GameObject)  = MenuOption(o.id, GAME_OBJECT_FIRST_OPTION, o.sceneMinLocation.x, o.sceneMinLocation.y)
        fun GAME_OBJECT_SECOND_OPTION(o: TileObject) = MenuOption(o.id, GAME_OBJECT_SECOND_OPTION, o.localLocation.sceneX, o.localLocation.sceneY)
    }

    override fun toString() = "id=${id}; opcode=${opcode.name}(${opcode.id}); params=(${param0}, ${param1})"
}

/**
 * This version of the function is meant for events where the parameters are a
 * canvas location, e.g. clicking on a tile object. The point can also be
 * omitted entirely if parameters are not necessary, in which case (0, 0) is
 * used.
 */
fun MenuOptionClicked.overwriteWith(id: Int, opcode: MenuOpcode, point: Point) {
    overwriteWith(id, opcode, point.x, point.y)
}

fun MenuOptionClicked.overwriteWith(o: GameObject) {
    overwriteWith(o.id, MenuOpcode.GAME_OBJECT_FIRST_OPTION, o.sceneMinLocation)
}

fun MenuOptionClicked.overwriteWith(o: WallObject) {
    overwriteWith(o.id, MenuOpcode.GAME_OBJECT_FIRST_OPTION, Point(o.localLocation.sceneX, o.localLocation.sceneY))
}

fun MenuOptionClicked.overwriteWith(o: GroundObject) {
    overwriteWith(o.id, MenuOpcode.GAME_OBJECT_FIRST_OPTION, o.canvasLocation)
}

fun MenuOptionClicked.overwriteWith(o: NPC) {
    overwriteWith(o.index, MenuOpcode.NPC_SECOND_OPTION, 0, 0)
}


// Overrides a clicked menu option's menu entry to the given one
//fun MenuOptionClicked.override(id: Int, opcode: MenuOpcode, point: Point) {
//    this.setMenuEntry(MenuEntry(
//        "option", "target", id, opcode.id, point.x, point.y, false
//    ))

