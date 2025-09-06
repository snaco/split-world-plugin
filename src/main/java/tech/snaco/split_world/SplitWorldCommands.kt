package tech.snaco.split_world

import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.persistence.PersistentDataType
import tech.snaco.split_world.types.WorldConfig

class SplitWorldCommands(private var keys: SplitWorldKeys,
                         private var playerUtils: PlayerUtils,
                         worldConfigs: Map<String, WorldConfig>,
                         private var manageCreativeCommands: Boolean)  {

    private var utils = Utils(worldConfigs)
    private var numberOfWorldsEnabled = worldConfigs.values.stream().filter { world: WorldConfig? ->
      world?.enabled == true
    }.toList().size

    fun onTabComplete(command: Command, args: Array<out String>?): List<String>? {
        if (command.name.equals("play-border-sound", ignoreCase = true) && args!!.size == 1) {
            return listOf("true", "false")
        } else if (command.name.equals("play-border-sound", ignoreCase = true) && args!!.size > 1) {
            return ArrayList()
        }
        return null
    }

    fun preProcessCommand(event: PlayerCommandPreprocessEvent) {
        if (!manageCreativeCommands || event.player.isOp) {
            return
        }

        // creative commands allowed in the creative zones, these will be blocked in survival mode
        val creativeCommands = listOf("/fill", "/clone", "/setblock")
        val player = event.player
        val commandStr = event.message
        val commandArgs = commandStr.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (commandArgs.size < 3) {
            return
        }

        // if not in managed creative commands, return to allow normal server handling of the event.
        if (!creativeCommands.contains(commandArgs[0])) {
            return
        }

        // block creative command in survival
        if (player.gameMode == GameMode.SURVIVAL) {
            player.sendMessage("You cannot use the " + commandArgs[0] + " command in survival.")
            event.isCancelled = true
            return
        }
        val coordinates: List<Double?> = getCoordinates(commandArgs) ?: return

        // no coordinates in command args
        val locations: List<Location> = getLocations(coordinates, player) ?: return

        // invalid number of arguments
        for (location in locations) {
            if (!utils.locationOnCreativeSide(location)) {
                if (numberOfWorldsEnabled > 1) {
                    player.sendMessage("The " + commandArgs[0] + " command cannot include blocks outside the creative sides of split worlds")
                    event.isCancelled = true
                } else if (numberOfWorldsEnabled == 1) {
                    player.sendMessage("The " + commandArgs[0] + " command cannot include blocks outside the creative side of the split world")
                    event.isCancelled = true
                }
                // we don't need to evaluate any more locations after finding one out of bounds
                return
            }
        }
    }

    fun onCommand(sender: CommandSender, command: Command, args: Array<out String>?): Boolean {
        val playerName = sender.name
        val player = sender.server.getPlayer(playerName)
        val playerPdc = player!!.persistentDataContainer

        // border sound toggle
        if (command.name.equals("play-border-sound", ignoreCase = true)) {
            if (args!!.size != 1) {
                return false
            }
            if (args[0].equals("true", ignoreCase = true)) {
                playerPdc.set(keys.playBorderSound, PersistentDataType.INTEGER, 1)
            } else if (args[0].equals("false", ignoreCase = true)) {
                playerPdc.set(keys.playBorderSound, PersistentDataType.INTEGER, 0)
            } else {
                return false
            }
            return true
        }

        //manage spawn builders
        if (command.name.equals("set-spawn-builder", ignoreCase = true)) {
            val server = player.server
            if (args!!.size != 2) {
                return false
            }
            val targetPlayer = server.getPlayer(args[0]) ?: return false
            val targetPlayerPdc = targetPlayer.persistentDataContainer
            if (args[1].equals("true", ignoreCase = true)) {
                targetPlayerPdc.set(keys.spawnBuilder, PersistentDataType.INTEGER, 1)
                println(targetPlayer.name + "is now a spawn builder.")
                targetPlayer.sendMessage("You now have permission to build in the spawn area.")
            } else if (args[1].equals("false", ignoreCase = true)) {
                targetPlayerPdc.set(keys.spawnBuilder, PersistentDataType.INTEGER, 0)
                println(targetPlayer.name + "is no longer a spawn builder.")
                targetPlayer.sendMessage("You no longer have permission to build in the spawn area.")
            } else {
                return false
            }
            return true
        }

        //dismiss welcome message permanently
        if (command.name.equals("understood", ignoreCase = true)) {
            playerPdc.set(keys.noWelcome, PersistentDataType.INTEGER, 1)
            player.sendMessage("You will no longer see the welcome message for split world.")
            return true
        }

        if (command.name.equals("disable-split-world", ignoreCase = true)) {
            if (player.hasPermission("split-world.disable-split-world")) {
                playerPdc.set(keys.splitWorldDisabled, PersistentDataType.INTEGER, 1)
            }
            return true
        }

        if (command.name.equals("enable-split-world", ignoreCase = true)) {
            if (player.hasPermission("split-world.enable-split-world")) {
                playerPdc.set(keys.splitWorldDisabled, PersistentDataType.INTEGER, 0)
            }
            playerUtils.switchPlayerToConfiguredGameMode(player)
            return true
        }

        return false
    }


    /* Command location parser */
    private fun getCoordinates(commandArgs: Array<String>): List<Double?>? {
        val args = ArrayList(listOf(*commandArgs))
        val command = args.removeAt(0)
        if (args.isEmpty()) {
            return null
        }
        val coordinateCount = when (command) {
            "/fill" -> 6
            "/clone" -> 9
            "/setblock" -> 3
            else -> -1
        }
        if (coordinateCount == -1) {
            return null
        }
        if (args.size < coordinateCount) {
            return null
        }
        val coordinates: MutableList<Double?> = ArrayList()
        for (i in 0 until coordinateCount) {
            if (args[i] == "~") {
                coordinates.add(null)
            } else {
                coordinates.add(args[i].toDouble())
            }
        }
        return coordinates.toList()
    }

    private fun getLocations(coordinates: List<Double?>, player: Player): List<Location>? {
        val size = coordinates.size
        if (size < 3 || size % 3 != 0) {
            return null
        }
        val locations: MutableList<Location> = ArrayList()
        for (i in 0 until size / 3) {
            val index = i * 3
            val x = if (coordinates[index] == null) player.location.x else coordinates[index]!!
            val y = if (coordinates[index + 1] == null) player.location.y else coordinates[index + 1]!!
            val z = if (coordinates[index + 2] == null) player.location.z else coordinates[index + 2]!!
            locations.add(Location(player.world, x, y, z))
        }
        return locations
    }

}