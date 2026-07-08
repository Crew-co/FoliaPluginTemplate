package com.crewco.foliatemplate.pdc

import org.bukkit.NamespacedKey
import org.bukkit.persistence.PersistentDataHolder
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin

/**
 * Typed helpers for the PersistentDataContainer (PDC) — Bukkit's built-in way
 * to attach durable custom data to players, entities, and item metas. Data
 * survives restarts and is stored by the server, so there's no database to run.
 *
 * Folia note: PDC on a live entity (including players) must be read/written on
 * that entity's region thread — do it inside a command/event handler for that
 * entity, or schedule with the entity scheduler. PDC on an ItemMeta is just
 * local object data and is fine anywhere.
 *
 * Keys must be lowercase and match [a-z0-9._-]. Usage:
 * ```
 * player.setString(plugin, "home_world", world.name)
 * val home = player.getString(plugin, "home_world")
 * ```
 */

fun PersistentDataHolder.setString(plugin: Plugin, key: String, value: String) =
    persistentDataContainer.set(NamespacedKey(plugin, key), PersistentDataType.STRING, value)

fun PersistentDataHolder.getString(plugin: Plugin, key: String): String? =
    persistentDataContainer.get(NamespacedKey(plugin, key), PersistentDataType.STRING)

fun PersistentDataHolder.setInt(plugin: Plugin, key: String, value: Int) =
    persistentDataContainer.set(NamespacedKey(plugin, key), PersistentDataType.INTEGER, value)

fun PersistentDataHolder.getInt(plugin: Plugin, key: String): Int? =
    persistentDataContainer.get(NamespacedKey(plugin, key), PersistentDataType.INTEGER)

fun PersistentDataHolder.setBoolean(plugin: Plugin, key: String, value: Boolean) =
    persistentDataContainer.set(NamespacedKey(plugin, key), PersistentDataType.BYTE, if (value) 1 else 0)

fun PersistentDataHolder.getBoolean(plugin: Plugin, key: String): Boolean? =
    persistentDataContainer.get(NamespacedKey(plugin, key), PersistentDataType.BYTE)?.let { it.toInt() != 0 }

fun PersistentDataHolder.hasKey(plugin: Plugin, key: String): Boolean =
    persistentDataContainer.has(NamespacedKey(plugin, key))

fun PersistentDataHolder.removeKey(plugin: Plugin, key: String) =
    persistentDataContainer.remove(NamespacedKey(plugin, key))
