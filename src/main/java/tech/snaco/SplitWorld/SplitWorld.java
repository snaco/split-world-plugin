package tech.snaco.SplitWorld;


import com.destroystokyo.paper.event.entity.EndermanEscapeEvent;
import io.papermc.paper.event.entity.EntityMoveEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;
import tech.snaco.SplitWorld.types.ItemStackArrayDataType;
import tech.snaco.SplitWorld.types.PotionEffectArrayDataType;
import tech.snaco.SplitWorld.types.WorldConfig;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings({"unchecked", "DataFlowIssue"})
public class SplitWorld extends JavaPlugin implements Listener {
    FileConfiguration config = getConfig();
    GameMode default_game_mode;
    Map<String, WorldConfig> world_configs;
    NamespacedKey no_welcome_key = new NamespacedKey(this, "no_welcome_message");
    NamespacedKey split_world_disabled_key = new NamespacedKey(this, "split_world_disabled");
    NamespacedKey first_join_key = new NamespacedKey(this, "split_world_first_join");
    NamespacedKey first_fish_attempt = new NamespacedKey(this, "first_fish_attempt");
    NamespacedKey competition_ended = new NamespacedKey(this, "competition_ended");
    NamespacedKey competition_participant = new NamespacedKey(this, "competition_participant");
    NamespacedKey received_rewards = new NamespacedKey(this, "received_rewards");
    NamespacedKey spawn_builder = new NamespacedKey(this, "spawn_builder");
    NamespacedKey play_border_sound = new NamespacedKey(this, "play_border_sound");

    ArrayList<Item> dropped_items = new ArrayList<>();
    int number_of_worlds_enabled;
    boolean manage_creative_commands;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        default_game_mode = switch (config.getString("default_game_mode")) {
            case "creative" -> GameMode.CREATIVE;
            case "adventure" -> GameMode.ADVENTURE;
            case "spectator" -> GameMode.SPECTATOR;
            default -> GameMode.SURVIVAL;
        };
        world_configs = config.getList("world_configs").stream().map(item -> new WorldConfig((Map<String, Object>) item)).collect(Collectors.toMap(WorldConfig::getWorldName, item -> item));
        Bukkit.getPluginManager().registerEvents(this, this);
        number_of_worlds_enabled = world_configs.values().stream().filter(world -> world.enabled).toList().size();
        manage_creative_commands = config.getBoolean("manage_creative_commands", true);
        new BukkitRunnable() {
            @Override
            public void run() {
                if ((long) dropped_items.size() > 0) {
                    var items_to_remove = new ArrayList<Item>();
                    for (Item item : dropped_items) {
                        if (worldEnabled(item.getWorld()) && locationInBufferZone(item.getLocation())) {
                            item.remove();
                            items_to_remove.add(item);
                        }
                    }
                    dropped_items.removeAll(items_to_remove);
                }
            }
        }.runTaskTimer(this, 0, 1L);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command cmd, @NotNull String label, String[] args) {
        var player_name = sender.getName();
        var player = sender.getServer().getPlayer(player_name);
        var player_pdc = player.getPersistentDataContainer();

        // border sound toggle
        if (cmd.getName().equalsIgnoreCase("play-border-sound")) {
            if (args.length != 1) {
                return false;
            }
            if (args[0].equalsIgnoreCase("true")) {
                player_pdc.set(play_border_sound, PersistentDataType.INTEGER, 1);
            } else if (args[0].equalsIgnoreCase("false")) {
                player_pdc.set(play_border_sound, PersistentDataType.INTEGER, 0);
            } else {
                return false;
            }
            return true;
        }

        //TODO: Finish
        if (cmd.getName().equalsIgnoreCase("competition-end")) {
            var world_pdc = player.getWorld().getPersistentDataContainer();
            var is_competition_ended = world_pdc.get(competition_ended, PersistentDataType.INTEGER);
            if (is_competition_ended == null) {
                world_pdc.set(competition_ended, PersistentDataType.INTEGER, 1);
                player.getWorld().getWorldBorder().setCenter(0, 0);
                player.getWorld().getWorldBorder().setSize(30000, 600);
            }
        }

        if (cmd.getName().equalsIgnoreCase("set-winners")) {
            // TODO: Implement set-winners
        }

        //manage spawn builders
        if (cmd.getName().equalsIgnoreCase("set-spawn-builder")) {
            var server = player.getServer();
            if (args.length != 2) {
                return false;
            }
            var target_player = server.getPlayer(args[0]);
            if (target_player == null) {
                return false;
            }
            var target_player_pdc = target_player.getPersistentDataContainer();
            if (args[1].equalsIgnoreCase("true")) {
                target_player_pdc.set(spawn_builder, PersistentDataType.INTEGER, 1);
                System.out.println(target_player.getName() + "is now a spawn builder.");
                target_player.sendMessage("You now have permission to build in the spawn area.");
            } else if (args[1].equalsIgnoreCase("false")) {
                target_player_pdc.set(spawn_builder, PersistentDataType.INTEGER, 0);
                System.out.println(target_player.getName() + "is no longer a spawn builder.");
                target_player.sendMessage("You no longer have permission to build in the spawn area.");
            } else {
                return false;
            }
            return true;
        }

        //dismiss welcome message permanently
        if (cmd.getName().equalsIgnoreCase("understood")) {
            player_pdc.set(no_welcome_key, PersistentDataType.INTEGER, 1);
            player.sendMessage("You will no longer see the welcome message for split world.");
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("disable-split-world")) {
            if (player.hasPermission("split-world.disable-split-world")) {
                player_pdc.set(split_world_disabled_key, PersistentDataType.INTEGER, 1);
            }
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("enable-split-world")) {
            if (player.hasPermission("split-world.enable-split-world")) {
                player_pdc.set(split_world_disabled_key, PersistentDataType.INTEGER, 0);
            }
            return true;
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, Command cmd, @NotNull String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("play-border-sound") && args.length == 1) {
            return List.of("true", "false");
        } else if (cmd.getName().equalsIgnoreCase("play-border-sound") && args.length > 1) {
            return new ArrayList<>();
        }
        return null;
    }
    /* Event Handlers */

    @EventHandler
    public void preProcessCommand(PlayerCommandPreprocessEvent event) {
        if (!manage_creative_commands || event.getPlayer().isOp()) {
            return;
        }

        // creative commands allowed in the creative zones, these will be blocked in survival mode
        var creative_commands = List.of("/fill", "/clone", "/setblock");
        var player = event.getPlayer();
        var command_str = event.getMessage();
        var command_args = command_str.split(" ");
        if (command_args.length < 3) {
            return;
        }

        // if not in managed creative commands, return to allow normal server handling of the event.
        if (!creative_commands.contains(command_args[0])) {
            return;
        }

        // block creative command in survival
        if (player.getGameMode() == GameMode.SURVIVAL) {
            player.sendMessage("You cannot use the " + command_args[0] + " command in survival.");
            event.setCancelled(true);
            return;
        }
        var coordinates = getCoordinates(command_args);

        // no coordinates in command args
        if (coordinates == null) {
            return;
        }
        var locations = getLocations(coordinates, player);

        // invalid number of arguments
        if (locations == null) {
            return;
        }

        for (var location : locations) {
            if (!locationOnCreativeSide(location)) {
                if (number_of_worlds_enabled > 1) {
                    player.sendMessage("The " + command_args[0] + " command cannot include blocks outside the creative sides of split worlds");
                    event.setCancelled(true);
                } else if (number_of_worlds_enabled == 1) {
                    player.sendMessage("The " + command_args[0] + " command cannot include blocks outside the creative side of the split world");
                    event.setCancelled(true);
                }
                // we don't need to evaluate any more locations after finding one out of bounds
                return;
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        var welcome_message_disabled = config.getBoolean("disable_welcome_message");
        if (welcome_message_disabled) {
            return;
        }
        var player = event.getPlayer();
        var player_pdc = player.getPersistentDataContainer();

        //track who participated in the competition
        var is_competition_ended = player.getWorld().getPersistentDataContainer().get(competition_ended, PersistentDataType.INTEGER);
        if (is_competition_ended == null || is_competition_ended == 0) {
            var is_participant = player_pdc.get(competition_participant, PersistentDataType.INTEGER);
            if (is_participant == null) {
                player_pdc.set(competition_participant, PersistentDataType.INTEGER, 1);
            }
        }

        var world_name = player.getWorld().getName();
        var no_welcome = player_pdc.get(no_welcome_key, PersistentDataType.INTEGER);
        if (!world_configs.containsKey(world_name) || !world_configs.get(world_name).enabled) {
            return;
        }
        var world_config = getWorldConfig(player.getWorld());
        if (no_welcome == null) {
            player.sendMessage(Component.text("Hello " + player.getName() + "! "
                    + "This world is split! You can head over towards the " + world_config.creative_side
                    + " side of the border at " + world_config.border_axis + "=" + world_config.border_location
                    + " to enter the creative side of the world. Your inventory will automatically be saved"
                    + " and loaded whenever you cross the border. Have fun! (To disable this message use /understood)"));
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (!worldEnabled(event.getBlock().getWorld())) { return; }
        if (locationInBufferZone(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
        if (locationOnSurvivalSide(event.getBlock().getLocation()) && locationInBufferZone(event.getPlayer().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (!worldEnabled(event.getPlayer().getWorld())) { return; }
        if (locationInBufferZone(event.getPlayer().getLocation())) {
            event.getPlayer().setHealth(0.1);
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onHealthRegain(EntityRegainHealthEvent event) {
        if (!worldEnabled(event.getEntity().getWorld())) { return; }
        if (locationInBufferZone(event.getEntity().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (!worldEnabled(event.getBlock().getWorld())) { return; }
        if (locationInBufferZone(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
        if (locationOnSurvivalSide(event.getBlock().getLocation()) && locationInBufferZone(event.getPlayer().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        var destination = event.getTo();
        var player = event.getPlayer();

        if (!worldEnabled(destination.getWorld())) {
            switchPlayerGameMode(player, default_game_mode);
            return;
        }

        if (locationInBufferZone(destination)) {
            switchPlayerGameMode(player, GameMode.SPECTATOR);
            return;
        } else if (locationOnCreativeSide(destination)) {
            switchPlayerGameMode(player, GameMode.CREATIVE);
            return;
        }

        var needs_warp = player.getGameMode() != GameMode.SURVIVAL;
        switchPlayerGameMode(player, GameMode.SURVIVAL);
        if (needs_warp) {
            warpPlayerToGround(player, player.getLocation());
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!worldEnabled(event.getPlayer().getWorld())) { return; }
        var player = event.getPlayer();
        var player_pdc = player.getPersistentDataContainer();
        var disabled = player_pdc.get(split_world_disabled_key, PersistentDataType.INTEGER);
        if (disabled != null && disabled == 1) {
            return;
        }

        // handle transitioning to survival safely
        if (player.getGameMode() != GameMode.SURVIVAL && locationOnSurvivalSide(event.getTo()) && locationOnSurvivalSide(player.getLocation())) {
            // temporarily load survival inv to check equip status
            loadPlayerInventory(player, GameMode.SURVIVAL);
            var player_has_elytra_equipped = player.getInventory().getChestplate() != null && player.getInventory().getChestplate().getType() == Material.ELYTRA;
            player.getInventory().clear();
            if (player_has_elytra_equipped && player.getLocation().getBlock().getType() == Material.AIR) {
                player.setGliding(true);
            }
            if (!player_has_elytra_equipped && locationOnSurvivalSide(player.getLocation()) && !player.isOnGround()) {
                warpPlayerToGround(player, event.getTo());
            }
        }
        switchPlayerToConfiguredGameMode(player);
        if (playerInBufferZone(player)) {
            player.getInventory().clear();
            var next_location = event.getTo();
            if (!locationIsTraversable(next_location)) {
                event.setCancelled(true);
            }
        }
        convertBufferZoneBlocksAroundPlayer(player);
    }

    @EventHandler
    public void onPlayerWorldChange(PlayerChangedWorldEvent event) {
        if (!worldEnabled(event.getPlayer().getWorld())) {
            return;
        }
        switchPlayerToConfiguredGameMode(event.getPlayer());
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        var custom_respawn = config.getBoolean("custom_respawn", false);
        var coordinates = Arrays.stream(config.getString("respawn_coordinates").split(" ")).map(Double::parseDouble).toList();
        if (custom_respawn && !event.isAnchorSpawn() && !event.isBedSpawn()) {
            event.setRespawnLocation(new Location(event.getRespawnLocation().getWorld(), coordinates.get(0), coordinates.get(1), coordinates.get(2), -88, 6));
        }
    }

    @EventHandler
    public void onBlockFromTo(BlockFromToEvent event) {
        if (!worldEnabled(event.getBlock().getWorld())) { return; }
        if (locationInBufferZone(event.getToBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityPortal(EntityPortalEvent event) {
        if (!worldEnabled(event.getFrom().getWorld())) { return; }
        if (locationOnCreativeSide(event.getFrom())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onItemSpawn(ItemSpawnEvent event) {
        if (!worldEnabled(event.getEntity().getWorld())) { return; }
        dropped_items.add(event.getEntity());
    }

    @EventHandler
    public void onEndermanEscape(EndermanEscapeEvent event) {
        if (!worldEnabled(event.getEntity().getWorld())) { return; }

        if (locationOnCreativeSide(event.getEntity().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityMove(EntityMoveEvent event) {
        if (!worldEnabled(event.getEntity().getWorld())) { return; }

        var entity = event.getEntity();
        var entity_next_location = event.getTo();
        var entity_world_name = entity_next_location.getWorld().getName();

        // only do this if players are online
        if (entity.getServer().getOnlinePlayers().size() == 0) { return; }
        // don't do for players (JIC)
        if (entity instanceof Player) { return; }
        // Make sure it's in an enabled world
        if (!world_configs.containsKey(entity_world_name) || !world_configs.get(entity_world_name).enabled) { return; }

        // stop no crossing unless you are a player
        if (locationInBufferZone(entity_next_location)) {
            event.setCancelled(true);
        }

        // only monsters on survival side
        if (entity instanceof Monster && locationOnCreativeSide(entity_next_location) && getWorldConfig(entity.getWorld()).no_creative_monsters) {
            entity.remove();
        }
    }

    @EventHandler
    public void onTarget(EntityTargetLivingEntityEvent event) {
        if (!worldEnabled(event.getEntity().getWorld())) { return; }
        if (event.getTarget() instanceof Player && !locationOnSurvivalSide(event.getTarget().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        var world = event.getLocation().getWorld();
        if (!worldEnabled(world)) { return; }
        if (!locationOnSurvivalSide(event.getLocation())
                && event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.NATURAL
                && getWorldConfig(world).no_creative_monsters
                && event.getEntity() instanceof Monster
        ) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPickup(PlayerAttemptPickupItemEvent event) {
        if (!worldEnabled(event.getPlayer().getWorld())) { return; }
        if (locationInBufferZone(event.getItem().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSpawn(PlayerSpawnLocationEvent event) {
        var custom_respawn = config.getBoolean("custom_respawn", false);
        var coordinates = Arrays.stream(config.getString("respawn_coordinates").split(" ")).map(Double::parseDouble).toList();
        var player_pdc = event.getPlayer().getPersistentDataContainer();
        var first_join = player_pdc.get(first_join_key, PersistentDataType.INTEGER);
        if (custom_respawn && (first_join == null || first_join != 1)) {
            player_pdc.set(first_join_key, PersistentDataType.INTEGER , 1);
            event.setSpawnLocation(new Location(event.getPlayer().getWorld(), coordinates.get(0), coordinates.get(1), coordinates.get(2), -88, 6));
        }
    }

    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {
        if (!worldEnabled(event.getPlayer().getWorld())) { return; }

        // no fishing creative stuff to survival side
        var caught = event.getCaught();
        if (caught == null) {
            return;
        }

        if (!locationOnSurvivalSide(caught.getLocation()) && !locationOnCreativeSide(event.getPlayer().getLocation())) {
            event.setCancelled(true);
        }

        //snark
        var player_pdc = event.getPlayer().getPersistentDataContainer();
        var first_attempt = player_pdc.get(first_fish_attempt, PersistentDataType.INTEGER);
        if (first_attempt == null) {
            event.getPlayer().giveExp(100);
            player_pdc.set(first_fish_attempt, PersistentDataType.INTEGER, 1);
        }
        event.getPlayer().sendMessage("Nice try.");
    }

    @EventHandler
    public void playerHunger(FoodLevelChangeEvent event) {
        if (!worldEnabled(event.getEntity().getWorld())) { return; }
        if (event.getEntity() instanceof Player && locationInBufferZone(event.getEntity().getLocation())) {
            event.setCancelled(true);
        }
    }

    /* Player Management */

    public void switchPlayerToConfiguredGameMode(Player player) {
        // keep players in the default mode when disabled for the world
        if (!worldEnabled(player.getWorld())) {
            switchPlayerGameMode(player, default_game_mode);
            return;
        }
        // set to spectator for buffer zone
        if (playerInBufferZone(player)) {
            var set_flying = player.isGliding() || player.isFlying();
            switchPlayerGameMode(player, GameMode.ADVENTURE);
            player.setAllowFlight(true);
            if (set_flying) {
                player.setFlying(true);
            }

            //keep player health and hunger static while in the border, ie no healing or dying here
            player.setFoodLevel(player.getFoodLevel());
            player.setHealth(player.getHealth());
        // creative side
        } else if (playerOnCreativeSide(player)) {
            switchPlayerGameMode(player, GameMode.CREATIVE);
        // survival side
        } else {
            switchPlayerGameMode(player, GameMode.SURVIVAL);
        }
    }

    public void switchPlayerGameMode(Player player, GameMode game_mode) {
        var player_inv = player.getInventory();
        var player_pdc = player.getPersistentDataContainer();
        if (player.getGameMode() != game_mode) {
            var play_sound = player_pdc.get(play_border_sound, PersistentDataType.INTEGER);
            if (play_sound == null || play_sound == 1) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_COW_BELL, 1.0f, 0.5f);
            }
            var y = 1.5;
            // south -Z
            // north +Z
            // east +X
            // west -X
            Vector vector = switch (player.getFacing()) {
                case NORTH -> new Vector(0, y, -1);
                case SOUTH -> new Vector(0, y, 1);
                case EAST -> new Vector(1, y, 0);
                case WEST -> new Vector(-1, y, 0);
                case UP -> new Vector(0, y + 1, 0);
                case DOWN -> new Vector(0, y - 1, 0);
                case NORTH_EAST -> new Vector(1, y, -1);
                case NORTH_WEST -> new Vector(-1, y, -1);
                case SOUTH_EAST -> new Vector(1, y, 1);
                case SOUTH_WEST -> new Vector(-1, y, 1);
                case WEST_NORTH_WEST -> new Vector(-1, y, -0.5);
                case NORTH_NORTH_WEST -> new Vector(-0.5, y, -1);
                case NORTH_NORTH_EAST -> new Vector(0.5, y, -1);
                case EAST_NORTH_EAST -> new Vector(1, y, -0.5);
                case EAST_SOUTH_EAST -> new Vector(1, y, 0.5);
                case SOUTH_SOUTH_EAST -> new Vector(0.5, y, 1);
                case SOUTH_SOUTH_WEST -> new Vector(-0.5, y, 1);
                case WEST_SOUTH_WEST -> new Vector(-1, y, 0.5);
                case SELF -> new Vector(0, 0, 0);
            };
            player.getWorld().spawnParticle(Particle.DRAGON_BREATH, player.getLocation().clone().add(vector), 20);

            savePlayerInventory(player);
            player_inv.clear();
            player.setGameMode(game_mode);
            loadPlayerInventory(player, game_mode);
        }
    }

    public void savePlayerInventory(Player player) {
        var player_pdc = player.getPersistentDataContainer();
        var inv_key = getPlayerInventoryKey(player, player.getGameMode());
        var eff_key = getPlayerEffectsKey(player, player.getGameMode());
        player_pdc.set(inv_key, new ItemStackArrayDataType(), player.getInventory().getContents());
        player_pdc.set(eff_key, new PotionEffectArrayDataType(), player.getActivePotionEffects().toArray(PotionEffect[]::new));
    }

    public void loadPlayerInventory(Player player, GameMode game_mode) {
        var inv_key = getPlayerInventoryKey(player, game_mode);
        var eff_key = getPlayerEffectsKey(player, game_mode);
        var player_pdc = player.getPersistentDataContainer();
        var new_inv = player_pdc.get(inv_key, new ItemStackArrayDataType());
        var effects = player_pdc.get(eff_key, new PotionEffectArrayDataType());
        if (new_inv != null) {
            player.getInventory().setContents(new_inv);
        }
        if (effects != null) {
            for (var effect : player.getActivePotionEffects()) {
                player.removePotionEffect(effect.getType());
            }
            for (var effect : effects) {
                player.addPotionEffect(effect);
            }
        }
    }

    public void convertBufferZoneBlocksAroundPlayer(Player player) {
        var world = player.getWorld();
        var world_config = getWorldConfig(world);
        var player_location = player.getLocation().clone();

        for (int i = world_config.border_location - (world_config.border_width / 2); i < world_config.border_location + (world_config.border_width / 2); i++) {
            for (int j = -5; j < 5; j++) {
                for (int y = -64; y < 319; y++) {
                    Location loc = player_location.clone();
                    loc.setY(y);
                    if (world_config.border_axis.equals("X")) {
                        loc.setX(i);
                        loc.setZ(loc.getZ() + j);
                    } else {
                        loc.setZ(i);
                        loc.setX(loc.getX() + j);
                    }
                    var block_type = world.getBlockAt(loc).getType();
                    if (block_type != Material.AIR && block_type != Material.WATER && block_type != Material.LAVA) {
                        world.getBlockAt(loc).setType(Material.BEDROCK);
                    } else if (block_type == Material.WATER || block_type == Material.LAVA) {
                        world.getBlockAt(loc).setType(Material.AIR);
                    }
                }
            }
        }
    }

    /* Location Methods */

    public boolean playerOnCreativeSide(Player player) {
        return locationOnCreativeSide(player.getLocation());
    }

    public boolean locationOnCreativeSide(Location location) {
        var world_config = getWorldConfig(location.getWorld());
        if (world_config.creative_side.equals("negative") && locationOnNegativeSideOfBuffer(location)) {
            return true;
        } else return world_config.creative_side.equals("positive") && locationOnPositiveSideOfBuffer(location);
    }

    public boolean locationOnSurvivalSide(Location location) {
        var world_config = getWorldConfig(location.getWorld());
        if (world_config.creative_side.equals("negative") && locationOnPositiveSideOfBuffer(location)) {
            return  true;
        } else return world_config.creative_side.equals("positive") && locationOnNegativeSideOfBuffer(location);
    }

    public boolean playerInBufferZone(Player player) {
        return locationInBufferZone(player.getLocation());
    }

    public boolean locationInBufferZone(Location location) {
        var pos = getRelevantPos(location);
        var world_config = getWorldConfig(location.getWorld());
        return pos >= world_config.border_location - (world_config.border_width / 2.0)
            && pos < world_config.border_location + (world_config.border_width / 2.0);
    }

    public boolean locationOnPositiveSideOfBuffer(Location location) {
        var world_config = getWorldConfig(location.getWorld());
        var pos = getRelevantPos(location);
        return pos >= world_config.border_location + (world_config.border_width / 2.0);
    }

    public boolean locationOnNegativeSideOfBuffer(Location location) {
        var world_config = getWorldConfig(location.getWorld());
        var pos = getRelevantPos(location);
        return pos < world_config.border_location - (world_config.border_width / 2.0);
    }

    public boolean locationIsTraversable(Location location) {
        var world = location.getWorld();
        var block_type = world.getBlockAt(location).getType();
        return !block_type.isSolid();
    }

    public double getRelevantPos(Location location) {
        var world_config = getWorldConfig(location.getWorld());
        return switch (world_config.border_axis.toUpperCase()) {
            case "Y" -> location.getY();
            case "Z" -> location.getZ();
            default -> location.getX();
        };
    }

    public void warpPlayerToGround(Player player, Location to_location) {
        var location = to_location.clone();
        var top = player.getWorld().getHighestBlockAt(location.getBlockX(), location.getBlockZ());
        var pitch = location.getPitch();
        var yaw = location.getYaw();
        var destination = top.getLocation().add(0, 1, 0);
        destination.setPitch(pitch);
        destination.setYaw(yaw);
        player.teleport(destination);
    }

    /* Misc. Utils */

    public WorldConfig getWorldConfig(World world) {
        return world_configs.get(world.getName());
    }

    public boolean worldEnabled(World world) {
        var world_name = world.getName();
        return world_configs.containsKey(world_name) && world_configs.get(world_name).enabled;
    }

    public NamespacedKey getPlayerInventoryKey(Player player, GameMode game_mode) {
        return switch (game_mode) {
            case CREATIVE -> new NamespacedKey(this, player.getName() + "_creative_inv");
            case SURVIVAL -> new NamespacedKey(this, player.getName() + "_survival_inv");
            case ADVENTURE -> new NamespacedKey(this, player.getName() + "_adventure_inv");
            case SPECTATOR -> new NamespacedKey(this, player.getName() + "_spectator_inv");
        };
    }

    public NamespacedKey getPlayerEffectsKey(Player player, GameMode game_mode) {
        return switch (game_mode) {
            case CREATIVE -> new NamespacedKey(this, player.getName() + "_creative_eff");
            case SURVIVAL -> new NamespacedKey(this, player.getName() + "_survival_eff");
            case ADVENTURE -> new NamespacedKey(this, player.getName() + "_adventure_eff");
            case SPECTATOR -> new NamespacedKey(this, player.getName() + "_spectator_eff");
        };
    }

    /* Command location parser */

    public List<Double> getCoordinates(String[] command_args) {
        var args = new ArrayList<>(Arrays.asList(command_args));
        var command = args.remove(0);
        if (args.size() == 0) {
            return null;
        }
        var coordinate_count = switch (command) {
            case "/fill" -> 6;
            case "/clone" -> 9;
            case "/setblock" -> 3;
            default -> -1;
        };
        if (coordinate_count == -1) {
            return null;
        }
        if (args.size() < coordinate_count) {
            return null;
        }
        List<Double> coordinates = new ArrayList<>();
        for (int i = 0; i < coordinate_count; i++) {
            if (args.get(i).equals("~")) {
                coordinates.add(null);
            } else {
                coordinates.add(Double.parseDouble(args.get(i)));
            }
        }
        return coordinates;
    }

    public List<Location> getLocations(List<Double> coordinates, Player player) {
        var size = coordinates.size();
        if (size < 3 || size % 3 != 0) {
            return null;
        }
        List<Location> locations = new ArrayList<>();
        for (int i = 0; i < size / 3; i ++) {
            var index = i * 3;
            var x = coordinates.get(index) == null ? player.getLocation().getX() : coordinates.get(index);
            var y = coordinates.get(index + 1) == null ? player.getLocation().getY() : coordinates.get(index + 1);
            var z = coordinates.get(index + 2) == null ? player.getLocation().getZ() : coordinates.get(index + 2);
            locations.add(new Location(player.getWorld(), x, y, z));
        }
        return locations;
    }

}
