package com.crewco.foliatemplate.scheduler

import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.command.CommandSender
import org.bukkit.entity.Entity
import org.bukkit.plugin.Plugin
import java.util.concurrent.TimeUnit

/**
 * Folia has NO main thread. Each region ticks on its own thread, so you cannot
 * use the old `Bukkit.getScheduler()` / `runTask` API for anything that touches
 * world state. Instead you schedule onto whichever thread "owns" the thing you
 * want to modify:
 *
 *  - [global]  → the global region (world-wide state: time, weather, etc.)
 *  - [region]  → the region that owns a Location / chunk (blocks near it)
 *  - [entity]  → the region that owns an Entity (that entity, its inventory...)
 *  - [async]   → a background thread (I/O, HTTP, DB — never touch world state)
 *
 * Rule of thumb: to change a block, schedule on its [region]; to change an
 * entity/player, schedule on that [entity]; for network/disk work, use [async]
 * and then hop back to a region/entity scheduler to apply the result.
 *
 * All delays are in *ticks* (20 ticks = 1 second) except the async helpers,
 * which take a real duration.
 */
class Schedulers(private val plugin: Plugin) {

    // ---- global region ------------------------------------------------------

    /** Run once on the global region thread, next tick. */
    fun global(task: () -> Unit): ScheduledTask =
        Bukkit.getGlobalRegionScheduler().run(plugin) { task() }

    /** Run once on the global region after [delayTicks]. */
    fun globalDelayed(delayTicks: Long, task: () -> Unit): ScheduledTask =
        Bukkit.getGlobalRegionScheduler().runDelayed(plugin, { task() }, delayTicks)

    /** Repeat on the global region every [periodTicks], starting after [initialDelayTicks]. */
    fun globalRepeating(initialDelayTicks: Long, periodTicks: Long, task: (ScheduledTask) -> Unit): ScheduledTask =
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, { task(it) }, initialDelayTicks, periodTicks)

    // ---- location / region --------------------------------------------------

    /** Run once on the region owning [location], next tick. */
    fun region(location: Location, task: () -> Unit): ScheduledTask =
        Bukkit.getRegionScheduler().run(plugin, location) { task() }

    fun regionDelayed(location: Location, delayTicks: Long, task: () -> Unit): ScheduledTask =
        Bukkit.getRegionScheduler().runDelayed(plugin, location, { task() }, delayTicks)

    fun regionRepeating(location: Location, initialDelayTicks: Long, periodTicks: Long, task: (ScheduledTask) -> Unit): ScheduledTask =
        Bukkit.getRegionScheduler().runAtFixedRate(plugin, location, { task(it) }, initialDelayTicks, periodTicks)

    // ---- entity -------------------------------------------------------------
    // The optional `retired` runnable fires if the entity is removed (e.g. the
    // player logs out) before the task runs, so you can clean up.

    fun entity(entity: Entity, retired: (() -> Unit)? = null, task: () -> Unit): ScheduledTask? =
        entity.scheduler.run(plugin, { task() }, retired?.let { Runnable(it) })

    fun entityDelayed(entity: Entity, delayTicks: Long, retired: (() -> Unit)? = null, task: () -> Unit): ScheduledTask? =
        entity.scheduler.runDelayed(plugin, { task() }, retired?.let { Runnable(it) }, delayTicks)

    fun entityRepeating(entity: Entity, initialDelayTicks: Long, periodTicks: Long, retired: (() -> Unit)? = null, task: (ScheduledTask) -> Unit): ScheduledTask? =
        entity.scheduler.runAtFixedRate(plugin, { task(it) }, retired?.let { Runnable(it) }, initialDelayTicks, periodTicks)

    // ---- async --------------------------------------------------------------
    // For work that must NOT touch world state: web requests, file/DB I/O, etc.

    fun async(task: () -> Unit): ScheduledTask =
        Bukkit.getAsyncScheduler().runNow(plugin) { task() }

    fun asyncDelayed(delay: Long, unit: TimeUnit = TimeUnit.SECONDS, task: () -> Unit): ScheduledTask =
        Bukkit.getAsyncScheduler().runDelayed(plugin, { task() }, delay, unit)

    fun asyncRepeating(initialDelay: Long, period: Long, unit: TimeUnit = TimeUnit.SECONDS, task: (ScheduledTask) -> Unit): ScheduledTask =
        Bukkit.getAsyncScheduler().runAtFixedRate(plugin, { task(it) }, initialDelay, period, unit)

    // ---- routing helpers ----------------------------------------------------

    /**
     * Run [task] on the region that owns [sender]: a player's own region for a
     * [Player]/[Entity], or the global region for console/command blocks.
     *
     * This is the safe way to talk back to whoever ran a command *after* you've
     * jumped onto an async thread — you can't assume you're still on their
     * region once you've hopped. If the sender is a player who logged out, the
     * task is quietly dropped rather than running on the wrong thread.
     */
    fun forSender(sender: CommandSender, task: () -> Unit) {
        when (sender) {
            is Entity -> entity(sender) { task() } // Player is an Entity
            else -> global { task() }
        }
    }

    // ---- ownership checks ---------------------------------------------------
    // Use these before touching world/entity state to confirm you're actually
    // on the owning region's thread (e.g. inside an event handler you didn't
    // schedule yourself). If false, schedule onto the right region instead.

    /** True if the current thread owns the region containing [location]. */
    fun ownsRegion(location: Location): Boolean = Bukkit.isOwnedByCurrentRegion(location)

    /** True if the current thread owns the region containing [entity]. */
    fun ownsEntity(entity: Entity): Boolean = Bukkit.isOwnedByCurrentRegion(entity)

    companion object {
        /**
         * True if we're running on Folia. Useful if you want a plugin that also
         * loads on plain Paper (where you'd fall back to the Bukkit scheduler).
         */
        val isFolia: Boolean by lazy {
            runCatching { Class.forName("io.papermc.paper.threadedregions.RegionizedServer") }.isSuccess
        }
    }
}
