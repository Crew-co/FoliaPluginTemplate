package com.example.foliatemplate.gui

import com.example.foliatemplate.scheduler.Schedulers
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack

/**
 * Base class for a chest-style GUI.
 *
 * How identification works: the menu IS its own inventory's [InventoryHolder]
 * (passed to `createInventory(this, ...)`). [MenuListener] then recognises a
 * click by checking `topInventory.holder is Menu` — so there's no global
 * open-menus registry to keep thread-safe.
 *
 * Thread-safety on Folia: create a NEW instance per open, per player (see
 * `ExampleCommand.menu`). Everything that touches this instance — [build],
 * [open], and click dispatch — runs on that player's region thread, so a single
 * player's menu is effectively single-threaded and needs no locks. Sharing one
 * instance across players would reintroduce shared mutable state; don't, unless
 * you synchronize it yourself.
 */
abstract class Menu(
    private val schedulers: Schedulers,
    title: Component,
    rows: Int,
) : InventoryHolder {

    init {
        require(rows in 1..6) { "A chest menu must have 1-6 rows, got $rows" }
    }

    private val handlers = HashMap<Int, (ClickContext) -> Unit>()
    private val inventory: Inventory = Bukkit.createInventory(this, rows * 9, title)

    /** Total slots (rows * 9). */
    protected val size: Int get() = inventory.size

    override fun getInventory(): Inventory = inventory

    /**
     * Populate the menu with [button] / [item] / [fill]. Called on the viewer's
     * region thread each time the menu opens, so reading live state (the player,
     * their stats, config, ...) while building is safe.
     */
    protected abstract fun build(player: Player)

    /** A clickable icon. [onClick] runs on the clicker's region thread. */
    protected fun button(slot: Int, icon: ItemStack, onClick: (ClickContext) -> Unit) {
        inventory.setItem(slot, icon)
        handlers[slot] = onClick
    }

    /** A non-interactive decoration icon. */
    protected fun item(slot: Int, icon: ItemStack) {
        inventory.setItem(slot, icon)
    }

    /** Fill every currently-empty slot (e.g. a glass-pane border/background). */
    protected fun fill(icon: ItemStack) {
        for (i in 0 until inventory.size) {
            if (inventory.getItem(i) == null) inventory.setItem(i, icon)
        }
    }

    /** Open (or refresh) this menu for [player] — safely on their region thread. */
    fun open(player: Player) {
        schedulers.entity(player) {
            handlers.clear()
            inventory.clear()
            build(player)
            player.openInventory(inventory)
        }
    }

    internal fun dispatch(ctx: ClickContext) {
        handlers[ctx.slot]?.invoke(ctx)
    }
}
