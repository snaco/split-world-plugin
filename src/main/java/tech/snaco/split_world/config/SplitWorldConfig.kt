package tech.snaco.split_world.config

import org.bukkit.GameMode
import org.bukkit.configuration.serialization.ConfigurationSerializable
import java.util.*

class SplitWorldConfig(
  map: Map<String, Any>, splitServer: SplitServer
) : ConfigurationSerializable {
  val worldName: String = map["world_name"] as? String
    ?: error("world_configs entry missing 'world_name'")
  val defaultGameMode: GameMode = (map["default_game_mode"] as? String)
    ?.let {
      try {
        GameMode.valueOf(it.uppercase(Locale.ENGLISH))
      } catch (_: IllegalArgumentException) {
        splitServer.defaultGameMode
      }
    } ?: splitServer.defaultGameMode
  val enabled: Boolean = map["enabled"] as? Boolean ?: false
  val borderAxis: String = map["border_axis"] as? String ?: "X"
  val borderLocation: Int = (map["border_location"] as? Number)?.toInt() ?: 0
  val creativeSide: String = map["creative_side"] as? String ?: "negative"
  val borderWidth: Int = 5
  val replaceBorderBlocks: Boolean = (map["replace_border_blocks"] as? Boolean) ?: true
  val noCreativeMonsters: Boolean = (map["no_creative_monsters"] as? Boolean) ?: false

  @Suppress("UNCHECKED_CAST")
  override fun serialize(): Map<String, Any> = mapOf(
    "world_name" to worldName,
    "enabled" to enabled,
    "border_axis" to borderAxis,
    "border_location" to borderLocation,
    "creative_side" to creativeSide,
    "border_width" to borderWidth,
    "replace_border_blocks" to replaceBorderBlocks,
    "no_creative_monsters" to noCreativeMonsters,
    "default_game_mode" to defaultGameMode.name.lowercase(Locale.ENGLISH)
  ) as Map<String, Any>
}