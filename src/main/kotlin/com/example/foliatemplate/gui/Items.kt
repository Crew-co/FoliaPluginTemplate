package com.example.foliatemplate.gui

import com.example.foliatemplate.util.mm
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

/**
 * Builds a menu icon with a MiniMessage display name and lore lines.
 *
 * Minecraft italicises custom item names/lore by default; that's turned off
 * here so menus look clean. Example:
 * ```
 * icon(Material.LIME_DYE, "<green>Confirm", "<gray>Click to accept")
 * ```
 */
fun icon(material: Material, name: String? = null, vararg lore: String): ItemStack {
    val stack = ItemStack(material)
    stack.editMeta { meta ->
        if (name != null) meta.displayName(mm(name).noItalic())
        if (lore.isNotEmpty()) meta.lore(lore.map { mm(it).noItalic() })
    }
    return stack
}

private fun Component.noItalic(): Component = decoration(TextDecoration.ITALIC, false)
