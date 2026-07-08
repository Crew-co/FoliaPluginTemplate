package com.crewco.foliatemplate

import com.crewco.core.command.CommandManager
import com.crewco.foliatemplate.commands.ExampleCommand
import com.crewco.foliatemplate.config.Config
import com.crewco.foliatemplate.config.Messages
import com.crewco.foliatemplate.gui.MenuListener
import com.crewco.foliatemplate.listeners.ExampleListener
import com.crewco.foliatemplate.scheduler.Schedulers
import com.crewco.foliatemplate.update.UpdateChecker
import com.crewco.foliatemplate.util.Cooldowns
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
        const val GITHUB_REPO = "Crew-co/FoliaPluginTemplate"   // TODO: "owner/repo" for update checks
    }
}
