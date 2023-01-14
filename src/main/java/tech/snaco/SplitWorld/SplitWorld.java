package tech.snaco.SplitWorld;


import io.papermc.paper.event.entity.EntityMoveEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import tech.snaco.SplitWorld.utils.ItemStackArrayDataType;
import tech.snaco.SplitWorld.utils.WorldConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SuppressWarnings({"unchecked", "DataFlowIssue"})
public class SplitWorld extends JavaPlugin implements Listener {
    FileConfiguration config = getConfig();
    GameMode default_game_mode;
    Map<String, WorldConfig> world_configs;
    NamespacedKey no_welcome_key = new NamespacedKey(this, "no_welcome_message");
    NamespacedKey split_world_disabled_key = new NamespacedKey(this, "split_world_disabled");
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

    @EventHandler
    public void preProcessCommand(PlayerCommandPreprocessEvent event) {
        if (!manage_creative_commands) {
            return;
        }
        var ignore_commands = List.of("/fill", "/clone", "/setblock");
        var player = event.getPlayer();

        var world = player.getWorld();
        var command_str = event.getMessage();
        var command_args = command_str.split(" ");
        if (command_args.length < 3) {
            return;
        }
        if (!ignore_commands.contains(command_args[0])) {
            return;
        }
        if (player.getGameMode() == GameMode.SURVIVAL) {
            player.sendMessage("You cannot use the " + command_args[0] + " command in survival.");
            event.setCancelled(true);
            return;
        }
        var coordinates = getCoordinates(command_args);
        if (coordinates == null) {
            return;
        }
        var locations = getLocations(coordinates, player);
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
                    + " and loaded whenever you cross the border. Have fun!"));
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (locationInBufferZone(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        var destination = event.getTo();
        var player = event.getPlayer();

        if (locationInBufferZone(destination)) {
            switchPlayerGameMode(player, GameMode.SPECTATOR);
        } else if (locationOnCreativeSide(destination)) {
            switchPlayerGameMode(player, GameMode.CREATIVE);
        } else {
            var needs_warp = player.getGameMode() != GameMode.SURVIVAL;
            switchPlayerGameMode(player, GameMode.SURVIVAL);
            if (needs_warp) {
                warpPlayerToGround(player);
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        var player = event.getPlayer();
        var player_pdc = player.getPersistentDataContainer();
        var disabled = player_pdc.get(split_world_disabled_key, PersistentDataType.INTEGER);
        convertBufferZoneBlocksAroundPlayer(player);
        if (disabled != null && disabled == 1) {
            return;
        }
        if (warpIsRecommended(player)) {
            warpPlayerToGround(player);
        }
        switchPlayerToConfiguredGameMode(player);
        if (playerInBufferZone(player)) {
            var next_block = event.getTo().getBlock();
            if (next_block.getType() != Material.AIR && next_block.getType() != Material.WATER) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerWorldChange(PlayerChangedWorldEvent event) {
        switchPlayerToConfiguredGameMode(event.getPlayer());
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        var custom_respawn = config.getBoolean("custom_respawn", false);
        var coordinates = Arrays.stream(config.getString("respawn_coordinates").split(" ")).map(Double::parseDouble).toList();
        if (custom_respawn) {
            event.setRespawnLocation(new Location(event.getPlayer().getWorld(), coordinates.get(0), coordinates.get(1), coordinates.get(2), -88, 6));
        }
    }

    @EventHandler
    public void onBlockFromTo(BlockFromToEvent event) {
        if (worldEnabled(event.getBlock().getWorld()) && locationInBufferZone(event.getToBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onItemSpawn(ItemSpawnEvent event) {
        dropped_items.add(event.getEntity());
    }

    @EventHandler
    public void onEntityMove(EntityMoveEvent event) {
        var entity = event.getEntity();
        var entity_location = event.getTo();
        var entity_world_name = entity_location.getWorld().getName();

        // don't do this to players
        if (entity instanceof  Player) { return; }
        // Only do for monsters
        if (!(entity instanceof Monster)) { return; }
        // Make sure it's in an enabled world
        if (!world_configs.containsKey(entity_world_name) || !world_configs.get(entity_world_name).enabled) { return; }
        // only do this if players are online
        if (entity.getServer().getOnlinePlayers().size() == 0) { return; }
        // don't affect monster's not trying to move in to the buffer zone
        if (!locationInBufferZone(entity_location)) {
            return;
        }
        // Stop it, don't go there. The buffer zone is forbidden to monsters.
        event.setCancelled(true);
    }

    @EventHandler
    public void onPickup(PlayerAttemptPickupItemEvent event) {
        if (locationInBufferZone(event.getItem().getLocation())) {
            event.setCancelled(true);
        }
    }

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
            var x = coordinates.get(i) == null ? player.getLocation().getX() : coordinates.get(i);
            var y = coordinates.get(i) == null ? player.getLocation().getY() : coordinates.get(i + 1);
            var z = coordinates.get(i) == null ? player.getLocation().getZ() : coordinates.get(i + 2);
            locations.add(new Location(player.getWorld(), x, y, z));
        }
        return locations;
    }

    public void convertBufferZoneBlocksAroundPlayer(Player player) {
        var world = player.getWorld();
        var world_config = getWorldConfig(world);
        var player_location = player.getLocation().clone();

        for (int i = world_config.border_location - (world_config.border_width / 2); i < world_config.border_location + (world_config.border_width / 2); i++) {
            for (int j = -5; j < 5; j++) {
                for (int y = -64; y < 319; y++) {
                    Location loc = player_location.clone();
                    loc.setY((double) y);
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

    public boolean locationIsTraversable(Location location) {
        var world = location.getWorld();
        var block_type = world.getBlockAt(location).getType();
        return block_type == Material.AIR || block_type == Material.WATER || block_type == Material.LAVA;
    }


    public void switchPlayerToConfiguredGameMode(Player player) {
        // keep players in the default mode when disabled for the world
        if (!worldEnabled(player.getWorld())) {
            switchPlayerGameMode(player, default_game_mode);
            return;
        }
        // set to spectator for buffer zone
        if (playerInBufferZone(player)) {
            switchPlayerGameMode(player, GameMode.SPECTATOR);
        // creative side
        } else if (playerOnCreativeSide(player)) {
            switchPlayerGameMode(player, GameMode.CREATIVE);
        // survival side
        } else {
            switchPlayerGameMode(player, GameMode.SURVIVAL);
        }
    }

    public boolean worldEnabled(World world) {
        var world_name = world.getName();
        return world_configs.containsKey(world_name) && world_configs.get(world_name).enabled;
    }

    public void warpPlayerToGround(Player player) {
        var location = player.getLocation().clone();
        var velocity = player.getVelocity().clone();
        var top = player.getWorld().getHighestBlockAt(location.getBlockX(), location.getBlockZ());
        var pitch = location.getPitch();
        var yaw = location.getYaw();
        var destination = top.getLocation().add(0, 1, 0);
        destination.setPitch(pitch);
        destination.setYaw(yaw);
        player.teleport(destination);
        player.setVelocity(velocity);
    }

    public boolean warpIsRecommended(Player event) {
        var player = event.getPlayer();
        var player_has_elytra_equipped = player.getInventory().getChestplate() != null && player.getInventory().getChestplate().getType() == Material.ELYTRA;
        var player_not_survival = player.getGameMode() != GameMode.SURVIVAL;
        var on_survival_side = locationOnSurvivalSide(player.getLocation());

        return !player_has_elytra_equipped && player_not_survival && on_survival_side;
    }

    public void switchPlayerGameMode(Player player, GameMode game_mode) {
        var player_inv = player.getInventory();
        if (player.getGameMode() != game_mode) {
            savePlayerInventory(player);
            player_inv.clear();
            player.setGameMode(game_mode);
            loadPlayerInventory(player, game_mode);
        }
    }

    public void savePlayerInventory(Player player) {
        var player_pdc = player.getPersistentDataContainer();
        var key = switch (player.getGameMode()) {
            case CREATIVE -> new NamespacedKey(this, player.getName() + "_creative_inv");
            case SURVIVAL -> new NamespacedKey(this, player.getName() + "_survival_inv");
            case ADVENTURE -> new NamespacedKey(this, player.getName() + "_adventure_inv");
            case SPECTATOR -> new NamespacedKey(this, player.getName() + "_spectator_inv");
        };
        player_pdc.set(key, new ItemStackArrayDataType(), player.getInventory().getContents());
    }

    public void loadPlayerInventory(Player player, GameMode game_mode) {
        var key = getPlayerInventoryKey(player, game_mode);
        var player_pdc = player.getPersistentDataContainer();
        var new_inv = player_pdc.get(key, new ItemStackArrayDataType());
        if (new_inv != null) {
            player.getInventory().setContents(new_inv);
        }
    }

    public NamespacedKey getPlayerInventoryKey(Player player, GameMode game_mode) {
        return switch (game_mode) {
            case CREATIVE -> new NamespacedKey(this, player.getName() + "_creative_inv");
            case SURVIVAL -> new NamespacedKey(this, player.getName() + "_survival_inv");
            case ADVENTURE -> new NamespacedKey(this, player.getName() + "_adventure_inv");
            case SPECTATOR -> new NamespacedKey(this, player.getName() + "_spectator_inv");
        };
    }

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

    public boolean locationWithinDistanceOfBuffer(Location location, int distance) {
        var pos = getRelevantPos(location);
        var world_config = getWorldConfig(location.getWorld());
        return pos >= world_config.border_location - (world_config.border_width / 2.0) - distance
                && pos < world_config.border_location + (world_config.border_width / 2.0) + distance;
    }

    public boolean locationOnPositiveSideOfBuffer(Location location) {
        var world_config = getWorldConfig(location.getWorld());
        var pos = getRelevantPos(location);
        return pos > world_config.border_location + (world_config.border_width / 2.0);
    }

    public boolean locationOnNegativeSideOfBuffer(Location location) {
        var world_config = getWorldConfig(location.getWorld());
        var pos = getRelevantPos(location);
        return pos < world_config.border_location - (world_config.border_width / 2.0);
    }

    public double getRelevantPos(Location location) {
        var world_config = getWorldConfig(location.getWorld());
        return switch (world_config.border_axis) {
            case "Y" -> location.getY();
            case "Z" -> location.getZ();
            default -> location.getX();
        };
    }

    public Location addToRelevantPos(Location location, double value) {
        var world_config = getWorldConfig(location.getWorld());
        return switch (world_config.border_axis) {
            case "Y" -> location.add(0, value, 0);
            case "Z" -> location.add(0, 0, value);
            default -> location.add(value, 0, 0);
        };
    }

    public WorldConfig getWorldConfig(World world) {
        return world_configs.get(world.getName());
    }
}
