package tech.snaco.split_world

import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.World
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import tech.snaco.split_world.types.WorldConfig
import java.util.*

class Utils(private var worldConfigs: Map<String, WorldConfig>) {
  /* Location Methods */

  fun playerOnCreativeSide(player: Player): Boolean {
    return locationOnCreativeSide(player.location)
  }

  fun locationOnCreativeSide(location: Location): Boolean {
    val worldConfig: WorldConfig = getWorldConfig(location.world)
    return if (worldConfig.creativeSide == "negative" && locationOnNegativeSideOfBuffer(location)) {
      true
    } else worldConfig.creativeSide == "positive" && locationOnPositiveSideOfBuffer(location)
  }

  fun locationOnSurvivalSide(location: Location): Boolean {
    val worldConfig: WorldConfig = getWorldConfig(location.world)
    return if (worldConfig.creativeSide == "negative" && locationOnPositiveSideOfBuffer(location)) {
      true
    } else worldConfig.creativeSide == "positive" && locationOnNegativeSideOfBuffer(location)
  }

  fun playerInBufferZone(player: Player): Boolean {
    return locationInBufferZone(player.location)
  }

  fun locationInBufferZone(location: Location): Boolean {
    val pos = getRelevantPos(location)
    val worldConfig: WorldConfig = getWorldConfig(location.world)
    return (pos >= worldConfig.borderLocation - worldConfig.borderWidth / 2.0 && pos < worldConfig.borderLocation + worldConfig.borderWidth / 2.0)
  }

  private fun locationOnPositiveSideOfBuffer(location: Location): Boolean {
    val worldConfig: WorldConfig = getWorldConfig(location.world)
    val pos = getRelevantPos(location)
    return pos >= worldConfig.borderLocation + worldConfig.borderWidth / 2.0
  }

  private fun locationOnNegativeSideOfBuffer(location: Location): Boolean {
    val worldConfig: WorldConfig = getWorldConfig(location.world)
    val pos = getRelevantPos(location)
    return pos < worldConfig.borderLocation - worldConfig.borderWidth / 2.0
  }

  fun locationIsTraversable(location: Location): Boolean {
    val world = location.world
    val blockType = world.getBlockAt(location).type
    return !blockType.isSolid
  }

  fun getRelevantPos(location: Location): Double {
    val worldConfig: WorldConfig = getWorldConfig(location.world)
    return when (worldConfig.borderAxis?.uppercase(Locale.getDefault())) {
      "Y" -> location.y
      "Z" -> location.z
      else -> location.x
    }
  }

  fun addToRelevantPos(location: Location, value: Double): Location {
    val worldConfig: WorldConfig = getWorldConfig(location.world)
    return when (worldConfig.borderAxis?.uppercase(Locale.getDefault())) {
      "Y" -> location.add(0.0, value, 0.0)
      "Z" -> location.add(0.0, 0.0, value)
      else -> location.add(value, 0.0, 0.0)
    }
  }

  /* Misc. Utils */

  fun getWorldConfig(world: World): WorldConfig {
    return worldConfigs[world.name]!!
  }

  fun worldEnabled(world: World): Boolean {
    val worldName = world.name
    return worldConfigs.containsKey(worldName) && worldConfigs[worldName]!!.enabled
  }

  fun closestSolidBlockBelowLocation(location: Location): Location {
    val loc = location.clone()
    var blockFound = false
    while (!blockFound) {
      loc.add(0.0, -1.0, 0.0)
      if (loc.block.isSolid) {
        blockFound = true
      }
      if (loc.y <= -64) break
    }
    return loc
  }

  fun getSpawnCoordinates(config: FileConfiguration): List<Double> {
    return config.getDoubleList("respawn_coordinates")
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