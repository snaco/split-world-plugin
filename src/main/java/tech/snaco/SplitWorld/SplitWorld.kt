package tech.snaco.SplitWorld

import com.destroystokyo.paper.event.entity.EndermanEscapeEvent
import io.papermc.paper.event.entity.EntityMoveEvent
import io.papermc.paper.event.player.PlayerBedFailEnterEvent
import io.papermc.paper.event.player.PlayerDeepSleepEvent
import net.kyori.adventure.text.Component
import org.bukkit.*
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Item
import org.bukkit.entity.Monster
import org.bukkit.entity.Player
import org.bukkit.entity.Skeleton
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockFromToEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.*
import org.bukkit.event.player.*
import org.bukkit.event.player.PlayerBedEnterEvent.BedEnterResult
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.Vector
import org.spigotmc.event.player.PlayerSpawnLocationEvent
import tech.snaco.SplitWorld.extras.easter_eggs.Messages
import tech.snaco.SplitWorld.types.WorldConfig
import java.util.*
import java.util.stream.Collectors
import kotlin.collections.ArrayList

@Suppress("UNCHECKED_CAST")
class SplitWorld : JavaPlugin(), Listener {
    private var config: FileConfiguration = getConfig()
    private var default_game_mode: GameMode = when (config.getString("default_game_mode")) {
        "creative" -> GameMode.CREATIVE
        "adventure" -> GameMode.ADVENTURE
        "spectator" -> GameMode.SPECTATOR
        else -> GameMode.SURVIVAL
    }
    private var world_configs: Map<String, WorldConfig> = config.getList("world_configs")!!.stream()
            .map { item: Any? -> WorldConfig((item as Map<String, Any>)) }
            .collect(Collectors.toMap(
                    { item: WorldConfig -> item.world_name },
                    { item: WorldConfig -> item }))
    private var keys: SplitWorldKeys = SplitWorldKeys(this)
    private var dropped_items = ArrayList<Item>()
    private var utils: Utils = Utils(world_configs)
    private var player_utils: PlayerUtils = PlayerUtils(utils, keys, default_game_mode)
    private var command_handler: SplitWorldCommands = SplitWorldCommands(keys, player_utils, world_configs, config.getBoolean("manage_creative_commands", true))
    private var players_sleeping_in_nether = HashSet<Player>()

    override fun onEnable() {
        saveDefaultConfig()
        Bukkit.getPluginManager().registerEvents(this, this)
        object : BukkitRunnable() {
            override fun run() {
                if (dropped_items.size.toLong() > 0) {
                    val items_to_remove = ArrayList<Item>()
                    for (item in dropped_items) {
                        if (utils.worldEnabled(item.world) && utils.locationInBufferZone(item.location)) {
                            item.remove()
                            items_to_remove.add(item)
                        }
                    }
                    dropped_items.removeAll(items_to_remove.toSet())
                }
            }
        }.runTaskTimer(this, 0, 1L)

        object : BukkitRunnable() {
            override fun run() {
                Messages.runNetherSleepTask(players_sleeping_in_nether, keys)
            }
        }.runTaskTimer(this, 20L, 1L)
    }

    /* Commands */
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        return command_handler.onCommand(sender, command, args)
    }

    override fun onTabComplete(sender: CommandSender, command: Command, label: String, args: Array<String>): List<String>? {
        return command_handler.onTabComplete(command, args)
    }

    @EventHandler
    fun preProcessCommand(event: PlayerCommandPreprocessEvent?) {
        command_handler.preProcessCommand(event!!)
    }

    /* TBD */
    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val welcome_message_disabled = config.getBoolean("disable_welcome_message")
        if (welcome_message_disabled) {
            return
        }
        val player = event.player
        val player_pdc = player.persistentDataContainer
        val world_name = player.world.name
        val no_welcome = player_pdc.get(keys.no_welcome, PersistentDataType.INTEGER)
        if (!world_configs.containsKey(world_name) || !world_configs[world_name]!!.enabled) {
            return
        }
        val world_config = utils.getWorldConfig(player.world)
        if (no_welcome == null) {
            player.sendMessage(Component.text("Hello " + player.name + "! "
                    + "This world is split! You can head over towards the " + world_config.creative_side
                    + " side of the border at " + world_config.border_axis + "=" + world_config.border_location
                    + " to enter the creative side of the world. Your inventory will automatically be saved"
                    + " and loaded whenever you cross the border. Have fun! (To disable this message use /understood)"))
        }
    }

    @EventHandler
    fun onPlace(event: BlockPlaceEvent) {
        if (!utils.worldEnabled(event.block.world)) {
            return
        }
        if (utils.locationInBufferZone(event.block.location)) {
            event.isCancelled = true
        }
        if (utils.locationOnSurvivalSide(event.block.location) && utils.locationInBufferZone(event.player.location)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onDeath(event: PlayerDeathEvent) {
        event.droppedExp = event.player.totalExperience / 4

        if (!utils.worldEnabled(event.player.world)) {
            return
        }
        if (utils.locationInBufferZone(event.player.location)) {
            event.player.health = 0.1
            event.isCancelled = true
        }
        players_sleeping_in_nether.remove(event.player)
    }

    @EventHandler
    fun onHealthRegain(event: EntityRegainHealthEvent) {
        if (!utils.worldEnabled(event.entity.world)) {
            return
        }
        if (utils.locationInBufferZone(event.entity.location)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onBreak(event: BlockBreakEvent) {
        if (!utils.worldEnabled(event.block.world)) {
            return
        }
        if (utils.locationInBufferZone(event.block.location)) {
            event.isCancelled = true
        }
        if (utils.locationOnSurvivalSide(event.block.location) && utils.locationInBufferZone(event.player.location)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onTeleport(event: PlayerTeleportEvent) {
        val destination = event.to
        val player = event.player
        if (!utils.worldEnabled(destination.world)) {
            player_utils.switchPlayerGameMode(player, default_game_mode)
            return
        }
        if (utils.locationInBufferZone(destination)) {
            player_utils.switchPlayerGameMode(player, GameMode.SPECTATOR)
            return
        } else if (utils.locationOnCreativeSide(destination)) {
            player_utils.switchPlayerGameMode(player, GameMode.CREATIVE)
            return
        }
        val needs_warp = player.gameMode != GameMode.SURVIVAL
        player_utils.switchPlayerGameMode(player, GameMode.SURVIVAL)
        if (needs_warp) {
            player_utils.warpPlayerToGround(player, player.location)
        }
    }

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        if (!utils.worldEnabled(event.player.world)) {
            return
        }
        val player = event.player
        val player_pdc = player.persistentDataContainer
        val disabled = player_pdc.get(keys.split_world_disabled, PersistentDataType.INTEGER)
        if (disabled != null && disabled == 1) {
            return
        }

        // handle transitioning to survival safely
        if (player.gameMode != GameMode.SURVIVAL && utils.locationOnSurvivalSide(event.to) && utils.locationOnSurvivalSide(player.location)) {
            // temporarily load survival inv to check equip status
            player_utils.loadPlayerInventory(player, GameMode.SURVIVAL)
            val player_has_elytra_equipped = player.inventory.chestplate != null && player.inventory.chestplate!!.type == Material.ELYTRA
            player.inventory.clear()
            if (player_has_elytra_equipped && player.location.block.type == Material.AIR) {
                player.isGliding = true
            }
            if (!player_has_elytra_equipped && utils.locationOnSurvivalSide(player.location) && player_utils.playerInAir(player)) {
                player.velocity = Vector(0, 0, 0)
                player_utils.warpPlayerToGround(player, player.location)
            }
        }
        player_utils.switchPlayerToConfiguredGameMode(player)
        if (utils.playerInBufferZone(player)) {
            player.inventory.clear()
            val next_location = event.to
            if (!utils.locationIsTraversable(next_location)) {
                event.isCancelled = true
            }
        }
        player_utils.convertBufferZoneBlocksAroundPlayer(player)
    }

    @EventHandler
    fun onPlayerWorldChange(event: PlayerChangedWorldEvent) {
        if (!utils.worldEnabled(event.player.world)) {
            return
        }
        player_utils.switchPlayerToConfiguredGameMode(event.player)
    }

    @EventHandler
    fun onPlayerRespawn(event: PlayerRespawnEvent) {
        val custom_respawn = config.getBoolean("custom_respawn", false)
        val coordinates = Arrays.stream(config.getString("respawn_coordinates")!!.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()).map { s: String -> s.toDouble() }.toList()
        if (custom_respawn && !event.isAnchorSpawn && !event.isBedSpawn) {
            event.respawnLocation = Location(event.respawnLocation.world, coordinates[0], coordinates[1], coordinates[2], -88f, 6f)
        }
        if (utils.locationOnSurvivalSide(event.respawnLocation)) {
            player_utils.switchPlayerGameMode(event.player, GameMode.SURVIVAL)
        } else {
            player_utils.switchPlayerGameMode(event.player, GameMode.CREATIVE)
        }
    }

    @EventHandler
    fun onBlockFromTo(event: BlockFromToEvent) {
        if (!utils.worldEnabled(event.block.world)) {
            return
        }
        if (utils.locationInBufferZone(event.toBlock.location)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onEntityPortal(event: EntityPortalEvent) {
        if (!utils.worldEnabled(event.from.world)) {
            return
        }
        if (utils.locationOnCreativeSide(event.from)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onItemSpawn(event: ItemSpawnEvent) {
        if (!utils.worldEnabled(event.entity.world)) {
            return
        }
        dropped_items.add(event.entity)
    }

    @EventHandler
    fun onEndermanEscape(event: EndermanEscapeEvent) {
        if (!utils.worldEnabled(event.entity.world)) {
            return
        }
        if (utils.locationOnCreativeSide(event.entity.location)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onEntityMove(event: EntityMoveEvent) {
        if (!utils.worldEnabled(event.entity.world)) {
            return
        }
        val entity = event.entity
        val entity_next_location = event.to
        val entity_world_name = entity_next_location.world.name

        // only do this if players are online
        if (entity.server.onlinePlayers.isEmpty()) {
            return
        }
        // don't do for players (JIC)
        if (entity is Player) {
            return
        }
        // Make sure it's in an enabled world
        if (!world_configs.containsKey(entity_world_name) || !world_configs[entity_world_name]!!.enabled) {
            return
        }

        // stop no crossing unless you are a player
        if (utils.locationInBufferZone(entity_next_location)) {
            event.isCancelled = true
        }

        // only monsters on survival side
        if (entity is Monster && utils.locationOnCreativeSide(entity_next_location) && utils.getWorldConfig(entity.getWorld()).no_creative_monsters) {
            entity.remove()
        }
    }

    @EventHandler
    fun onTarget(event: EntityTargetLivingEntityEvent) {
        if (!utils.worldEnabled(event.entity.world)) {
            return
        }
        if (event.target is Player && !utils.locationOnSurvivalSide((event.target as Player).location)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onCreatureSpawn(event: CreatureSpawnEvent) {
        val world = event.location.world
        if (!utils.worldEnabled(world)) {
            return
        }
        if (!utils.locationOnSurvivalSide(event.location) && event.spawnReason == CreatureSpawnEvent.SpawnReason.NATURAL && utils.getWorldConfig(world).no_creative_monsters
                && event.entity is Monster) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onPickup(event: PlayerAttemptPickupItemEvent) {
        if (!utils.worldEnabled(event.player.world)) {
            return
        }
        if (utils.locationInBufferZone(event.item.location)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onSpawn(event: PlayerSpawnLocationEvent) {
        val custom_respawn = config.getBoolean("custom_respawn", false)
        val coordinates = Arrays.stream(config.getString("respawn_coordinates")!!.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()).map { s: String -> s.toDouble() }.toList()
        val player_pdc = event.player.persistentDataContainer
        val first_join = player_pdc.get(keys.first_join, PersistentDataType.INTEGER)
        if (custom_respawn && (first_join == null || first_join != 1)) {
            player_pdc.set(keys.first_join, PersistentDataType.INTEGER, 1)
            event.spawnLocation = Location(event.player.world, coordinates[0], coordinates[1], coordinates[2], -88f, 6f)
        }
    }

    @EventHandler
    fun onPlayerFish(event: PlayerFishEvent) {
        if (!utils.worldEnabled(event.player.world)) {
            return
        }

        // no fishing creative stuff to survival side
        val caught = event.caught ?: return
        if (!utils.locationOnSurvivalSide(caught.location) && !utils.locationOnCreativeSide(event.player.location)) {
            event.isCancelled = true
        }

        //snark
        val player_pdc = event.player.persistentDataContainer
        val first_attempt = player_pdc.get(keys.first_fish_attempt, PersistentDataType.INTEGER)
        if (first_attempt == null) {
            event.player.giveExp(100)
            player_pdc.set(keys.first_fish_attempt, PersistentDataType.INTEGER, 1)
        }
        event.player.sendMessage("Nice try.")
    }

    @EventHandler
    fun playerHunger(event: FoodLevelChangeEvent) {
        if (!utils.worldEnabled(event.entity.world)) {
            return
        }
        if (event.entity is Player && utils.locationInBufferZone(event.entity.location)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onSkeletonShoots(event: EntityShootBowEvent) {
        if (event.entity is Skeleton) {
            val skeleton = event.entity as Skeleton
            if (skeleton.target != null && !utils.locationOnSurvivalSide(skeleton.target!!.location)) {
                event.isCancelled = true
            }
        }
    }

    @EventHandler
    fun itemInteract(event: PlayerInteractEvent) {
        if (event.item == null) {
            return
        }
        if (event.item!!.type == Material.DIAMOND){
            event.player.playSound(event.player.location, Sound.ENTITY_GENERIC_EAT, 1.0f, 1.0f)
            event.player.inventory.getItem(event.player.inventory.indexOf(event.item!!))!!.subtract()
            event.player.giveExp(50)
            event.player.sendMessage("You ate a diamond you madlad!")
        }
    }

    @EventHandler
    fun enterBed(event: PlayerBedEnterEvent) {
        val nether_egg_complete = Utils.getPdcInt(event.player, keys.nether_egg)
        if (nether_egg_complete != null && nether_egg_complete >= 1561) {
            return
        }
        if (event.bedEnterResult == BedEnterResult.NOT_POSSIBLE_HERE) {
            event.setUseBed(Event.Result.ALLOW)
            event.player.sendMessage("Didn't expect that did you?")
        }
    }

    @EventHandler
    fun failEnterBed(event: PlayerBedFailEnterEvent) {
        val nether_egg_complete = Utils.getPdcInt(event.player, keys.nether_egg)
        if (nether_egg_complete != null && nether_egg_complete >= 1561) {
            return
        }
        if (event.failReason == PlayerBedFailEnterEvent.FailReason.NOT_POSSIBLE_HERE) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun enterDeepSleep(event: PlayerDeepSleepEvent) {
        if (event.player.world.name.endsWith("_nether")) {
            players_sleeping_in_nether.add(event.player)
            event.isCancelled = true
        }
    }

    @EventHandler
    fun leaveBed(event: PlayerBedLeaveEvent) {
        if (event.player.world.name.endsWith("_nether")) {
            players_sleeping_in_nether.remove(event.player)
        }
    }
}