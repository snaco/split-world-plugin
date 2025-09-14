package tech.snaco.split_world.listener

import net.kyori.adventure.text.Component
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.spigotmc.event.player.PlayerSpawnLocationEvent
import tech.snaco.split_world.utils.firstJoin
import tech.snaco.split_world.utils.splitConfig
import tech.snaco.split_world.utils.splitWorldPlugin
import tech.snaco.split_world.utils.welcomeMessageDisabled

class SpawnListener : Listener {

  @EventHandler
  fun onPlayerJoin(event: PlayerJoinEvent) {
    if (splitWorldPlugin().splitServerConfig.disableWelcomeMessage()) {
      return
    }

    val worldConfig = event.player.world.splitConfig()
    if (!event.player.welcomeMessageDisabled) {
      event.player.sendMessage(
        Component.text(
          """
                Hello ${event.player.name}! 
                This is a split world! That means half of the world is creative, and half is survival.
                There is a border at ${worldConfig.borderAxis()}=${worldConfig.borderLocation()}.
                Creative is on the ${worldConfig.creativeSide()} side of the border.
                You now have two inventories, one for each side of the border, which will be saved and restored automatically when you cross.
                Have fun! (You can disable this message from showing using /understood) 
            """.trimIndent()
        )
      )
    }
  }

  @EventHandler(priority = EventPriority.LOWEST)
  fun onPlayerRespawn(event: PlayerRespawnEvent) {
    if (splitWorldPlugin().splitServerConfig.customRespawn() && !event.isAnchorSpawn && !event.isBedSpawn) {
      event.respawnLocation = splitWorldPlugin().splitServerConfig.respawnLocation()
    }
  }

  @EventHandler(priority = EventPriority.LOWEST)
  fun onSpawn(event: PlayerSpawnLocationEvent) {
    if (splitWorldPlugin().splitServerConfig.customRespawn() && event.player.firstJoin) {
      event.player.firstJoin = false
      event.spawnLocation = splitWorldPlugin().splitServerConfig.respawnLocation()
    }
  }
}