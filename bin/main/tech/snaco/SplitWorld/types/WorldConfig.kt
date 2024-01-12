package tech.snaco.SplitWorld.types

import org.bukkit.configuration.serialization.ConfigurationSerializable

class WorldConfig(map: Map<String, Any>) : ConfigurationSerializable {
    @JvmField
    var world_name: String
    @JvmField
    var enabled: Boolean
    @JvmField
    var border_axis: String? = null
    @JvmField
    var border_location = 0
    @JvmField
    var creative_side: String? = null
    @JvmField
    var border_width = 0
    @JvmField
    var replace_border_blocks = false
    @JvmField
    var no_creative_monsters = false

    init {
        world_name = map["world_name"] as String
        enabled = map["enabled"] as Boolean
        if (enabled) {
            border_axis = map["border_axis"] as String?
            border_location = map["border_location"] as Int
            creative_side = map["creative_side"] as String?
            border_width = map["border_width"] as Int
            replace_border_blocks = map["replace_border_blocks"] as Boolean
            no_creative_monsters = map["no_creative_monsters"] as Boolean
        }
    }

    override fun serialize(): Map<String, Any> {
        return java.util.Map.of<String, Any>(
                "world_name", world_name,
                "enabled", enabled,
                "border_axis", border_axis,
                "border_location", border_location,
                "creative_side", creative_side,
                "border_width", border_width,
                "replace_border_blocks", replace_border_blocks,
                "no_creative_monsters", no_creative_monsters
        )
    }
}