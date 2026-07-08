package com.crewco.foliatemplate.gui

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent

/**
 * Routes inventory interactions to the correct [Menu]. Register once in
 * onEnable: `server.pluginManager.registerEvents(MenuListener(), this)`.
 *
 * Inventory events for a player fire on that player's region thread on Folia,
 * so this handler is already on the right thread to touch the player.
 */
class MenuListener : Listener {

    @EventHandler
    fun onClick(event: InventoryClickEvent) {
        val menu = event.view.topInventory.holder as? Menu ?: return

        // Cancel the entire event: prevents taking items out AND shift-clicking
        // items in from the player's own inventory while the menu is open.
        event.isCancelled = true

        val player = event.whoClicked as? Player ?: return
        // Only dispatch clicks that land inside the menu, not the player inventory.
        if (event.rawSlot in 0 until event.view.topInventory.size) {
            menu.dispatch(ClickContext(player, event.rawSlot, event.click, event))
        }
    }

    @EventHandler
    fun onDrag(event: InventoryDragEvent) {
        // Block dragging across a menu (which could otherwise move/insert items).
        if (event.view.topInventory.holder is Menu) event.isCancelled = true
    }
}
