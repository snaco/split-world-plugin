package tech.snaco.SplitWorld

import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.persistence.PersistentDataType
import tech.snaco.SplitWorld.types.WorldConfig

class SplitWorldCommands(private var keys: SplitWorldKeys,
                         world_configs: Map<String, WorldConfig>,
                         private var manage_creative_commands: Boolean)  {

    private var utils = Utils(world_configs)
    private var number_of_worlds_enabled = world_configs.values.stream().filter { world: WorldConfig? ->
        world?.enabled ?: false
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
        if (!manage_creative_commands || event.player.isOp) {
            return
        }

        // creative commands allowed in the creative zones, these will be blocked in survival mode
        val creative_commands = listOf("/fill", "/clone", "/setblock")
        val player = event.player
        val command_str = event.message
        val command_args = command_str.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (command_args.size < 3) {
            return
        }

        // if not in managed creative commands, return to allow normal server handling of the event.
        if (!creative_commands.contains(command_args[0])) {
            return
        }

        // block creative command in survival
        if (player.gameMode == GameMode.SURVIVAL) {
            player.sendMessage("You cannot use the " + command_args[0] + " command in survival.")
            event.isCancelled = true
            return
        }
        val coordinates: List<Double?> = getCoordinates(command_args) ?: return

        // no coordinates in command args
        val locations: List<Location> = getLocations(coordinates, player) ?: return

        // invalid number of arguments
        for (location in locations) {
            if (!utils.locationOnCreativeSide(location)) {
                if (number_of_worlds_enabled > 1) {
                    player.sendMessage("The " + command_args[0] + " command cannot include blocks outside the creative sides of split worlds")
                    event.isCancelled = true
                } else if (number_of_worlds_enabled == 1) {
                    player.sendMessage("The " + command_args[0] + " command cannot include blocks outside the creative side of the split world")
                    event.isCancelled = true
                }
                // we don't need to evaluate any more locations after finding one out of bounds
                return
            }
        }
    }

    fun onCommand(sender: CommandSender, command: Command, args: Array<out String>?): Boolean {
        val player_name = sender.name
        val player = sender.server.getPlayer(player_name)
        val player_pdc = player!!.persistentDataContainer

        // border sound toggle
        if (command.name.equals("play-border-sound", ignoreCase = true)) {
            if (args!!.size != 1) {
                return false
            }

            if (args[0].equals("true", ignoreCase = true)) {
                player_pdc.set(keys.play_border_sound, PersistentDataType.INTEGER, 1)
            } else if (args[0].equals("false", ignoreCase = true)) {
                player_pdc.set(keys.play_border_sound, PersistentDataType.INTEGER, 0)
            } else {
                return false
            }
            return true
        }

        //TODO: Finish
        if (command.name.equals("competition-end", ignoreCase = true)) {
            val world_pdc = player.world.persistentDataContainer
            val is_competition_ended = world_pdc.get(keys.competition_ended, PersistentDataType.INTEGER)
            if (is_competition_ended == null) {
                world_pdc.set(keys.competition_ended, PersistentDataType.INTEGER, 1)
                player.world.worldBorder.setCenter(0.0, 0.0)
                player.world.worldBorder.setSize(30000.0, 600)
            }
        }

        // TODO: Implement set-winners

        //manage spawn builders
        if (command.name.equals("set-spawn-builder", ignoreCase = true)) {
            val server = player.server
            if (args!!.size != 2) {
                return false
            }
            val target_player = server.getPlayer(args[0]) ?: return false
            val target_player_pdc = target_player.persistentDataContainer
            if (args[1].equals("true", ignoreCase = true)) {
                target_player_pdc.set(keys.spawn_builder, PersistentDataType.INTEGER, 1)
                println(target_player.name + "is now a spawn builder.")
                target_player.sendMessage("You now have permission to build in the spawn area.")
            } else if (args[1].equals("false", ignoreCase = true)) {
                target_player_pdc.set(keys.spawn_builder, PersistentDataType.INTEGER, 0)
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
            player_pdc.set(keys.no_welcome, PersistentDataType.INTEGER, 1)
            player.sendMessage("You will no longer see the welcome message for split world.")
            return true
        }

        if (command.name.equals("disable-split-world", ignoreCase = true)) {
            if (player.hasPermission("split-world.disable-split-world")) {
                player_pdc.set(keys.split_world_disabled, PersistentDataType.INTEGER, 1)
            }
            return true
        }

        if (command.name.equals("enable-split-world", ignoreCase = true)) {
            if (player.hasPermission("split-world.enable-split-world")) {
                player_pdc.set(keys.split_world_disabled, PersistentDataType.INTEGER, 0)
            }
            return true
        }

        return false
    }


    /* Command location parser */
    private fun getCoordinates(command_args: Array<String>): List<Double?>? {
        val args = ArrayList(listOf(*command_args))
        val command = args.removeAt(0)
        if (args.size == 0) {
            return null
        }
        val coordinate_count = when (command) {
            "/fill" -> 6
            "/clone" -> 9
            "/setblock" -> 3
            else -> -1
        }
        if (coordinate_count == -1) {
            return null
        }
        if (args.size < coordinate_count) {
            return null
        }
        val coordinates: MutableList<Double?> = ArrayList()
        for (i in 0 until coordinate_count) {
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