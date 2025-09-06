package tech.snaco.split_world

import org.bukkit.*
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.util.Vector
import tech.snaco.split_world.types.ItemStackArrayDataType
import tech.snaco.split_world.types.PotionEffectArrayDataType
import tech.snaco.split_world.types.WorldConfig

@Suppress("UNCHECKED_CAST")
class PlayerUtils(private var utils: Utils, private var keys: SplitWorldKeys, private var defaultGameMode: GameMode) {
  /* Player Management */
  fun switchPlayerToConfiguredGameMode(player: Player) {
    // keep players in the default mode when disabled for the world
    if (!utils.worldEnabled(player.world)) {
      switchPlayerGameMode(player, defaultGameMode)
      return
    }
    // set to spectator for buffer zone
    if (utils.playerInBufferZone(player)) {
      val setFlying = player.isGliding || player.isFlying
      switchPlayerGameMode(player, GameMode.ADVENTURE)
      player.allowFlight = true
      if (setFlying) {
        player.isFlying = true
      }

      //keep player health and hunger static while in the border, ie no healing or dying here
      player.foodLevel = player.foodLevel
      player.health = player.health
      // creative side
    } else if (utils.playerOnCreativeSide(player)) {
      switchPlayerGameMode(player, GameMode.CREATIVE)
      // survival side
    } else {
      switchPlayerGameMode(player, GameMode.SURVIVAL)
    }
  }

  fun switchPlayerGameMode(player: Player, gameMode: GameMode) {
    val playerInv = player.inventory
    val playerPdc = player.persistentDataContainer
    if (player.gameMode != gameMode) {
      val playSound = playerPdc.get(keys.playBorderSound, PersistentDataType.INTEGER)
      if (playSound == null || playSound == 1) {
        player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f)
      }
      val facingVector = getFacingVector(player)
      player.world.spawnParticle(
        Particle.DRAGON_BREATH,
        player.location
          .clone()
          .add(facingVector),
        20
      )
      savePlayerInventory(player)
      playerInv.clear()
      player.gameMode = gameMode
      loadPlayerInventory(player)
    }
  }

  fun savePlayerInventory(player: Player) {
    val playerPdc = player.persistentDataContainer
    val invKey: NamespacedKey = keys.getPlayerInventoryKey(player)
    val effKey: NamespacedKey = keys.getPlayerEffectsKey(player)
    playerPdc.set(invKey, ItemStackArrayDataType(), player.inventory.contents as Array<ItemStack>)
    playerPdc.set(effKey, PotionEffectArrayDataType(), player.activePotionEffects.toTypedArray())
  }

  fun loadPlayerInventory(player: Player) {
    val invKey: NamespacedKey = keys.getPlayerInventoryKey(player)
    val effKey: NamespacedKey = keys.getPlayerEffectsKey(player)
    val playerPdc = player.persistentDataContainer
    val newInv = playerPdc.get(invKey, ItemStackArrayDataType())
    val effects = playerPdc.get(effKey, PotionEffectArrayDataType())
    if (newInv != null) {
      player.inventory.contents = newInv
    }
    if (effects != null) {
      for (effect in player.activePotionEffects) {
        player.removePotionEffect(effect.type)
      }
      for (effect in effects) {
        player.addPotionEffect(effect)
      }
    }
  }

  fun convertBufferZoneBlocksAroundPlayer(player: Player) {
    val world = player.world
    val worldConfig: WorldConfig = utils.getWorldConfig(world)
    val playerLocation = player.location.clone()
    for (i in worldConfig.borderLocation - worldConfig.borderWidth / 2 until worldConfig.borderLocation + worldConfig.borderWidth / 2) {
      for (j in -5..4) {
        for (y in -64..318) {
          val loc = playerLocation.clone()
          loc.y = y.toDouble()
          if (worldConfig.borderAxis == "X") {
            loc.x = i.toDouble()
            loc.z = loc.z + j
          } else {
            loc.z = i.toDouble()
            loc.x = loc.x + j
          }
          val blockType = world.getBlockAt(loc).type
          if (blockType != Material.AIR && blockType != Material.WATER && blockType != Material.LAVA) {
            world.getBlockAt(loc).type = Material.BEDROCK
          } else if (blockType == Material.WATER || blockType == Material.LAVA) {
            world.getBlockAt(loc).type = Material.AIR
          }
        }
      }
    }
  }

  fun warpPlayerToGround(player: Player, toLocation: Location) {
    val location = toLocation.clone()
    val top = player.world.getHighestBlockAt(location.blockX, location.blockZ)
    val pitch = location.pitch
    val yaw = location.yaw
    val destination =
      if (location.block.type == Material.AIR && location.add(0.0, 1.0, 0.0).block.type == Material.AIR) {
        utils
          .closestSolidBlockBelowLocation(utils.addToRelevantPos(location, 0.5))
          .add(0.0, 1.0, 0.0)
      } else {
        utils
          .addToRelevantPos(top.location, 0.5)
          .add(0.0, 1.0, 0.0)
      }
    destination.pitch = pitch
    destination.yaw = yaw
    player.teleport(destination)
  }

  private fun getFacingVector(player: Player): Vector {
    val y = 1.5
    // south -Z
    // north +Z
    // east +X
    // west -X
    return when (player.facing) {
      BlockFace.NORTH -> Vector(0.0, y, -1.0)
      BlockFace.SOUTH -> Vector(0.0, y, 1.0)
      BlockFace.EAST -> Vector(1.0, y, 0.0)
      BlockFace.WEST -> Vector(-1.0, y, 0.0)
      BlockFace.UP -> Vector(0.0, y + 1, 0.0)
      BlockFace.DOWN -> Vector(0.0, y - 1, 0.0)
      BlockFace.NORTH_EAST -> Vector(1.0, y, -1.0)
      BlockFace.NORTH_WEST -> Vector(-1.0, y, -1.0)
      BlockFace.SOUTH_EAST -> Vector(1.0, y, 1.0)
      BlockFace.SOUTH_WEST -> Vector(-1.0, y, 1.0)
      BlockFace.WEST_NORTH_WEST -> Vector(-1.0, y, -0.5)
      BlockFace.NORTH_NORTH_WEST -> Vector(-0.5, y, -1.0)
      BlockFace.NORTH_NORTH_EAST -> Vector(0.5, y, -1.0)
      BlockFace.EAST_NORTH_EAST -> Vector(1.0, y, -0.5)
      BlockFace.EAST_SOUTH_EAST -> Vector(1.0, y, 0.5)
      BlockFace.SOUTH_SOUTH_EAST -> Vector(0.5, y, 1.0)
      BlockFace.SOUTH_SOUTH_WEST -> Vector(-0.5, y, 1.0)
      BlockFace.WEST_SOUTH_WEST -> Vector(-1.0, y, 0.5)
      BlockFace.SELF -> Vector(0, 0, 0)
    }
  }

  fun playerInAir(player: Player): Boolean {
    val underFeet = player.location
      .clone()
      .add(0.0, -1.0, 0.0)
    return underFeet.block.type == Material.AIR && player.location.block.type == Material.AIR
  }
}