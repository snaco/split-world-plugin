package tech.snaco.split_world.config

import org.bukkit.*
import tech.snaco.split_world.SplitWorldPlugin

/** Root config loaded from config.yml in a type-safe way. */
@Suppress("UNCHECKED_CAST")
class SplitServerConfig(val plugin: SplitWorldPlugin) {
  fun defaultGameMode(): GameMode = plugin.config
    .getString("default_game_mode")
    ?.let {
      try {
        GameMode.valueOf(it)
      } catch (_: IllegalArgumentException) {
        GameMode.SURVIVAL
      }
    } ?: GameMode.SURVIVAL

  fun disableWelcomeMessage(): Boolean = plugin.config.getBoolean("disable_welcome_message", false)
  fun xpModEnabled(): Boolean = plugin.config.getBoolean("enable_xp_mod", false)
  fun xpLossPercentage(): Double = plugin.config.getDouble("xp_loss_percentage", 25.0)
  fun manageCreativeCommands(): Boolean = plugin.config.getBoolean("manage_creative_commands", true)
  fun creativeCommands(): List<String> =
    plugin.config
      .getStringList("creative_commands")
      .ifEmpty { listOf("fill", "setblock", "clone") }

  fun easterEggsEnabled(): Boolean = plugin.config.getBoolean("enable_easter_eggs", false)
  fun customRespawn(): Boolean = plugin.config.getBoolean("custom_respawn", false)
  fun respawnWorld(): World = plugin.config
    .getString("respawn_world")
    ?.let { Bukkit.getWorld(it) }
    ?: plugin.server.worlds[0]

  fun respawnLocation(): Location = plugin.config
    .getObject("respawn_location", Location::class.java)
    ?.let { Location(respawnWorld(), it.x, it.y, it.z, it.yaw, it.pitch) }
    ?: Location(respawnWorld(), 0.0, 0.0, 0.0)

  fun worldConfigs(): Map<World, SplitWorldConfig> = plugin.config
    .getMapList("world_configs")
    .mapNotNull { it as? Map<String, Any> }
    .map { SplitWorldConfig(it, this) }
    .mapNotNull { cfg ->
      Bukkit
        .getWorld(cfg.worldName())
        ?.let { world -> world to cfg }
    }
    .toMap()

  init {
    plugin.saveDefaultConfig()
    if (Bukkit.getWorld("split_world") == null) {
      Bukkit.createWorld(WorldCreator("split_world"))
    }
    if (Bukkit.getWorld("creative_world") == null) {
      Bukkit.createWorld(WorldCreator("creative_world"))
    }
  }
}

