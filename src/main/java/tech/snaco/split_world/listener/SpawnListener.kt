package tech.snaco.split_world.listener

import net.kyori.adventure.text.Component
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.spigotmc.event.player.PlayerSpawnLocationEvent
import tech.snaco.split_world.utils.*

class SpawnListener : Listener {

  @EventHandler
  fun onPlayerJoin(event: PlayerJoinEvent) {
    if (splitWorldConfig().disableWelcomeMessage()) {
      return
    }
    val noWelcome = event.player.getPdcInt(splitWorldConfig().keys.noWelcome)
    val worldConfig = event.player.world.splitConfig()
    if (noWelcome == null) {
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

  @EventHandler
  fun onPlayerRespawn(event: PlayerRespawnEvent) {
    if (splitWorldConfig().customRespawn() && !event.isAnchorSpawn && !event.isBedSpawn) {
      event.respawnLocation = splitWorldConfig().respawnLocation()
    }
  }

  @EventHandler
  fun onSpawn(event: PlayerSpawnLocationEvent) {
    event.player.switchGameMode(
      event.player.world
        .splitConfig()
        .defaultGameMode()
    )
    if (splitWorldConfig().customRespawn() && event.player.getPdcInt(splitWorldConfig().keys.firstJoin) != 1) {
      event.player.setPdcInt(splitWorldConfig().keys.firstJoin, 1)
      event.spawnLocation = splitWorldConfig().respawnLocation()
    }
  }
}