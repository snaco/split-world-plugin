package tech.snaco.split_world.listener

import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockEvent
import org.bukkit.event.block.BlockPlaceEvent
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
  fun onPlayerMove(event: PlayerMoveEvent) {
    if (event.player.world.isSplit() && !event.player.splitDisabled()) {
      event.player.convertBufferZoneBlocks()
      if (event.player.location.inBufferZone()) {
        event.player.inventory.clear()
        if (!event.to.isTraversable()) {
          event.isCancelled = true
        }
      }
    }
  }

  @EventHandler
  fun onPickup(event: PlayerAttemptPickupItemEvent) {
    if (event.player.world.isSplit() && event.player.location.inBufferZone()) {
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
    if (event.block.location.inBufferZone()) {
      event.isCancelled = true
    }
    if (event.block.location.onDefaultSide() && player.location.inBufferZone()) {
      event.isCancelled = true
    }
  }
}