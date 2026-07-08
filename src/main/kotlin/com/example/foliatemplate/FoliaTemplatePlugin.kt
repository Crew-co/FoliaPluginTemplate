package com.example.foliatemplate

import com.example.foliatemplate.command.CommandManager
import com.example.foliatemplate.commands.ExampleCommand
import com.example.foliatemplate.config.Config
import com.example.foliatemplate.config.Messages
import com.example.foliatemplate.gui.MenuListener
import com.example.foliatemplate.listeners.ExampleListener
import com.example.foliatemplate.scheduler.Schedulers
import com.example.foliatemplate.update.UpdateChecker
import com.example.foliatemplate.util.Cooldowns
import org.bstats.bukkit.Metrics
import org.bukkit.plugin.java.JavaPlugin

/**
 * Entry point. Folia loads this because the generated plugin.yml declares
 * `folia-supported: true` (set via `foliaSupported = true` in build.gradle.kts).
 *
 * Keep onEnable light: create helpers, load config, register commands/listeners.
 * There is no main thread on Folia — for timed work touching the world, use
 * [schedulers] rather than the old Bukkit scheduler.
 */
class FoliaTemplatePlugin : JavaPlugin() {

    lateinit var schedulers: Schedulers
        private set
    lateinit var commands: CommandManager
        private set
    lateinit var config: Config
        private set
    lateinit var messages: Messages
        private set

    /** Thread-safe per-player cooldowns, shared across the plugin. */
    val cooldowns = Cooldowns()

    override fun onEnable() {
        schedulers = Schedulers(this)
        commands = CommandManager(this)

        // Copy bundled defaults on first run, then load them into memory.
        saveResource("config.yml", false)
        saveResource("messages.yml", false)
        config = Config(this).also { it.loadNow() }
        messages = Messages(this).also { it.loadNow() }

        // Commands (annotation-driven) + event listeners.
        commands.register(ExampleCommand(this))
        server.pluginManager.registerEvents(MenuListener(), this)   // powers all GUIs
        server.pluginManager.registerEvents(ExampleListener(this), this)

        // Anonymous usage metrics. Set BSTATS_PLUGIN_ID to your id from
        // https://bstats.org/getting-started (it stays disabled while 0).
        if (BSTATS_PLUGIN_ID != 0) Metrics(this, BSTATS_PLUGIN_ID)

        // Optional, async, read-only update check (honors the config toggle).
        if (config.current.updateCheck) {
            UpdateChecker(this, schedulers, GITHUB_REPO).checkAsync()
        }

        logger.info("Enabled ${pluginMeta.name} v${pluginMeta.version} (Folia: ${Schedulers.isFolia})")
    }

    override fun onDisable() {
        logger.info("Disabled. Folia cancels this plugin's scheduled tasks automatically.")
    }

    private companion object {
        const val BSTATS_PLUGIN_ID = 0                  // TODO: your bStats id
        const val GITHUB_REPO = "your-name/your-repo"   // TODO: "owner/repo" for update checks
    }
}
