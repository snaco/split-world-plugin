package tech.snaco.split_world.utils

import org.bukkit.World
import tech.snaco.split_world.config.SplitWorldConfig

fun World.isSplit(): Boolean {
  return splitConfig().enabled
}

fun World.splitConfig(): SplitWorldConfig {
  return splitWorldConfig().worldConfigs[this] ?: SplitWorldConfig(
    mapOf(
      "world_name" to this.name,
      "enabled" to false,
    ),
    splitWorldConfig()
  )
}
