package tech.snaco.split_world.listener

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockFromToEvent
import org.bukkit.event.block.BlockPistonExtendEvent
import org.bukkit.event.block.BlockPistonRetractEvent
import org.bukkit.event.entity.EntityPortalEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerFishEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.vehicle.VehicleMoveEvent
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitRunnable
import tech.snaco.split_world.types.DroppedItem
import tech.snaco.split_world.utils.*

class CheatListener(plugin: Plugin) : Listener {
  private val droppedItems = ArrayList<DroppedItem>()

  init {
    object : BukkitRunnable() {
      override fun run() {
        if (droppedItems.size.toLong() > 0) {
          val droppedItemsToStopTracking = ArrayList<DroppedItem>()
          for (droppedItem in droppedItems) {
            if (droppedItem.item.world.isSplit() && droppedItem.item.location.inBufferZone() && !droppedItem.previousLocation.inBufferZone()) {
              droppedItem.cheater.sendMessage("Stop that")
              droppedItem.cheater.inventory.addItem(droppedItem.item.itemStack)
              droppedItemsToStopTracking.add(droppedItem)
              droppedItem.item.remove()
            } else {
              droppedItem.previousLocation = droppedItem.item.location
            }
          }
          droppedItems.removeAll(droppedItemsToStopTracking.toSet())
        }
      }
    }.runTaskTimer(plugin, 0, 1L)
  }

  @EventHandler
  fun onPistonExtend(event: BlockPistonExtendEvent) {
    if (!event.block.world.isSplit()) {
      return
    }

    val dir = event.direction
    for (moved in event.blocks) {
      val fromLoc = moved.location
      val toLoc = moved.getRelative(dir).location

      if (toLoc.inBufferZone() || toLoc.onDifferentSide(fromLoc)) {
        event.isCancelled = true
        return
      }
    }
  }

  @EventHandler
  fun onPistonRetract(event: BlockPistonRetractEvent) {
    if (!event.block.world.isSplit()) {
      return
    }

    // During retraction, blocks move toward the piston (opposite of the facing)
    val dir = event.direction.oppositeFace
    for (moved in event.blocks) {
      val fromLoc = moved.location
      val toLoc = moved.getRelative(dir).location

      if (toLoc.inBufferZone() || toLoc.onDifferentSide(fromLoc)) {
        event.isCancelled = true
        return
      }
    }
  }

  @EventHandler
  fun onBlockFromTo(event: BlockFromToEvent) {
    if (!event.block.world.isSplit()) {
      return
    }
    if (event.toBlock.location.inBufferZone()) {
      event.isCancelled = true
    }
  }

  @EventHandler
  fun onPlayerInteract(event: PlayerInteractEvent) {
    if (!event.player.world.isSplit()) {
      return
    }
    if (event.clickedBlock != null && event.clickedBlock!!.onDifferentSide(event.player.location)) {
      event.isCancelled = true
    }
    if (event.interactionPoint != null && event.interactionPoint!!.onDifferentSide(event.player.location)) {
      event.isCancelled = true
    }
  }

  @EventHandler
  fun onInventoryOpen(event: InventoryOpenEvent) {
    if (!event.player.world.isSplit()) {
      return
    }
    if (event.inventory.location != null) {
      if (event.player.location.onDifferentSide(event.inventory.location!!)) {
        event.isCancelled = true
      }
    }
    if (event.player.location.inBufferZone()) {
      event.isCancelled = true
    }
  }

  @EventHandler
  fun onPlayerDrop(event: PlayerDropItemEvent) {
    if (!event.player.world.isSplit()) {
      return
    }
    if (event.player.location.inBufferZone()) {
      event.isCancelled = true
      return
    }
    droppedItems.add(
      DroppedItem(
        event.itemDrop,
        event.itemDrop.location,
        event.player.gameMode,
        cheater = event.player,
      )
    )
  }

  @EventHandler
  fun onEntityPortal(event: EntityPortalEvent) {
    if (event.from.world.isSplit() && event.from.onCreativeSide()) {
      event.isCancelled = true
    }
  }

  @EventHandler
  fun onPlayerFish(event: PlayerFishEvent) {
    if (!event.player.world.isSplit()) {
      return
    }

    // no fishing creative stuff to survival side
    val caught = event.caught ?: return
    if (!caught.location.onDefaultSide() && !event.player.location.onCreativeSide()) {
      event.isCancelled = true
    }

    //snark
    if (event.player.firstFishAttempt) {
      event.player.giveExp(100)
      event.player.firstFishAttempt = false
    }
    event.player.sendMessage("Nice try.")
  }

  /**
   * Prevents vehicles from moving across the buffer zone.
   */
  @EventHandler
  fun onVehicleMove(event: VehicleMoveEvent) {
    if (!event.vehicle.world.isSplit()) {
      return
    }
    if (event.to.inBufferZone(1.0)) {
      val config = event.vehicle.world.splitConfig()
      val sideMod = when (event.from.onDefaultSide()) {
        true -> config.defaultSideModifier()
        false -> config.creativeSideModifier()
      }
      val tpLoc = event.from.clone().setRelevantPos((config.borderWidth() + 2) * sideMod)
      event.vehicle.passengers.forEach { passenger -> passenger.teleport(tpLoc) }
      event.vehicle.teleport(tpLoc)
    }
  }

  @EventHandler
  fun onPlayerMove(event: PlayerMoveEvent) {
    if (!event.player.world.isSplit()) {
      return
    }
    if (event.to.inBufferZone(1.0)) {
      event.player.eject()
      event.player.vehicle?.eject()
      val config = event.player.world.splitConfig()
      val sideMod = when (event.from.onDefaultSide()) {
        true -> config.defaultSideModifier()
        false -> config.creativeSideModifier()
      }
      val tpLoc = event.from.clone().setRelevantPos((config.borderWidth() + 2) * sideMod)
      event.player.vehicle?.teleport(tpLoc)
    }
  }
}
