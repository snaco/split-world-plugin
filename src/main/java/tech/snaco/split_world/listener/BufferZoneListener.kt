package tech.snaco.split_world.listener

import io.papermc.paper.event.packet.PlayerChunkLoadEvent
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityRegainHealthEvent
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerAttemptPickupItemEvent
import org.bukkit.event.player.PlayerMoveEvent
import tech.snaco.split_world.utils.*

class BufferZoneListener : Listener {

  @EventHandler
  fun onDeath(event: PlayerDeathEvent) {
    if (event.player.world.isSplit() && event.player.location.inBufferZone()) {
      event.player.health = 0.1
      event.isCancelled = true
    }
  }

  @EventHandler
  fun onHealthRegain(event: EntityRegainHealthEvent) {
    if (event.entity.world.isSplit() && event.entity.location.inBufferZone()) {
      event.isCancelled = true
    }
  }

  @EventHandler
  fun onChunkLoad(event: PlayerChunkLoadEvent) {
    if (event.world.isSplit()) {
      for (y in event.world.minHeight..<event.world.maxHeight) {
        for (x in 0..15) {
          for (z in 0..15) {
            val block = event.chunk.getBlock(x, y, z)
            if (block.type.isAir || block.type == Material.LAVA || block.type == Material.WATER || block.type == Material.SNOW) {
              continue
            }
            if (block.inBufferZone) {
              block.type = Material.COPPER_BLOCK
            }
          }
        }
      }
    }
  }

  @EventHandler
  fun onPlayerMove(event: PlayerMoveEvent) {
    print(event.player.location.getRelevantPos())
    if (event.player.world.isSplit() && !event.player.splitDisabled()) {
      if (event.player.location.inBufferZone()) {
        if (!event.to.isTraversable()) {
          event.isCancelled = true
        }
      }
    }
  }

  @EventHandler
  fun onPickup(event: PlayerAttemptPickupItemEvent) {
    if (event.player.location.onDifferentSide(event.item.location) || event.player.location.inBufferZone()) {
      event.isCancelled = true
    }
  }

  @EventHandler
  fun playerHunger(event: FoodLevelChangeEvent) {
    if (event.entity.world.isSplit() && event.entity is Player && event.entity.location.inBufferZone()) {
      event.isCancelled = true
    }
  }

  @EventHandler
  fun playerDamage(event: EntityDamageByEntityEvent) {
    if (event.entity.world.isSplit() && event.entity is Player && event.entity.location.inBufferZone()) {
      event.isCancelled = true
    }
  }

  @EventHandler
  fun onPlace(event: BlockPlaceEvent) {
    handleBlockEvent(event, event.player)
  }

  @EventHandler
  fun onBreak(event: BlockBreakEvent) {
    handleBlockEvent(event, event.player)
  }

  fun <T> handleBlockEvent(event: T, player: Player) where T : BlockEvent, T : Cancellable {
    if (!event.block.world.isSplit()) {
      return
    }
    if (event.block.inBufferZone && !player.location.inBufferZone()) {
      event.isCancelled = true
    }
    if (player.location.onCreativeSide() && !event.block.onCreativeSide) {
      event.isCancelled = true
    }
    if (player.location.onDefaultSide() && !event.block.onDefaultSide) {
      event.isCancelled = true
    }
  }
}