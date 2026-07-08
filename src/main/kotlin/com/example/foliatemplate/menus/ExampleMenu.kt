package com.example.foliatemplate.menus

import com.example.foliatemplate.gui.Menu
import com.example.foliatemplate.gui.icon
import com.example.foliatemplate.scheduler.Schedulers
import com.example.foliatemplate.util.mm
import org.bukkit.Material
import org.bukkit.entity.Player

/**
 * A 3-row example menu. Open it with `new ExampleMenu(schedulers).open(player)`.
 *
 * Create a fresh instance per open (see `ExampleCommand.menu`) so each player's
 * menu stays confined to their own region thread.
 */
class ExampleMenu(schedulers: Schedulers) : Menu(
    schedulers,
    title = mm("<gradient:#7f5af0:#2cb67d>Example Menu</gradient>"),
    rows = 3,
) {
    override fun build(player: Player) {
        button(
            11,
            icon(Material.LIME_DYE, "<green><bold>Greet me", "<gray>Left-click for a hello"),
        ) { ctx ->
            ctx.reply("<green>Hello, ${ctx.player.name}!")
        }

        button(
            13,
            icon(Material.CLOCK, "<yellow>Your ping", "<gray>Click to check"),
        ) { ctx ->
            ctx.reply("<yellow>Ping: <white>${ctx.player.ping}ms")
        }

        button(
            15,
            icon(Material.BARRIER, "<red>Close"),
        ) { ctx ->
            ctx.close()
        }

        // Background: fill the remaining slots with a subtle glass pane.
        fill(icon(Material.GRAY_STAINED_GLASS_PANE, " "))
    }
}
