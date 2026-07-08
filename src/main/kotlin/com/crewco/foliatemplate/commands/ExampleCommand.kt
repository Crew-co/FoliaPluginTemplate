package com.crewco.foliatemplate.commands

import com.crewco.foliatemplate.FoliaTemplatePlugin
import com.crewco.core.command.Command
import com.crewco.core.command.CommandContext
import com.crewco.core.command.Default
import com.crewco.core.command.Subcommand
import com.crewco.core.command.TabComplete
import com.crewco.foliatemplate.menus.ExampleMenu
import com.crewco.foliatemplate.pdc.getString
import com.crewco.foliatemplate.pdc.setString
import org.bukkit.Bukkit
import java.time.Duration
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * A worked example. Run in-game as:
 *
 *   /example                    → the @Default handler (help text)
 *   /example ping               → simple reply
 *   /example tp <player>        → teleport, on the entity's owning region
 *   /example boom               → schedule an explosion on the block's region
 *   /example count <n>          → async work that reports back on the right thread
 *   /example counter            → thread-safe shared state done correctly
 *   /example menu               → opens the example GUI
 *   /example note [text]        → persistent per-player storage (PDC)
 *   /example reload             → hot-reload config + messages
 *
 * Thread-safety notes are inline. The golden rule: a command handler already
 * runs on the region that owns the sender, so touching the sender/world here is
 * safe — but the moment you hop onto an async thread you must hop back onto an
 * owning region (see `count`) before touching the game again.
 */
@Command(
    name = "example",
    description = "Demonstrates the command framework and Folia schedulers.",
    aliases = ["ex"],
    permission = "foliatemplate.example",
)
class ExampleCommand(private val plugin: FoliaTemplatePlugin) {

    // Shared plugin state is NOT automatically thread-safe on Folia: different
    // regions call commands on different threads in parallel. A ConcurrentHashMap
    // of atomics is safe to read and update from any of them without locking.
    private val runCounts = ConcurrentHashMap<UUID, AtomicInteger>()

    @Default
    fun help(ctx: CommandContext) {
        ctx.info("Try: <white>ping</white>, <white>tp</white>, <white>boom</white>, <white>count</white>, <white>counter</white>, <white>menu</white>, <white>note</white>, <white>reload</white>")
    }

    @Subcommand("ping", description = "Replies with pong.")
    fun ping(ctx: CommandContext) {
        ctx.success("Pong!")
    }

    // playerOnly + minArgs are enforced by the framework before this runs.
    @Subcommand("tp", playerOnly = true, minArgs = 1, usage = "/example tp <player>", description = "Teleport to a player.")
    fun teleport(ctx: CommandContext) {
        val me = ctx.requireSenderPlayer()
        val target = ctx.requirePlayer(0) // throws a friendly error if offline

        // A teleport must run on the region owning the entity being moved, and
        // must use teleportAsync (never the blocking teleport). Scheduling on
        // the entity guarantees both, even if `me` is in another region.
        plugin.schedulers.entity(me) {
            me.teleportAsync(target.location)
            ctx.success("Teleported to ${target.name}.")
        }
    }

    @Subcommand("boom", playerOnly = true, description = "Cosmetic explosion at your feet.")
    fun boom(ctx: CommandContext) {
        val player = ctx.requireSenderPlayer()

        // Thread-safe cooldown check; duration comes from config.
        val cooldown = Duration.ofSeconds(plugin.config.current.boomCooldownSeconds)
        if (!plugin.cooldowns.test(player.uniqueId, cooldown)) {
            val left = plugin.cooldowns.remainingSeconds(player.uniqueId).toString()
            player.sendMessage(plugin.messages.component("boom-cooldown", "seconds" to left))
            return
        }

        val loc = player.location
        val power = plugin.config.current.explosionPower
        // Changing the world at a location must run on that location's region.
        plugin.schedulers.region(loc) {
            loc.world.createExplosion(loc, power, false, false) // no block damage
            ctx.info("Boom.")
        }
    }

    @Subcommand("count", minArgs = 1, usage = "/example count <n>", description = "Async countdown demo.")
    fun count(ctx: CommandContext) {
        val n = ctx.requireInt(0, "a whole number")
        if (n !in 1..10) {
            ctx.error("Pick a number from 1 to 10.")
            return
        }
        val sender = ctx.sender // capture; we're about to leave this thread

        plugin.schedulers.async {
            for (i in n downTo 1) {
                Thread.sleep(500) // stands in for real blocking work (HTTP, DB, file)
                // We're on an async thread now, so we can't message directly —
                // hop back onto the sender's owning region to talk to the game.
                plugin.schedulers.forSender(sender) { ctx.info("$i...") }
            }
            plugin.schedulers.forSender(sender) { ctx.success("Done!") }
        }
    }

    @Subcommand("counter", playerOnly = true, description = "Thread-safe per-player counter.")
    fun counter(ctx: CommandContext) {
        val player = ctx.requireSenderPlayer()
        // computeIfAbsent + AtomicInteger.incrementAndGet are both atomic, so
        // this is correct even if the same player somehow triggers it from two
        // region threads at once. No `synchronized`, no lost updates.
        val count = runCounts.computeIfAbsent(player.uniqueId) { AtomicInteger() }.incrementAndGet()
        ctx.success("You've run /example counter <white>$count</white> time(s).")
    }

    @Subcommand("menu", playerOnly = true, description = "Opens the example GUI.")
    fun menu(ctx: CommandContext) {
        val player = ctx.requireSenderPlayer()
        // A fresh menu instance per open keeps this player's menu state on their
        // own region thread. open() schedules the actual inventory open safely.
        ExampleMenu(plugin.schedulers).open(player)
    }

    @Subcommand("note", playerOnly = true, usage = "/example note [text]", description = "Save/read a personal note (PDC).")
    fun note(ctx: CommandContext) {
        val player = ctx.requireSenderPlayer()
        // PDC on a player must be touched on the player's region thread — a
        // command handler for that player already is, so this is safe. The note
        // persists across restarts with no database.
        if (ctx.isEmpty) {
            val saved = player.getString(plugin, "note")
            if (saved == null) ctx.info("No note saved. Set one with /example note <text>.")
            else ctx.reply("<gray>Your note: <white>$saved")
        } else {
            player.setString(plugin, "note", ctx.joinFrom(0))
            ctx.success("Saved your note.")
        }
    }

    @Subcommand("reload", permission = "foliatemplate.example.reload", description = "Reload config and messages.")
    fun reload(ctx: CommandContext) {
        val sender = ctx.sender
        // Both reloads read the files off-thread; reply once messages are ready.
        plugin.config.reloadAsync(plugin.schedulers)
        plugin.messages.reloadAsync(plugin.schedulers) {
            plugin.schedulers.forSender(sender) {
                sender.sendMessage(plugin.messages.component("reloaded"))
            }
        }
    }

    // Completes the <player> argument of `/example tp <...>`.
    @TabComplete(subcommand = "tp")
    fun completeTp(ctx: CommandContext): List<String> =
        Bukkit.getOnlinePlayers().map { it.name }
}
