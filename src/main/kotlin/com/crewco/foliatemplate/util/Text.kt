package com.crewco.foliatemplate.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage

/**
 * One shared [MiniMessage] instance. MiniMessage is thread-safe and designed to
 * be reused, so this is safe to call from any region/async thread.
 *
 * Usage: `mm("<green>Hello <bold>world</bold></green>")`
 * Format reference: https://docs.advntr.dev/minimessage/format.html
 */
private val MM = MiniMessage.miniMessage()

fun mm(text: String): Component = MM.deserialize(text)
