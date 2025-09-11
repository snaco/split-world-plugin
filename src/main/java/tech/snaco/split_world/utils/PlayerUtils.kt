package tech.snaco.split_world.utils

import org.bukkit.*
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.util.Vector
import tech.snaco.split_world.SplitWorldPlugin
import tech.snaco.split_world.types.ItemStackArrayDataType
import tech.snaco.split_world.types.PotionEffectArrayDataType

private fun getPlugin(): SplitWorldPlugin {
  return Bukkit
    .getPluginManager()
    .getPlugin("SplitWorld") as? SplitWorldPlugin ?: error("SplitWorld plugin not loaded!")
}

fun Player.getInventoryKey(): NamespacedKey {
  return NamespacedKey(getPlugin(), name + "_" + gameMode.name.lowercase() + "_inv")
}

fun Player.getEnderChestKey(): NamespacedKey {
  return NamespacedKey(getPlugin(), name + "_" + gameMode.name.lowercase() + "_ender_chest")
}

fun Player.getEffectsKey(): NamespacedKey {
  return NamespacedKey(getPlugin(), name + "_" + gameMode.name.lowercase() + "_eff")
}

fun Player.setPdcInt(key: NamespacedKey, value: Int) {
  persistentDataContainer.set(key, PersistentDataType.INTEGER, value)
}

fun Player.getPdcInt(key: NamespacedKey): Int? {
  return persistentDataContainer.get(key, PersistentDataType.INTEGER)
}

fun Player.saveInventory() {
  // save inventory contents
  @Suppress("UNCHECKED_CAST") persistentDataContainer.set(
    getInventoryKey(), ItemStackArrayDataType(), inventory.contents as Array<ItemStack>
  )
  // save potion effects
  persistentDataContainer.set(
    getEffectsKey(), PotionEffectArrayDataType(), activePotionEffects.toTypedArray()
  )
  // save ender chest contents
  @Suppress("UNCHECKED_CAST") persistentDataContainer.set(
    getEnderChestKey(), ItemStackArrayDataType(), enderChest.contents as Array<ItemStack>
  )
}

fun Player.loadInventory() {
  // load inventory contents
  val inventoryContents = persistentDataContainer.get(getInventoryKey(), ItemStackArrayDataType())
  if (inventoryContents != null) {
    inventory.contents = inventoryContents
  }

  // load potion effects
  val effects = persistentDataContainer.get(getEffectsKey(), PotionEffectArrayDataType())
  if (effects != null) {
    for (effect in activePotionEffects) {
      removePotionEffect(effect.type)
    }
    for (effect in effects) {
      addPotionEffect(effect)
    }
  }

  //load ender chest contents
  val enderChestContents = persistentDataContainer.get(getEnderChestKey(), ItemStackArrayDataType())
  if (enderChestContents != null) {
    enderChest.contents = enderChestContents
  }
}

fun Player.getFacingVector(): Vector {
  val y = 1.5
  // south -Z
  // north +Z
  // east +X
  // west -X
  return when (facing) {
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

fun Player.switchGameMode(gameMode: GameMode) {
  if (this.gameMode != gameMode) {
    val shouldPlaySound = getPdcInt(splitWorldConfig().keys.playBorderSound)
    if (shouldPlaySound == null || shouldPlaySound == 1) {
      playSound(location, Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f)
    }
    val facingVector = getFacingVector()
    world.spawnParticle(
      Particle.DRAGON_BREATH,
      location
        .clone()
        .add(facingVector),
      20
    )
    saveInventory()
    inventory.clear()
    this.gameMode = gameMode
    loadInventory()
  }
}

fun Player.switchToConfiguredGameMode() {
  if (!this.world.isSplit()) {
    switchGameMode(world.splitConfig().defaultGameMode)
    return
  }
  // set to spectator for buffer zone
  if (location.inBufferZone()) {
    switchGameMode(GameMode.ADVENTURE)
    allowFlight = true
    if (isGliding || isFlying) {
      isFlying = true
    }

    //keep player health and hunger static while in the border, ie no healing or dying here
    foodLevel = foodLevel
    health = health
    // creative side
  } else if (location.onCreativeSide()) {
    switchGameMode(GameMode.CREATIVE)
    // survival side
  } else {
    switchGameMode(GameMode.SURVIVAL)
  }
}

fun Player.convertBufferZoneBlocks() {
  val playerLocation = location.clone()
  val config = splitWorldConfig()
  val start = world.splitConfig().borderLocation - world.splitConfig().borderWidth / 2
  val end = world.splitConfig().borderLocation + world.splitConfig().borderWidth / 2
  for (i in start until end) {
    for (j in -5..4) {
      for (y in -64..318) {
        val loc = playerLocation.clone()
        loc.y = y.toDouble()
        if (world.splitConfig().borderAxis == "X") {
          loc.x = i.toDouble()
          loc.z += j
        } else {
          loc.z = i.toDouble()
          loc.x += j
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

fun Player.inAir(): Boolean {
  val underFeet = location
    .clone()
    .add(0.0, -1.0, 0.0)
  return underFeet.block.type == Material.AIR && location.block.type == Material.AIR
}

fun Player.warpToGround() {
  val location = location.clone()
  val top = world.getHighestBlockAt(location.blockX, location.blockZ)
  val pitch = location.pitch
  val yaw = location.yaw
  val destination = if (location.block.type == Material.AIR && location.add(0.0, 1.0, 0.0).block.type == Material.AIR) {
    location
      .addAcrossSplitAxis(0.5)
      .closestSolidBlockBelow()
      .add(0.0, 1.0, 0.0)
  } else {
    top.location
      .addAcrossSplitAxis(0.5)
      .add(0.0, 1.0, 0.0)
  }
  destination.pitch = pitch
  destination.yaw = yaw
  teleport(destination)
}