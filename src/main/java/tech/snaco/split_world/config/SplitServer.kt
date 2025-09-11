package tech.snaco.split_world.config

import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.World
import tech.snaco.split_world.SplitWorldKeys
import tech.snaco.split_world.SplitWorldPlugin

/** Root config loaded from config.yml in a type-safe way. */
@Suppress("UNCHECKED_CAST")
class SplitServer(plugin: SplitWorldPlugin) {
  val defaultGameMode: GameMode = plugin.config
    .getString("default_game_mode")
    ?.let {
      try {
        GameMode.valueOf(it)
      } catch (_: IllegalArgumentException) {
        GameMode.SURVIVAL
      }
    } ?: GameMode.SURVIVAL
  val disableWelcomeMessage: Boolean = plugin.config.getBoolean("disable_welcome_message", false)
  val xpModEnabled: Boolean = plugin.config.getBoolean("enable_xp_mod", false)
  val xpLossPercentage: Double = plugin.config.getDouble("xp_loss_percentage", 25.0)
  val manageCreativeCommands: Boolean = plugin.config.getBoolean("manage_creative_commands", true)
  val creativeCommands: List<String> =
    plugin.config
      .getStringList("creative_commands")
      .ifEmpty { listOf("fill", "setblock", "clone") }
  val easterEggsEnabled: Boolean = plugin.config.getBoolean("enable_easter_eggs", false)
  val customRespawn: Boolean = plugin.config.getBoolean("custom_respawn", false)
  val respawnWorld: World = plugin.config
    .getString("respawn_world")
    ?.let { Bukkit.getWorld(it) }
    ?: plugin.server.worlds[0]
  val respawnLocation: Location = plugin.config
    .getObject("respawn_location", Location::class.java)
    ?.let { Location(respawnWorld, it.x, it.y, it.z, it.pitch, it.yaw) }
    ?: Location(respawnWorld, 0.0, 0.0, 0.0)
  val worldConfigs: Map<World, SplitWorldConfig> = plugin.config
    .getMapList("world_configs")
    .mapNotNull { it as? Map<String, Any> }
    .map { SplitWorldConfig(it, this) }
    .mapNotNull { cfg ->
      Bukkit
        .getWorld(cfg.worldName)
        ?.let { world -> world to cfg }
    }
    .toMap()
  val keys = SplitWorldKeys(plugin)

  init {
    plugin.saveDefaultConfig()
  }
}

