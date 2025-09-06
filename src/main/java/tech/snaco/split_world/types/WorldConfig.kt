package tech.snaco.split_world.types

import org.bukkit.configuration.serialization.ConfigurationSerializable

class WorldConfig(map: Map<String, Any>) : ConfigurationSerializable {
  @JvmField
  var worldName: String = map["world_name"] as String

  @JvmField
  var enabled: Boolean = map["enabled"] as Boolean

  @JvmField
  var borderAxis: String? = null

  @JvmField
  var borderLocation = 0

  @JvmField
  var creativeSide: String? = null

  @JvmField
  var borderWidth = 0

  @JvmField
  var replaceBorderBlocks = false

  @JvmField
  var noCreativeMonsters = false

  init {
    if (enabled) {
      borderAxis = map["border_axis"] as String?
      borderLocation = map["border_location"] as Int
      creativeSide = map["creative_side"] as String?
      borderWidth = map["border_width"] as Int
      replaceBorderBlocks = map["replace_border_blocks"] as Boolean
      noCreativeMonsters = map["no_creative_monsters"] as Boolean
    }
  }

  override fun serialize(): Map<String, Any> {
    return java.util.Map.of<String, Any>(
      "world_name",
      worldName,
      "enabled",
      enabled,
      "border_axis",
      borderAxis,
      "border_location",
      borderLocation,
      "creative_side",
      creativeSide,
      "border_width",
      borderWidth,
      "replace_border_blocks",
      replaceBorderBlocks,
      "no_creative_monsters",
      noCreativeMonsters
    )
  }
}