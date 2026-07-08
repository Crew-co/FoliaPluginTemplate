package com.example.foliatemplate.gui

import com.example.foliatemplate.util.mm
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent

/**
 * Everything a menu button handler needs. Handlers run on the clicking player's
 * region thread, so touching [player] (messages, closing, inventory) is safe.
 */
class ClickContext(
    val player: Player,
    /** Raw slot within the menu that was clicked. */
    val slot: Int,
    val type: ClickType,
    /** The underlying event, already cancelled by [MenuListener]. */
    val event: InventoryClickEvent,
) {
    val isLeftClick: Boolean get() = type.isLeftClick
    val isRightClick: Boolean get() = type.isRightClick
    val isShiftClick: Boolean get() = type.isShiftClick

    /** Close the menu for the player. */
    fun close() = player.closeInventory()

    fun reply(message: String) = player.sendMessage(mm(message))
    fun reply(message: Component) = player.sendMessage(message)
}
