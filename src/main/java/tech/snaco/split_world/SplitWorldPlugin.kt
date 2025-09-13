package tech.snaco.split_world

import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.plugin.java.JavaPlugin
import tech.snaco.split_world.listener.*
import tech.snaco.split_world.utils.loadSplitWorld
import tech.snaco.split_world.utils.registerEvents
import tech.snaco.split_world.utils.unloadSplitWorld

@Suppress("UNCHECKED_CAST")
class SplitWorldPlugin : JavaPlugin(), Listener {
  private lateinit var commandHandler: SplitWorldCommands

  override fun onDisable() {
    server.unloadSplitWorld()
  }

  override fun reloadConfig() {
    super.reloadConfig()
  }

  override fun onEnable() {
    server.loadSplitWorld(this)
    commandHandler = SplitWorldCommands()
    Bukkit
      .getPluginManager()
      .registerEvents(
        this,
        SpawnListener(),
        XpModListener(),
        MonsterListener(),
        BufferZoneListener(),
        EasterEggListener(this),
        CheatListener(this),
        GameModeListener()
      )
  }

  override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
    return commandHandler.onCommand(sender, command, args)
  }

  override fun onTabComplete(
    sender: CommandSender, command: Command, label: String, args: Array<String>
  ): List<String>? {
    return commandHandler.onTabComplete(command, args)
  }

  @EventHandler
  fun preProcessCommand(event: PlayerCommandPreprocessEvent) {
    commandHandler.preProcessCommand(event)
  }

  fun pdcKey(name: String): NamespacedKey {
    return NamespacedKey(this, name)
  }
}