package tech.snaco.split_world.config

import org.bukkit.GameMode
import org.bukkit.configuration.serialization.ConfigurationSerializable
import java.util.*

class SplitWorldConfig(
  private val map: Map<String, Any>, private val splitServerConfig: SplitServerConfig
) : ConfigurationSerializable {

  fun worldName(): String = map["world_name"] as? String
    ?: error("world_configs entry missing 'world_name'")

  fun defaultGameMode(): GameMode = (map["default_game_mode"] as? String)
    ?.let {
      try {
        GameMode.valueOf(it.uppercase(Locale.ENGLISH))
      } catch (_: IllegalArgumentException) {
        splitServerConfig.defaultGameMode()
      }
    } ?: splitServerConfig.defaultGameMode()

  fun enabled(): Boolean = map["enabled"] as? Boolean ?: false
  fun borderAxis(): String = map["border_position"] as? String ?: "X"
  fun borderLocation(): Double = (map["border_location"] as? Number)?.toDouble() ?: 0.0
  fun creativeSide(): String = map["creative_side"] as? String ?: "negative"
  fun borderWidth(): Double = (map["border_width"] as? Number)?.toDouble() ?: 5.0
  fun replaceBorderBlocks(): Boolean = (map["replace_border_blocks"] as? Boolean) ?: true
  fun noCreativeMonsters(): Boolean = (map["no_creative_monsters"] as? Boolean) ?: true

  @Suppress("UNCHECKED_CAST")
  override fun serialize(): Map<String, Any> = mapOf(
    "world_name" to worldName(),
    "enabled" to enabled(),
    "border_axis" to borderAxis(),
    "border_location" to borderLocation(),
    "creative_side" to creativeSide(),
    "border_width" to borderWidth(),
    "replace_border_blocks" to replaceBorderBlocks(),
    "no_creative_monsters" to noCreativeMonsters(),
    "default_game_mode" to defaultGameMode().name.lowercase(Locale.ENGLISH)
  ) as Map<String, Any>
}