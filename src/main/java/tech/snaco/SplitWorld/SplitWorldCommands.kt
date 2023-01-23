package tech.snaco.SplitWorld

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.persistence.PersistentDataType

class SplitWorldCommands(private var keys: SplitWorldKeys)  {
    fun onTabComplete(sender: CommandSender, command: Command, label: String, args: Array<out String>?): List<String>? {
        if (command.name.equals("play-border-sound", ignoreCase = true) && args!!.size == 1) {
            return listOf("true", "false")
        } else if (command.name.equals("play-border-sound", ignoreCase = true) && args!!.size > 1) {
            return ArrayList()
        }
        return null
    }

    fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>?): Boolean {
        val player_name = sender.name
        val player = sender.server.getPlayer(player_name)
        val player_pdc = player!!.persistentDataContainer

        // border sound toggle
        if (command.name.equals("play-border-sound", ignoreCase = true)) {
            if (args!!.size != 1) {
                return false
            }

            if (args[0].equals("true", ignoreCase = true)) {
                player_pdc.set(keys.playBorderSound, PersistentDataType.INTEGER, 1)
            } else if (args[0].equals("false", ignoreCase = true)) {
                player_pdc.set(keys.playBorderSound, PersistentDataType.INTEGER, 0)
            } else {
                return false
            }
            return true
        }

        //TODO: Finish
        if (command.name.equals("competition-end", ignoreCase = true)) {
            val world_pdc = player.world.persistentDataContainer
            val is_competition_ended = world_pdc.get(keys.competitionEnded, PersistentDataType.INTEGER)
            if (is_competition_ended == null) {
                world_pdc.set(keys.competitionEnded, PersistentDataType.INTEGER, 1)
                player.world.worldBorder.setCenter(0.0, 0.0)
                player.world.worldBorder.setSize(30000.0, 600)
            }
        }

        if (command.name.equals("set-winners", ignoreCase = true)) {
            // TODO: Implement set-winners
        }

        //manage spawn builders

        //manage spawn builders
        if (command.name.equals("set-spawn-builder", ignoreCase = true)) {
            val server = player.server
            if (args!!.size != 2) {
                return false
            }
            val target_player = server.getPlayer(args[0]) ?: return false
            val target_player_pdc = target_player.persistentDataContainer
            if (args[1].equals("true", ignoreCase = true)) {
                target_player_pdc.set(keys.spawnBuilder, PersistentDataType.INTEGER, 1)
                println(target_player.name + "is now a spawn builder.")
                target_player.sendMessage("You now have permission to build in the spawn area.")
            } else if (args[1].equals("false", ignoreCase = true)) {
                target_player_pdc.set(keys.spawnBuilder, PersistentDataType.INTEGER, 0)
                println(target_player.name + "is no longer a spawn builder.")
                target_player.sendMessage("You no longer have permission to build in the spawn area.")
            } else {
                return false
            }
            return true
        }

        //dismiss welcome message permanently

        //dismiss welcome message permanently
        if (command.name.equals("understood", ignoreCase = true)) {
            player_pdc.set(keys.noWelcome, PersistentDataType.INTEGER, 1)
            player.sendMessage("You will no longer see the welcome message for split world.")
            return true
        }

        if (command.name.equals("disable-split-world", ignoreCase = true)) {
            if (player.hasPermission("split-world.disable-split-world")) {
                player_pdc.set(keys.splitWorldDisabled, PersistentDataType.INTEGER, 1)
            }
            return true
        }

        if (command.name.equals("enable-split-world", ignoreCase = true)) {
            if (player.hasPermission("split-world.enable-split-world")) {
                player_pdc.set(keys.splitWorldDisabled, PersistentDataType.INTEGER, 0)
            }
            return true
        }

        return false
    }
}