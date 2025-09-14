package tech.snaco.split_world.utils

import org.bukkit.Bukkit
import tech.snaco.split_world.SplitWorldPlugin

fun splitWorldPlugin(): SplitWorldPlugin {
  return Bukkit
    .getPluginManager()
    .getPlugin("SplitWorld") as? SplitWorldPlugin ?: error("SplitWorld plugin not loaded!")
}

