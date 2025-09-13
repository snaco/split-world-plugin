package tech.snaco.split_world.utils

import org.bukkit.*
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.util.Vector
import tech.snaco.split_world.SplitWorldPlugin
import tech.snaco.split_world.types.ItemStackArrayDataType
import tech.snaco.split_world.types.PotionEffectArrayDataType

private fun getPlugin(): SplitWorldPlugin {
  return Bukkit
    .getPluginManager()
    .getPlugin("SplitWorld") as? SplitWorldPlugin ?: error("SplitWorld plugin not loaded!")
}

fun Player.splitDisabled(): Boolean {
  return getPdcInt(splitWorldConfig().keys.splitWorldDisabled) == 1
}

fun Player.getInventoryKey(gameMode: GameMode): NamespacedKey {
  return NamespacedKey(getPlugin(), name + "_" + gameMode.name.lowercase() + "_inv")
}

fun Player.getInventoryKey(): NamespacedKey {
  return getInventoryKey(gameMode)
}

fun Player.getEnderChestKey(gameMode: GameMode): NamespacedKey {
  return NamespacedKey(getPlugin(), name + "_" + gameMode.name.lowercase() + "_ender_chest")
}

fun Player.getEnderChestKey(): NamespacedKey {
  return getEnderChestKey(gameMode)
}

fun Player.getEffectsKey(): NamespacedKey {
  return getEffectsKey(gameMode)
}

fun Player.getEffectsKey(gameMode: GameMode): NamespacedKey {
  return NamespacedKey(getPlugin(), name + "_" + gameMode.name.lowercase() + "_eff")
}

//#region PDC

fun Player.setPdcInt(name: String, value: Int) = setPdcInt(getPlugin().pdcKey(name), value)

fun Player.setPdcInt(key: NamespacedKey, value: Int) = persistentDataContainer.set(
  key, PersistentDataType.INTEGER, value
)

fun Player.setPdcBoolean(name: String, value: Boolean) = persistentDataContainer.set(
  getPlugin().pdcKey(name), PersistentDataType.BOOLEAN, value
)

fun Player.getPdcBoolean(name: String): Boolean? =
  persistentDataContainer.get(getPlugin().pdcKey(name), PersistentDataType.BOOLEAN)

fun Player.getPdcBoolean(name: String, default: Boolean): Boolean = getPdcBoolean(name) ?: default

fun Player.getPdcInt(name: String): Int? =
  persistentDataContainer.get(getPlugin().pdcKey(name), PersistentDataType.INTEGER)

fun Player.getPdcInt(name: String, default: Int): Int = getPdcInt(name) ?: default

fun Player.getPdcInt(key: NamespacedKey): Int? {
  return persistentDataContainer.get(key, PersistentDataType.INTEGER)
}

//#endregion

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


fun Player.peekAtInventory(gameMode: GameMode): Array<ItemStack> =
  persistentDataContainer.get(getInventoryKey(gameMode), ItemStackArrayDataType()) ?: listOf<ItemStack>().toTypedArray()

fun Player.peekAtEnderChest(gameMode: GameMode): Array<ItemStack> =
  persistentDataContainer.get(getEnderChestKey(gameMode), ItemStackArrayDataType())
    ?: listOf<ItemStack>().toTypedArray()

fun Player.peekAtEffects(gameMode: GameMode): Array<PotionEffect> =
  persistentDataContainer.get(getEffectsKey(gameMode), PotionEffectArrayDataType())
    ?: listOf<PotionEffect>().toTypedArray()

fun Player.stashMyInventory(gameMode: GameMode) =
  persistentDataContainer.set(
    getInventoryKey(gameMode),
    ItemStackArrayDataType(),
    inventory.contents
      .filterNotNull()
      .toTypedArray()
  )

fun Player.stashMyEnderChest(gameMode: GameMode) =
  persistentDataContainer.set(
    getEnderChestKey(gameMode),
    ItemStackArrayDataType(),
    enderChest.contents
      .filterNotNull()
      .toTypedArray()
  )

fun Player.stashMyPotionEffects(gameMode: GameMode) =
  persistentDataContainer.set(getEffectsKey(gameMode), PotionEffectArrayDataType(), activePotionEffects.toTypedArray())


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

fun Player.facingVector(): Vector {
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

fun Player.seeThatSparkle() =
  I.spawnParticle(
    Particle.DRAGON_BREATH,
    location
      .clone()
      .add(facingVector()),
    20
  )

fun Player.hearThatDing() =
  I.playSound(at.my.location, Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f)

fun Player.switchGameMode(toTheOneItShouldBe: GameMode) {
  if (my.gameMode != toTheOneItShouldBe) {
    and_if(I.shouldHearTheDing) { ->
      then.I.hearThatDing()
    }
    and_if(I.also.shouldSeeTheSparkle) { ->
      then.I.seeThatSparkle()
    }
    also
    if (I.shouldSeeTheSparkle) {
      then.I.seeThatSparkle()
    }
    then.I.stashMyInventory(_for.my.gameMode)
    I.also.stashMyPotionEffects(_for.my.gameMode)
    and.I.stashMyEnderChest(_for.my.gameMode).too
    after.that
    I.set.my.gameMode = toTheOneItShouldBe
    inventory.clear()
    enderChest.clear()
    activePotionEffects.clear()
    and.set.my.inventory.contents = peekAtInventory(toTheOneItShouldBe)
    and.set.my.enderChest.contents = peekAtEnderChest(toTheOneItShouldBe)
    activePotionEffects.addAll(peekAtEffects(toTheOneItShouldBe))
  }
}

private val Unit.too: Any get() = this
val Player.set: Player get() = this
val Player.after: Player get() = this
val Player.that: Player get() = this
val Player.my: Player get() = this
val Player.I: Player get() = this
val Player.and: Player get() = this
fun Player.and_if(condition: Boolean, op: () -> Any?) = if (condition) op() else {
}

val Player.then: Player get() = this
val Player.at: Player get() = this
val Player.also: Player get() = this
val Player._for: Player get() = this
val Player.finally: Player get() = this
val Player.now: Player get() = this
val Player.but: Player get() = this


fun Player.switchToConfiguredGameMode() {
  if (splitWorldDisabled) {
    return
  }
  if (!world.isSplit()) {
    switchGameMode(
      world
        .splitConfig()
        .defaultGameMode()
    )
    return
  }
  if (location.inBufferZone()) {
    val wasFlying = isFlying
    val wasGliding = isGliding
    switchGameMode(GameMode.ADVENTURE)
    allowFlight = true
    if (wasGliding || wasFlying) {
      isFlying = true
    }

    //keep player health and hunger static while in the buffer, no healing or dying here
    foodLevel = foodLevel
    health = health
    // creative side
  } else if (location.onCreativeSide()) {
    switchGameMode(GameMode.CREATIVE)
    // survival side
  } else if (location.onDefaultSide()) {
    switchGameMode(
      world
        .splitConfig()
        .defaultGameMode()
    )
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


var Player.welcomeMessageDisabled: Boolean
  get() = getPdcBoolean("no_welcome_message", false)
  set(value) = setPdcBoolean("no_welcome_message", value)

var Player.splitWorldDisabled: Boolean
  get() = getPdcBoolean("split_world_disabled", false)
  set(value) = setPdcBoolean("split_world_disabled", value)

var Player.firstJoin: Boolean
  get() = getPdcBoolean("first_join", true)
  set(value) = setPdcBoolean("first_join", value)

var Player.firstFishAttempt: Boolean
  get() = getPdcBoolean("first_fish_attempt", true)
  set(value) = setPdcBoolean("first_fish_attempt", value)

var Player.spawnBuilder: Boolean
  get() = getPdcBoolean("spawn_builder", false)
  set(value) = setPdcBoolean("spawn_builder", value)

var Player.shouldHearTheDing: Boolean
  get() = getPdcBoolean("play_border_sound", true)
  set(value) = setPdcBoolean("play_border_sound", value)

var Player.sleepInNetherScore: Int
  get() = getPdcInt("sleep_in_nether_score", 0)
  set(value) = setPdcInt("sleep_in_nether_score", value)

var Player.netherEgg: Int
  get() = getPdcInt("nether_egg_score", 0)
  set(value) = setPdcInt("nether_egg_score", value)

var Player.netherSleepThrottle: Int
  get() = getPdcInt("nether_sleep_throttle", 100)
  set(value) = setPdcInt("nether_sleep_throttle", value)

var Player.netherSleepTock: Int
  get() = getPdcInt("nether_sleep_tock", 1)
  set(value) = setPdcInt("nether_sleep_tock", value)

val Player.shouldSeeTheSparkle: Boolean get() = true