package com.crewco.foliatemplate.listeners

import com.crewco.foliatemplate.FoliaTemplatePlugin
import com.crewco.foliatemplate.util.mm
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.player.PlayerJoinEvent

/**
 * Example listeners.
 *
 * Folia note: an event handler runs on the region that owns the relevant
 * player/block, so touching them here is thread-safe — the same rule commands
 * follow. If you need to reach a DIFFERENT entity or location than the event's,
 * schedule onto its region (see the Schedulers helper). Listener registration
 * itself happens once in onEnable and is fine on Folia.
 */
class ExampleListener(private val plugin: FoliaTemplatePlugin) : Listener {

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        val player = event.player
        // Safe: this fires on the joining player's region thread.
        if (plugin.config.current.welcomeEnabled) {
            player.sendMessage(plugin.messages.component("welcome", "player" to player.name))
        }

        // A short delayed follow-up, scheduled on the player's own region. The
        // optional retired callback runs instead if they log out first.
        plugin.schedulers.entityDelayed(player, delayTicks = 40L, retired = {
            plugin.logger.info("${player.name} left before the welcome follow-up.")
        }) {
            player.sendMessage(mm("<dark_gray>(scheduled with the entity scheduler, 2s later)"))
        }
    }

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        // Fires on the region that owns the block, so nearby world access is safe.
        if (event.block.type == Material.DIAMOND_ORE) {
            event.player.sendMessage(mm("<aqua>Nice find!"))
        }
    }
}
