package tech.snaco.split_world.utils

import org.bukkit.*
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.Vector
import tech.snaco.split_world.event.AfterGameModeChangeEvent
import tech.snaco.split_world.event.BeforeGameModeChangeEvent
import tech.snaco.split_world.types.ItemStackArrayDataType
import tech.snaco.split_world.types.PotionEffectArrayDataType


fun Player.preventFirstFallDamage() {
  val hadAllowFlight = allowFlight
  // Ensure gliding can be toggled without Elytra
  allowFlight = true
  if (!isOnGround) {
    isGliding = true
  }

  var taskRef: BukkitTask? = null
  taskRef = Bukkit
    .getScheduler()
    .runTaskTimer(splitWorldPlugin(), Runnable {
      // Stop when the player touches the ground or becomes invalid
      if (isOnGround || !isValid || isDead) {
        isGliding = false
        allowFlight = hadAllowFlight
        taskRef?.cancel()
      }
    }, 1L, 1L)
}

fun Player.getInventoryKey(gameMode: GameMode): NamespacedKey {
  return NamespacedKey(splitWorldPlugin(), name + "_" + gameMode.name.lowercase() + "_inv")
}

fun Player.getEnderChestKey(gameMode: GameMode): NamespacedKey {
  return NamespacedKey(splitWorldPlugin(), name + "_" + gameMode.name.lowercase() + "_ender_chest")
}

fun Player.getEffectsKey(gameMode: GameMode): NamespacedKey {
  return NamespacedKey(splitWorldPlugin(), name + "_" + gameMode.name.lowercase() + "_eff")
}

//#region PDC

fun Player.setPdcInt(name: String, value: Int) = setPdcInt(splitWorldPlugin().pdcKey(name), value)

fun Player.setPdcInt(key: NamespacedKey, value: Int) = persistentDataContainer.set(
  key, PersistentDataType.INTEGER, value
)

fun Player.setPdcBoolean(name: String, value: Boolean) = persistentDataContainer.set(
  splitWorldPlugin().pdcKey(name), PersistentDataType.BOOLEAN, value
)

fun Player.getPdcBoolean(name: String): Boolean? =
  persistentDataContainer.get(splitWorldPlugin().pdcKey(name), PersistentDataType.BOOLEAN)

fun Player.getPdcBoolean(name: String, default: Boolean): Boolean = getPdcBoolean(name) ?: default

fun Player.getPdcInt(name: String): Int? =
  persistentDataContainer.get(splitWorldPlugin().pdcKey(name), PersistentDataType.INTEGER)

fun Player.getPdcInt(name: String, default: Int): Int = getPdcInt(name) ?: default

//#endregion

fun Player.peekAtInventory(gameMode: GameMode): Array<ItemStack> =
  persistentDataContainer.get(getInventoryKey(gameMode), ItemStackArrayDataType()) ?: listOf<ItemStack>().toTypedArray()

fun Player.peekAtEnderChest(gameMode: GameMode): Array<ItemStack> =
  persistentDataContainer.get(getEnderChestKey(gameMode), ItemStackArrayDataType())
    ?: listOf<ItemStack>().toTypedArray()

fun Player.peekAtEffects(gameMode: GameMode): List<PotionEffect> =
  (persistentDataContainer.get(getEffectsKey(gameMode), PotionEffectArrayDataType())
    ?: listOf<PotionEffect>().toTypedArray()).toList()

@Suppress("UNCHECKED_CAST")
fun Player.stashMyInventory(gameMode: GameMode) =
  persistentDataContainer.set(
    getInventoryKey(gameMode),
    ItemStackArrayDataType(),
    inventory.contents as Array<ItemStack>
  )

@Suppress("UNCHECKED_CAST")
fun Player.stashMyEnderChest(gameMode: GameMode) =
  persistentDataContainer.set(
    getEnderChestKey(gameMode),
    ItemStackArrayDataType(),
    enderChest.contents as Array<ItemStack>
  )

fun Player.stashMyPotionEffects(gameMode: GameMode) =
  persistentDataContainer.set(getEffectsKey(gameMode), PotionEffectArrayDataType(), activePotionEffects.toTypedArray())

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

fun Player.switchGameModeToWorldDefault() {
  switchGameMode(
    world
      .splitConfig()
      .defaultGameMode()
  )
}

fun Player.switchGameMode(theOneItShouldBe: GameMode) {
  BeforeGameModeChangeEvent(this, theOneItShouldBe).callEvent()
  if (my.gameMode != theOneItShouldBe) {
    andIf(I.shouldHearTheDing) {
      then.I.hearThatDing()
    }
    also(I.shouldAlwaysSeeTheSparkle) {
      so.I.seeThatSparkle()
    }
    then.I.stashMyInventory(_for.my.gameMode)
    I.also.stashMyPotionEffects(_for.my.gameMode)
    and.I.stashMyEnderChest(_for.my.gameMode).too
    after.that
    I.set.my.gameMode = theOneItShouldBe
    inventory.clear()
    enderChest.clear()
    I.clearActivePotionEffects()
    and.I.set.my.inventory.contents = peekAtInventory(theOneItShouldBe)
    and.set.my.enderChest.contents = peekAtEnderChest(theOneItShouldBe)
    and.I.addPotionEffects(peekAtEffects(theOneItShouldBe))
    if (I.am.inAir()) {
      preventFirstFallDamage()
    }
  }
  AfterGameModeChangeEvent(this, theOneItShouldBe).callEvent()
}

private val Unit.too: Any get() = this
val Player.set: Player get() = this
val Player.after: Player get() = this
val Player.am: Player get() = this
val Player.that: Player get() = this
val Player.my: Player get() = this
val Player.I: Player get() = this
val Player.so: Player get() = this
val Player.and: Player get() = this
fun andIf(condition: Boolean, op: () -> Any?) = if (condition) op() else {
}

fun also(condition: Boolean, op: () -> Any?) = if (condition) op() else {
}

val Player.then: Player get() = this
val Player.at: Player get() = this
val Player.also: Player get() = this
val Player._for: Player get() = this

fun Player.switchToConfiguredGameMode() {
  if (splitWorldDisabled) {
    return
  }
  if (!world.isSplit()) {
    switchGameMode(world.defaultGameMode)
    return
  }
  if (location.inBufferZone()) {
    switchGameMode(GameMode.ADVENTURE)
    allowFlight = true
    val wasFlying = isFlying
    val wasGliding = isGliding
    if (wasGliding || wasFlying) {
      isFlying = true
    }
  } else if (location.onCreativeSide()) {
    switchGameMode(GameMode.CREATIVE)
  } else if (location.onDefaultSide()) {
    switchGameMode(world.defaultGameMode)
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

val Player.shouldAlwaysSeeTheSparkle: Boolean get() = true