package tech.snaco.split_world.listener

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent
import org.bukkit.GameMode
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.event.player.PlayerTeleportEvent
import tech.snaco.split_world.utils.inAir
import tech.snaco.split_world.utils.switchToConfiguredGameMode
import tech.snaco.split_world.utils.warpToGround

class GameModeListener : Listener {

  @EventHandler(priority = EventPriority.HIGHEST)
  fun onTeleport(event: PlayerTeleportEvent) {
    event.player.switchToConfiguredGameMode()
    if (event.player.inAir() && event.player.gameMode == GameMode.SURVIVAL) {
      event.player.warpToGround()
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  fun onPlayerMove(event: PlayerMoveEvent) {
    event.player.switchToConfiguredGameMode()
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  fun onPlayerWorldChange(event: PlayerChangedWorldEvent) {
    event.player.switchToConfiguredGameMode()
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  fun postRespawn(event: PlayerPostRespawnEvent) {
    event.player.switchToConfiguredGameMode()
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  fun onPlayerRespawn(event: PlayerRespawnEvent) {
    event.player.switchToConfiguredGameMode()
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  fun onPlayerJoin(event: PlayerRespawnEvent) {
    event.player.switchToConfiguredGameMode()
  }
}