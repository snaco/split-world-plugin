package tech.snaco.SplitWorld

import org.bukkit.*
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.util.Vector
import tech.snaco.SplitWorld.types.ItemStackArrayDataType
import tech.snaco.SplitWorld.types.PotionEffectArrayDataType
import tech.snaco.SplitWorld.types.WorldConfig

@Suppress("UNCHECKED_CAST")
class PlayerUtils(private var utils: Utils, private var keys: SplitWorldKeys, private var default_game_mode: GameMode) {
    /* Player Management */
    fun switchPlayerToConfiguredGameMode(player: Player) {
        // keep players in the default mode when disabled for the world
        if (!utils.worldEnabled(player.world)) {
            switchPlayerGameMode(player, default_game_mode)
            return
        }
        // set to spectator for buffer zone
        if (utils.playerInBufferZone(player)) {
            val set_flying = player.isGliding || player.isFlying
            switchPlayerGameMode(player, GameMode.ADVENTURE)
            player.allowFlight = true
            if (set_flying) {
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

    fun switchPlayerGameMode(player: Player, game_mode: GameMode) {
        val player_inv = player.inventory
        val player_pdc = player.persistentDataContainer
        if (player.gameMode != game_mode) {
            val play_sound = player_pdc.get(keys.play_border_sound, PersistentDataType.INTEGER)
            if (play_sound == null || play_sound == 1) {
                player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_COW_BELL, 1.0f, 0.5f)
            }
            val y = 1.5
            // south -Z
            // north +Z
            // east +X
            // west -X
            val vector = when (player.facing) {
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
            player.world.spawnParticle(Particle.DRAGON_BREATH, player.location.clone().add(vector), 20)
            savePlayerInventory(player)
            player_inv.clear()
            player.gameMode = game_mode
            loadPlayerInventory(player, game_mode)
        }
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun savePlayerInventory(player: Player) {
        val player_pdc = player.persistentDataContainer
        val inv_key: NamespacedKey = keys.getPlayerInventoryKey(player, player.gameMode)
        val eff_key: NamespacedKey = keys.getPlayerEffectsKey(player, player.gameMode)
        player_pdc.set(inv_key, ItemStackArrayDataType(), player.inventory.contents as Array<ItemStack>)
        player_pdc.set(eff_key, PotionEffectArrayDataType(), player.activePotionEffects.toTypedArray())
    }

    fun loadPlayerInventory(player: Player, game_mode: GameMode) {
        val inv_key: NamespacedKey = keys.getPlayerInventoryKey(player, game_mode)
        val eff_key: NamespacedKey = keys.getPlayerEffectsKey(player, game_mode)
        val player_pdc = player.persistentDataContainer
        val new_inv = player_pdc.get(inv_key, ItemStackArrayDataType())
        val effects = player_pdc.get(eff_key, PotionEffectArrayDataType())
        if (new_inv != null) {
            player.inventory.contents = new_inv
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
        val world_config: WorldConfig = utils.getWorldConfig(world)
        val player_location = player.location.clone()
        for (i in world_config.border_location - world_config.border_width / 2 until world_config.border_location + world_config.border_width / 2) {
            for (j in -5..4) {
                for (y in -64..318) {
                    val loc = player_location.clone()
                    loc.y = y.toDouble()
                    if (world_config.border_axis == "X") {
                        loc.x = i.toDouble()
                        loc.z = loc.z + j
                    } else {
                        loc.z = i.toDouble()
                        loc.x = loc.x + j
                    }
                    val block_type = world.getBlockAt(loc).type
                    if (block_type != Material.AIR && block_type != Material.WATER && block_type != Material.LAVA) {
                        world.getBlockAt(loc).type = Material.BEDROCK
                    } else if (block_type == Material.WATER || block_type == Material.LAVA) {
                        world.getBlockAt(loc).type = Material.AIR
                    }
                }
            }
        }
    }

    fun warpPlayerToGround(player: Player, to_location: Location) {
        val location = to_location.clone()
        val top = player.world.getHighestBlockAt(location.blockX, location.blockZ)
        val pitch = location.pitch
        val yaw = location.yaw
        val destination = top.location.add(0.0, 1.0, 0.0)
        destination.pitch = pitch
        destination.yaw = yaw
        player.teleport(destination)
    }
}