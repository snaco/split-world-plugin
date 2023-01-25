package tech.snaco.SplitWorld

import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import tech.snaco.SplitWorld.types.WorldConfig
import java.util.*

class Utils(private var world_configs: Map<String, WorldConfig>) {
    /* Location Methods */

    fun playerOnCreativeSide(player: Player): Boolean {
        return locationOnCreativeSide(player.location)
    }

    fun locationOnCreativeSide(location: Location): Boolean {
        val world_config: WorldConfig = getWorldConfig(location.world)
        return if (world_config.creative_side == "negative" && locationOnNegativeSideOfBuffer(location)) {
            true
        } else world_config.creative_side == "positive" && locationOnPositiveSideOfBuffer(location)
    }

    fun locationOnSurvivalSide(location: Location): Boolean {
        val world_config: WorldConfig = getWorldConfig(location.world)
        return if (world_config.creative_side == "negative" && locationOnPositiveSideOfBuffer(location)) {
            true
        } else world_config.creative_side == "positive" && locationOnNegativeSideOfBuffer(location)
    }

    fun playerInBufferZone(player: Player): Boolean {
        return locationInBufferZone(player.location)
    }

    fun locationInBufferZone(location: Location): Boolean {
        val pos = getRelevantPos(location)
        val world_config: WorldConfig = getWorldConfig(location.world)
        return (pos >= world_config.border_location - world_config.border_width / 2.0
                && pos < world_config.border_location + world_config.border_width / 2.0)
    }

    private fun locationOnPositiveSideOfBuffer(location: Location): Boolean {
        val world_config: WorldConfig = getWorldConfig(location.world)
        val pos = getRelevantPos(location)
        return pos >= world_config.border_location + world_config.border_width / 2.0
    }

    private fun locationOnNegativeSideOfBuffer(location: Location): Boolean {
        val world_config: WorldConfig = getWorldConfig(location.world)
        val pos = getRelevantPos(location)
        return pos < world_config.border_location - world_config.border_width / 2.0
    }

    fun locationIsTraversable(location: Location): Boolean {
        val world = location.world
        val block_type = world.getBlockAt(location).type
        return !block_type.isSolid
    }

    fun getRelevantPos(location: Location): Double {
        val world_config: WorldConfig = getWorldConfig(location.world)
        return when (world_config.border_axis?.uppercase(Locale.getDefault())) {
            "Y" -> location.y
            "Z" -> location.z
            else -> location.x
        }
    }

    fun addToRelevantPos(location: Location, value: Double): Location {
        val world_config: WorldConfig = getWorldConfig(location.world)
        return when (world_config.border_axis?.uppercase(Locale.getDefault())) {
            "Y" -> location.add(0.0, value, 0.0)
            "Z" -> location.add(0.0, 0.0, value)
            else -> location.add(value, 0.0, 0.0)
        }
    }

    /* Misc. Utils */

    fun getWorldConfig(world: World): WorldConfig {
        return world_configs[world.name]!!
    }

    fun worldEnabled(world: World): Boolean {
        val world_name = world.name
        return world_configs.containsKey(world_name) && world_configs[world_name]!!.enabled
    }

    fun closestSolidBlockBelowLocation(location: Location): Location {
        val loc = location.clone()
        var block_found = false
        while (!block_found) {
            loc.add(0.0, -1.0, 0.0)
            if (loc.block.isSolid) {
                block_found = true
            }
            if (loc.y <= -64) break
        }
        return loc
    }

    companion object {
        fun setPdcInt(player: Player, key: NamespacedKey, value: Int) {
            player.persistentDataContainer.set(key, PersistentDataType.INTEGER, value)
        }

        fun getPdcInt(player: Player, key: NamespacedKey): Int? {
            return player.persistentDataContainer.get(key, PersistentDataType.INTEGER)
        }
    }
}