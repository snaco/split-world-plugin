package tech.snaco.split_world.config

import org.bukkit.GameMode
import org.bukkit.configuration.serialization.ConfigurationSerializable
import java.util.*

class SplitWorldConfig(
  private val map: Map<String, Any>, private val splitServerConfig: SplitServerConfig
) : ConfigurationSerializable {

  fun worldName(): String = map["world_name"] as? String ?: error("world_configs entry missing 'world_name'")

  fun defaultGameMode(): GameMode = (map["default_game_mode"] as? String)?.let {
      try {
        GameMode.valueOf(it.uppercase(Locale.ENGLISH))
      } catch (_: IllegalArgumentException) {
        splitServerConfig.defaultGameMode()
      }
    } ?: splitServerConfig.defaultGameMode()

  fun enabled(): Boolean = map["enabled"] as? Boolean ?: false
  fun borderAxis(): String = map["border_axis"] as? String ?: "X"
  fun borderLocation(): Double = (map["border_location"] as? Number)?.toDouble() ?: 0.0
  fun creativeSide(): String = map["creative_side"] as? String ?: "negative"
  fun borderWidth(): Double = (map["border_width"] as? Number)?.toDouble() ?: 5.0
  fun noCreativeMonsters(): Boolean = (map["no_creative_monsters"] as? Boolean) ?: true

  /** gets a -1 or 1 used to multiply abs values to transform them to the creative side */
  fun creativeSideModifier(): Double = if (creativeSide() == "negative") -1.0 else 1.0

  /** gets a -1 or 1 used to multiply abs values to transform them to the default side */
  fun defaultSideModifier(): Double = -creativeSideModifier()

  @Suppress("UNCHECKED_CAST")
  override fun serialize(): Map<String, Any> = mapOf(
    "world_name" to worldName(),
    "enabled" to enabled(),
    "border_axis" to borderAxis(),
    "border_location" to borderLocation(),
    "creative_side" to creativeSide(),
    "border_width" to borderWidth(),
    "no_creative_monsters" to noCreativeMonsters(),
    "default_game_mode" to defaultGameMode().name.lowercase(Locale.ENGLISH)
  ) as Map<String, Any>
}