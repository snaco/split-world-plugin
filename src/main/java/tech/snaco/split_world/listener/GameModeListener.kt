package tech.snaco.split_world.listener

import org.bukkit.GameMode
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.event.player.PlayerTeleportEvent
import tech.snaco.split_world.utils.*

class GameModeListener : Listener {

  @EventHandler
  fun onTeleport(event: PlayerTeleportEvent) {
    val destination = event.to
    val player = event.player
    if (!destination.world.isSplit()) {
      player.switchGameMode(
        destination.world
          .splitConfig()
          .defaultGameMode()
      )
      return
    }
    if (destination.inBufferZone()) {
      player.switchGameMode(GameMode.ADVENTURE)
      return
    } else if (destination.onCreativeSide()) {
      player.switchGameMode(GameMode.CREATIVE)
      return
    }
    val needsWarp = player.gameMode != GameMode.SURVIVAL
    player.switchGameMode(GameMode.SURVIVAL)
    if (needsWarp) {
      player.warpToGround()
    }
  }

  @EventHandler
  fun onPlayerMove(event: PlayerMoveEvent) {
    if (event.player.splitDisabled()) {
      return
    }
//    if (event.player.world.isSplit()) {
//      // handle transitioning to survival safely
//      if (event.player.gameMode != GameMode.SURVIVAL
//        && event.to.onDefaultSide()
//        && event.player.location.onDefaultSide()
//      ) {
//        // temporarily load survival inv to check equip status
//        event.player.loadInventory()
//        val playerHasElytraEquipped = event.player.inventory.chestplate?.type == Material.ELYTRA
//        event.player.inventory.clear()
//        event.player.enderChest.contents
//        if (playerHasElytraEquipped && event.player.location.block.type == Material.AIR) {
//          event.player.isGliding = true
//        }
//        if (!playerHasElytraEquipped && event.player.location.onDefaultSide() && event.player.inAir()) {
//          event.player.velocity = Vector(0, 0, 0)
//          event.player.warpToGround()
//        }
//      }
//    }
    event.player.switchToConfiguredGameMode()
  }

  @EventHandler
  fun onPlayerWorldChange(event: PlayerChangedWorldEvent) {
    if (event.player.world.isSplit()) {
      event.player.switchToConfiguredGameMode()
    }
  }

  @EventHandler
  fun onPlayerRespawn(event: PlayerRespawnEvent) {
    event.player.switchToConfiguredGameMode()
  }
}