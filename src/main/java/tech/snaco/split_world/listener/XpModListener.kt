package tech.snaco.split_world.listener

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import tech.snaco.split_world.utils.splitWorldConfig

class XpModListener : Listener {
  @EventHandler
  fun onDeath(event: PlayerDeathEvent) {
    if (splitWorldConfig().xpModEnabled) {
      event.droppedExp =
        event.player.totalExperience - (event.player.totalExperience * (splitWorldConfig().xpLossPercentage / 100)).toInt()
    }
  }
}