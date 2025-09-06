package tech.snaco.split_world

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
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockEvent
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
import tech.snaco.split_world.extras.easter_eggs.Messages
import tech.snaco.split_world.types.WorldConfig
import java.util.*
import java.util.stream.Collectors

@Suppress("UNCHECKED_CAST")
class SplitWorld : JavaPlugin(), Listener {
    private var config: FileConfiguration = getConfig()
    private var defaultGameMode: GameMode = when (config.getString("default_game_mode")) {
        "creative" -> GameMode.CREATIVE
        "adventure" -> GameMode.ADVENTURE
        "spectator" -> GameMode.SPECTATOR
        else -> GameMode.SURVIVAL
    }
    private var worldConfigs: Map<String, WorldConfig> = config.getList("world_configs")!!.stream()
            .map { item: Any? -> WorldConfig((item as Map<String, Any>)) }
            .collect(Collectors.toMap(
                    { item: WorldConfig -> item.worldName },
                    { item: WorldConfig -> item }))
    private var keys: SplitWorldKeys = SplitWorldKeys(this)
    private var droppedItems = ArrayList<Item>()
    private var utils: Utils = Utils(worldConfigs)
    private var playerUtils: PlayerUtils = PlayerUtils(utils, keys, defaultGameMode)
    private var commandHandler: SplitWorldCommands = SplitWorldCommands(keys, playerUtils, worldConfigs, config.getBoolean("manage_creative_commands", true))
    private var playersSleepingInNether = HashSet<Player>()

    override fun onEnable() {
        saveDefaultConfig()
        Bukkit.getPluginManager().registerEvents(this, this)
        object : BukkitRunnable() {
            override fun run() {
                if (droppedItems.size.toLong() > 0) {
                    val itemsToRemove = ArrayList<Item>()
                    for (item in droppedItems) {
                        if (utils.worldEnabled(item.world) && utils.locationInBufferZone(item.location)) {
                            item.remove()
                            itemsToRemove.add(item)
                        }
                    }
                    droppedItems.removeAll(itemsToRemove.toSet())
                }
            }
        }.runTaskTimer(this, 0, 1L)

        object : BukkitRunnable() {
            override fun run() {
                Messages.runNetherSleepTask(playersSleepingInNether, keys)
            }
        }.runTaskTimer(this, 20L, 1L)
    }

    /* Commands */
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        return commandHandler.onCommand(sender, command, args)
    }

    override fun onTabComplete(sender: CommandSender, command: Command, label: String, args: Array<String>): List<String>? {
        return commandHandler.onTabComplete(command, args)
    }

    @EventHandler
    fun preProcessCommand(event: PlayerCommandPreprocessEvent?) {
        commandHandler.preProcessCommand(event!!)
    }

    /* TBD */
    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val welcomeMessageDisabled = config.getBoolean("disable_welcome_message")
        if (welcomeMessageDisabled) {
            return
        }
        val player = event.player
        val playerPdc = player.persistentDataContainer
        val worldName = player.world.name
        val noWelcome = playerPdc.get(keys.noWelcome, PersistentDataType.INTEGER)
        if (!worldConfigs.containsKey(worldName) || !worldConfigs[worldName]!!.enabled) {
            return
        }
        val worldConfig = utils.getWorldConfig(player.world)
        if (noWelcome == null) {
            player.sendMessage(Component.text("""
                Hello ${player.name}! 
                This is a split world! That means half of the world is creative, and half is survival.
                There is a border at ${worldConfig.borderAxis}=${worldConfig.borderLocation}.
                Creative is on the ${worldConfig.creativeSide} side of the border.
                You now have two inventories, one for each side of the border, which will be saved and restored automatically when you cross.
                Have fun! (You can disable this message from showing using /understood) 
            """.trimIndent()))
        }
    }

    @EventHandler
    fun onPlace(event: BlockPlaceEvent) {
        handleBlockEvent(event, event.player)
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
        playersSleepingInNether.remove(event.player)
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
        handleBlockEvent(event, event.player)
    }

    @EventHandler
    fun onTeleport(event: PlayerTeleportEvent) {
        val destination = event.to
        val player = event.player
        if (!utils.worldEnabled(destination.world)) {
            playerUtils.switchPlayerGameMode(player, defaultGameMode)
            return
        }
        if (utils.locationInBufferZone(destination)) {
            playerUtils.switchPlayerGameMode(player, GameMode.SPECTATOR)
            return
        } else if (utils.locationOnCreativeSide(destination)) {
            playerUtils.switchPlayerGameMode(player, GameMode.CREATIVE)
            return
        }
        val needsWarp = player.gameMode != GameMode.SURVIVAL
        playerUtils.switchPlayerGameMode(player, GameMode.SURVIVAL)
        if (needsWarp) {
            playerUtils.warpPlayerToGround(player, player.location)
        }
    }

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        if (!utils.worldEnabled(event.player.world)) {
            return
        }
        val player = event.player
        val playerPdc = player.persistentDataContainer
        val disabled = playerPdc.get(keys.splitWorldDisabled, PersistentDataType.INTEGER)
        if (disabled != null && disabled == 1) {
            return
        }

        // handle transitioning to survival safely
        if (player.gameMode != GameMode.SURVIVAL && utils.locationOnSurvivalSide(event.to) && utils.locationOnSurvivalSide(player.location)) {
            // temporarily load survival inv to check equip status
            playerUtils.loadPlayerInventory(player)
            val playerHasElytraEquipped = player.inventory.chestplate != null && player.inventory.chestplate!!.type == Material.ELYTRA
            player.inventory.clear()
          player.enderChest.contents
            if (playerHasElytraEquipped && player.location.block.type == Material.AIR) {
                player.isGliding = true
            }
            if (!playerHasElytraEquipped && utils.locationOnSurvivalSide(player.location) && playerUtils.playerInAir(player)) {
                player.velocity = Vector(0, 0, 0)
                playerUtils.warpPlayerToGround(player, player.location)
            }
        }
        playerUtils.switchPlayerToConfiguredGameMode(player)
        if (utils.playerInBufferZone(player)) {
            player.inventory.clear()
            val nextLocation = event.to
            if (!utils.locationIsTraversable(nextLocation)) {
                event.isCancelled = true
            }
        }
        playerUtils.convertBufferZoneBlocksAroundPlayer(player)
    }

    @EventHandler
    fun onPlayerWorldChange(event: PlayerChangedWorldEvent) {
        if (!utils.worldEnabled(event.player.world)) {
            return
        }
        playerUtils.switchPlayerToConfiguredGameMode(event.player)
    }

    @EventHandler
    fun onPlayerRespawn(event: PlayerRespawnEvent) {
        val customRespawn = config.getBoolean("custom_respawn", false)
        val coordinates = utils.getSpawnCoordinates(config)
        if (customRespawn && !event.isAnchorSpawn && !event.isBedSpawn) {
            event.respawnLocation = Location(event.respawnLocation.world, coordinates[0], coordinates[1], coordinates[2], -88f, 6f)
        }
        if (utils.locationOnSurvivalSide(event.respawnLocation)) {
            playerUtils.switchPlayerGameMode(event.player, GameMode.SURVIVAL)
        } else {
            playerUtils.switchPlayerGameMode(event.player, GameMode.CREATIVE)
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
        droppedItems.add(event.entity)
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
        val entityNextLocation = event.to
        val entityWorldName = entityNextLocation.world.name

        // only do this if players are online
        if (entity.server.onlinePlayers.isEmpty()) {
            return
        }
        // don't do for players (JIC)
        if (entity is Player) {
            return
        }
        // Make sure it's in an enabled world
        if (!worldConfigs.containsKey(entityWorldName) || !worldConfigs[entityWorldName]!!.enabled) {
            return
        }

        // stop no crossing unless you are a player
        if (utils.locationInBufferZone(entityNextLocation)) {
            event.isCancelled = true
        }

        // only monsters on survival side
        if (entity is Monster && utils.locationOnCreativeSide(entityNextLocation) && utils.getWorldConfig(entity.world).noCreativeMonsters) {
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
        if (!utils.locationOnSurvivalSide(event.location) && event.spawnReason == CreatureSpawnEvent.SpawnReason.NATURAL && utils.getWorldConfig(world).noCreativeMonsters
                && event.entity is Monster) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onPickup(event: PlayerAttemptPickupItemEvent) {
        if (!utils.worldEnabled(event.player.world)) {
            return
        }
        if (utils.locationInBufferZone(event.player.location)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onSpawn(event: PlayerSpawnLocationEvent) {
        val customRespawn = config.getBoolean("custom_respawn", false)
        val coordinates = utils.getSpawnCoordinates(config)
        val playerPdc = event.player.persistentDataContainer
        val firstJoin = playerPdc.get(keys.firstJoin, PersistentDataType.INTEGER)
        if (customRespawn && (firstJoin == null || firstJoin != 1)) {
            playerPdc.set(keys.firstJoin, PersistentDataType.INTEGER, 1)
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
        val playerPdc = event.player.persistentDataContainer
        val firstAttempt = playerPdc.get(keys.firstFishAttempt, PersistentDataType.INTEGER)
        if (firstAttempt == null) {
            event.player.giveExp(100)
            playerPdc.set(keys.firstFishAttempt, PersistentDataType.INTEGER, 1)
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
            event.player.giveExpLevels(2)
            event.player.sendMessage("You ate a diamond you absolute madlad!")
        }
    }

    @EventHandler
    fun enterBed(event: PlayerBedEnterEvent) {
        val netherEggComplete = Utils.getPdcInt(event.player, keys.netherEgg)
        if (netherEggComplete != null && netherEggComplete >= 1561) {
            return
        }
        if (event.bedEnterResult == BedEnterResult.NOT_POSSIBLE_HERE) {
            event.setUseBed(Event.Result.ALLOW)
            event.player.sendMessage("Didn't expect that did you?")
        }
    }

    @EventHandler
    fun failEnterBed(event: PlayerBedFailEnterEvent) {
        val netherEggComplete = Utils.getPdcInt(event.player, keys.netherEgg)
        if (netherEggComplete != null && netherEggComplete >= 1561) {
            return
        }
        if (event.failReason == PlayerBedFailEnterEvent.FailReason.NOT_POSSIBLE_HERE) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun enterDeepSleep(event: PlayerDeepSleepEvent) {
        if (event.player.world.name.endsWith("_nether")) {
            playersSleepingInNether.add(event.player)
            event.isCancelled = true
        }
    }

    @EventHandler
    fun leaveBed(event: PlayerBedLeaveEvent) {
        if (event.player.world.name.endsWith("_nether")) {
            playersSleepingInNether.remove(event.player)
        }
    }

    fun <T> handleBlockEvent(event: T, player: Player) where T : BlockEvent, T : Cancellable {
        if (!utils.worldEnabled(event.block.world)) {
            return
        }
        if (utils.locationInBufferZone(event.block.location)) {
            event.isCancelled = true
        }
        if (utils.locationOnSurvivalSide(event.block.location) && utils.locationInBufferZone(player.location)) {
            event.isCancelled = true
        }
    }
}