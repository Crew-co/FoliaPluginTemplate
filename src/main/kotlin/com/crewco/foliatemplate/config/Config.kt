package com.crewco.foliatemplate.config

import com.crewco.foliatemplate.scheduler.Schedulers
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.util.concurrent.atomic.AtomicReference

/**
 * Type-safe, hot-reloadable config.
 *
 * Folia angle: region threads must never block on disk, and shared mutable
 * config is a data race waiting to happen. So the parsed values live in an
 * immutable [Settings] snapshot behind an [AtomicReference]; readers just call
 * [current] (a cheap volatile read), and [reloadAsync] parses the file on a
 * background thread and swaps the whole snapshot in one atomic write.
 */
class Config(private val plugin: JavaPlugin) {

    /** An immutable view of the config. Copy-on-reload; safe to read anywhere. */
    data class Settings(
        val welcomeEnabled: Boolean,
        val explosionPower: Float,
        val boomCooldownSeconds: Long,
        val updateCheck: Boolean,
    )

    private val ref = AtomicReference(DEFAULT)

    /** The current settings snapshot. Safe to read from any thread. */
    val current: Settings get() = ref.get()

    /** Blocking load — call once from onEnable (startup, before regions tick). */
    fun loadNow() = ref.set(read())

    /** Reload off-thread, then swap in the new snapshot and invoke [done]. */
    fun reloadAsync(schedulers: Schedulers, done: (Settings) -> Unit = {}) {
        schedulers.async {
            val settings = read()
            ref.set(settings)
            done(settings)
        }
    }

    private fun read(): Settings {
        val yaml = YamlConfiguration.loadConfiguration(File(plugin.dataFolder, "config.yml"))
        return Settings(
            welcomeEnabled = yaml.getBoolean("welcome.enabled", DEFAULT.welcomeEnabled),
            explosionPower = yaml.getDouble("example.explosion-power", DEFAULT.explosionPower.toDouble()).toFloat(),
            boomCooldownSeconds = yaml.getLong("example.cooldown-seconds", DEFAULT.boomCooldownSeconds),
            updateCheck = yaml.getBoolean("update-check", DEFAULT.updateCheck),
        )
    }

    companion object {
        /** Used until the first successful load, and as per-key fallbacks. */
        val DEFAULT = Settings(
            welcomeEnabled = true,
            explosionPower = 0f,
            boomCooldownSeconds = 3,
            updateCheck = true,
        )
    }
}
