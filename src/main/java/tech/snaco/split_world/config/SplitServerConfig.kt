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
  fun disableAdvancementsInCreative(): Boolean = plugin.config.getBoolean("disable_advancements_in_creative", true)
  fun easterEggsEnabled(): Boolean = plugin.config.getBoolean("enable_easter_eggs", false)
  fun customRespawn(): Boolean = plugin.config.getBoolean("custom_respawn", false)
  fun borderParticles(): Boolean = plugin.config.getBoolean("border_particles", true)
  fun borderBlocks(): Boolean = plugin.config.getBoolean("border_blocks", true)
  fun respawnLocation(): Location {
    val locationCfg = plugin.config.getConfigurationSection("respawn_location")
      ?: error("Missing respawn_location section of split world config")
    locationCfg.contains("x") || error("Missing x coordinate in respawn_location section of split world config")
    locationCfg.contains("y") || error("Missing y coordinate in respawn_location section of split world config")
    locationCfg.contains("z") || error("Missing z coordinate in respawn_location section of split world config")
    val world = Bukkit.getWorld(
      locationCfg.getString("world") ?: error("Missing world name in respawn_location section of split world config")
    ) ?: error("Invalid world name in respawn_location section of split world config")
    val x = locationCfg.getDouble("x")
    val y = locationCfg.getDouble("y")
    val z = locationCfg.getDouble("z")
    val yaw = locationCfg.getDouble("yaw", 0.0)
    val pitch = locationCfg.getDouble("yaw", 0.0)
    return Location(world, x, y, z, yaw.toFloat(), pitch.toFloat())
  }

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

