package com.crewco.foliatemplate.config

import com.crewco.foliatemplate.scheduler.Schedulers
import com.crewco.foliatemplate.util.mm
import net.kyori.adventure.text.Component
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.util.concurrent.atomic.AtomicReference

/**
 * Loads user-facing strings from messages.yml so text/colors live outside code
 * and can be translated or re-styled without recompiling.
 *
 * Same snapshot pattern as [Config]: an immutable map behind an
 * [AtomicReference], parsed off-thread on reload. Values are MiniMessage with
 * `{placeholder}` substitution.
 */
class Messages(private val plugin: JavaPlugin) {

    private val ref = AtomicReference<Map<String, String>>(emptyMap())

    fun loadNow() = ref.set(read())

    fun reloadAsync(schedulers: Schedulers, done: () -> Unit = {}) {
        schedulers.async {
            ref.set(read())
            done()
        }
    }

    /**
     * Resolve [key] to a Component, replacing `{name}` placeholders from
     * [placeholders]. Missing keys render visibly rather than throwing, so a
     * typo is obvious in-game instead of crashing a command.
     */
    fun component(key: String, vararg placeholders: Pair<String, String>): Component {
        var raw = ref.get()[key] ?: return mm("<red>[missing message: $key]")
        for ((name, value) in placeholders) raw = raw.replace("{$name}", value)
        return mm(raw)
    }

    private fun read(): Map<String, String> {
        val yaml = YamlConfiguration.loadConfiguration(File(plugin.dataFolder, "messages.yml"))
        return yaml.getKeys(true)
            .filter { yaml.isString(it) }
            .associateWith { yaml.getString(it).orEmpty() }
    }
}
