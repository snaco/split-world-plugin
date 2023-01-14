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
import java.util.Map;
import java.util.Vector;
import java.util.stream.Collectors;

@SuppressWarnings({"unchecked", "DataFlowIssue"})
public class SplitWorld extends JavaPlugin implements Listener {
    FileConfiguration config = getConfig();
    GameMode default_game_mode;
    Map<String, WorldConfig> world_configs;
    NamespacedKey no_welcome_key = new NamespacedKey(this, "no_welcome_message");
    ArrayList<Item> dropped_items = new ArrayList<>();

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
        new BukkitRunnable() {
            @Override
            public void run() {
                if ((long) dropped_items.size() > 0) {
                    var items_to_remove = new ArrayList<Item>();
                    for (Item item : dropped_items) {
                        if (locationInBufferZone(item.getLocation())) {
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
        if (cmd.getName().equalsIgnoreCase("understood")) {
            var player_name = sender.getName();
            var player = sender.getServer().getPlayer(player_name);
            var player_pdc = player.getPersistentDataContainer();
            player_pdc.set(no_welcome_key, PersistentDataType.INTEGER, 1);
            player.sendMessage("You will no longer see the welcome message for split world.");
            return true;
        }
        return false;
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
        convertBufferZoneBlocksAroundPlayer(player);
        var needs_warp = false;
        if (warpIsRecommended(player)) {
            needs_warp = true;
        }
        switchPlayerToConfiguredGameMode(player);
        if (needs_warp) {
            warpPlayerToGround(player);
        }
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
    public void onBlockFromTo(BlockFromToEvent event) {
        if (locationInBufferZone(event.getToBlock().getLocation())) {
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

    public void convertBufferZoneBlocksAroundPlayer(Player player) {
        var x_radius = 5;
        var z_radius = 5;
        var player_location = player.getLocation().clone();
        for (int x = -x_radius; x < x_radius; x++) {
            for (int z = -z_radius; z < z_radius; z++) {
                for (int y = -64; y < 319; y++) {
                    var x_y_z_coordinates = player_location.add(x, 0, z);
                    x_y_z_coordinates.setY(y);
                    if (locationInBufferZone(x_y_z_coordinates)) {
                        if (locationIsTraversable(x_y_z_coordinates)) {
                            x_y_z_coordinates.getBlock().setType(Material.AIR);
                        } else {
                            x_y_z_coordinates.getBlock().setType(Material.BEDROCK);
                        }
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

    public Boolean warpIsRecommended(Player player) {
        return player.getInventory().getChestplate() != null &&
                player.getInventory().getChestplate().getType() == Material.ELYTRA &&
                player.getGameMode() != GameMode.SURVIVAL
                && !playerOnCreativeSide(player)
                && !playerInBufferZone(player);
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

    public void loadPlayerInventory(Player player, GameMode game_mod warpPlayerToGround(player);e) {
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
