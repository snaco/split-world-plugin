package tech.snaco.split_world.utils

import org.bukkit.World
import tech.snaco.split_world.config.SplitWorldConfig

fun World.isSplit(): Boolean {
  return splitConfig().enabled()
}

fun World.splitConfig(): SplitWorldConfig {
  return splitWorldPlugin().splitServerConfig.worldConfigs()[this] ?: SplitWorldConfig(
    mapOf(
      "world_name" to this.name,
      "enabled" to false,
    ),
    splitWorldPlugin().splitServerConfig
  )
}

val World.defaultGameMode get() = splitConfig().defaultGameMode()
