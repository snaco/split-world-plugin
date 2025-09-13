package tech.snaco.split_world.listener

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockFromToEvent
import org.bukkit.event.entity.EntityPortalEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerFishEvent
import org.bukkit.persistence.PersistentDataType
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
            if (droppedItem.item.world.isSplit()
              && droppedItem.item.location.inBufferZone()
              && !droppedItem.previousLocation.inBufferZone()
            ) {
              droppedItem.item.remove()
              droppedItem.cheater.sendMessage("Stop that")
              droppedItem.item.velocity = droppedItem.item.velocity.multiply(-2.0)
              droppedItem.cheater.inventory.addItem(droppedItem.item.itemStack)
              droppedItemsToStopTracking.add(droppedItem)
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
  fun onBlockFromTo(event: BlockFromToEvent) {
    if (!event.block.world.isSplit()) {
      return
    }
    if (event.toBlock.location.inBufferZone()) {
      event.isCancelled = true
    }
  }

  @EventHandler
  fun onPlayerDrop(event: PlayerDropItemEvent) {
    if (!event.player.world.isSplit()) {
      return
    }
//    if (!event.player.location.onNegativeSideOfBuffer() && !event.player.location.onPositiveSideOfBuffer()) {
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
    val playerPdc = event.player.persistentDataContainer
    val firstAttempt = playerPdc.get(splitWorldConfig().keys.firstFishAttempt, PersistentDataType.INTEGER)
    if (firstAttempt == null) {
      event.player.giveExp(100)
      playerPdc.set(splitWorldConfig().keys.firstFishAttempt, PersistentDataType.INTEGER, 1)
    }
    event.player.sendMessage("Nice try.")
  }
}
