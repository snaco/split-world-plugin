package tech.snaco.split_world.utils

import org.bukkit.event.Listener
import org.bukkit.plugin.PluginManager
import tech.snaco.split_world.SplitWorldPlugin

fun PluginManager.registerEvents(plugin: SplitWorldPlugin, vararg listeners: Listener) {
  registerEvents(plugin, plugin)
  for (listener in listeners) {
    registerEvents(listener, plugin)
  }
}