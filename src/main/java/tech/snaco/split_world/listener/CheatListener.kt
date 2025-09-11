package tech.snaco.split_world.listener

import org.bukkit.entity.Item
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockFromToEvent
import org.bukkit.event.entity.EntityPortalEvent
import org.bukkit.event.entity.ItemSpawnEvent
import org.bukkit.event.player.PlayerFishEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitRunnable
import tech.snaco.split_world.utils.*

class CheatListener(plugin: Plugin) : Listener {
  private val droppedItems = ArrayList<Item>()

  init {
    object : BukkitRunnable() {
      override fun run() {
        if (droppedItems.size.toLong() > 0) {
          val itemsToRemove = ArrayList<Item>()
          for (item in droppedItems) {
            if (item.world.isSplit() && item.location.inBufferZone()) {
              item.remove()
              itemsToRemove.add(item)
            }
          }
          droppedItems.removeAll(itemsToRemove.toSet())
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
  fun onItemSpawn(event: ItemSpawnEvent) {
    if (!event.entity.world.isSplit()) {
      return
    }
    droppedItems.add(event.entity)
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