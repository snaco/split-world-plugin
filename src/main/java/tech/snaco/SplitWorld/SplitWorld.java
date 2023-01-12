package tech.snaco.SplitWorld;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import tech.snaco.SplitWorld.utils.ItemStackArrayDataType;
import tech.snaco.SplitWorld.utils.WorldConfig;
import tech.snaco.SplitWorld.utils.mc;

import java.util.Map;
import java.util.stream.Collectors;

@SuppressWarnings({"unchecked", "DataFlowIssue"})
public class SplitWorld extends JavaPlugin implements Listener {
    FileConfiguration config = getConfig();
    GameMode default_game_mode;
    Map<String, WorldConfig> world_configs;
    NamespacedKey chunk_processed_key = new NamespacedKey(this, "split_world_buffered");
    NamespacedKey no_welcome_key = new NamespacedKey(this, "no_welcome_message");

    @Override
    public void onEnable() {
        saveDefaultConfig();
        default_game_mode = mc.gameModeFromString(config.getString("default_game_mode"));
        world_configs = config.getList("world_configs").stream().map(item -> new WorldConfig((Map<String, Object>) item)).collect(Collectors.toMap(WorldConfig::getWorldName, item -> item));
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        var player_pdc = player.getPersistentDataContainer();
        var world_name = player.getWorld().getName();
        var no_welcome = player_pdc.get(no_welcome_key, PersistentDataType.INTEGER);
        if (!world_configs.containsKey(world_name) || !world_configs.get(world_name).enabled) {
            return;
        }
        var world_config = getWorldConfigFromPlayer(player);
        if (no_welcome == null) {
            player.sendMessage(Component.text("Hello " + player.getName() + "! "
                    + "This world is split! You can head over towards the " + world_config.creative_side
                    + " side of the border at " + world_config.border_axis + "=" + world_config.border_location
                    + " to enter the creative side of the world. Your inventory will automatically be saved"
                    + " and loaded whenever you cross the border. Have fun!"));
        }
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
    public void onPlayerMove(PlayerMoveEvent event) {
        var player = event.getPlayer();
        managePlayerGameMode(player);
        convertBufferZoneBlocksAroundPlayer(player);
        if (playerInBufferZone(player)) {
            if (event.getTo().getBlock().getType() != Material.AIR) {
                event.setCancelled(true);
            }
        }

    }

    @EventHandler
    public void onPlayerWorldChange(PlayerChangedWorldEvent event) {
        managePlayerGameMode(event.getPlayer());
    }

    public void convertBufferZoneBlocksAroundPlayer(Player player) {
        var world_config = getWorldConfigFromPlayer(player);
        var world = player.getWorld();
        var player_location = player.getLocation();

        if (!playerInBufferZone(player)) {
            return;
        }

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
                    if (world.getBlockAt(loc).getType() != Material.AIR || world.getBlockAt(loc).getType() == Material.CAVE_AIR) {
                        world.getBlockAt(loc).setType(Material.BEDROCK);
                    }
                }
            }
        }
    }

    public void managePlayerGameMode(Player player) {
        var player_world = player.getLocation().getWorld().getName();
        if (!world_configs.containsKey(player_world) || !world_configs.get(player_world).enabled) {
            switchPlayerGameMode(player, default_game_mode);
            return;
        }

        if (playerInBufferZone(player)) {
            switchPlayerGameMode(player, GameMode.SPECTATOR);
        } else if (playerOnCreativeSide(player)) {
            switchPlayerGameMode(player, GameMode.CREATIVE);
        } else {
            var needs_warp = player.getGameMode() != GameMode.SURVIVAL;
            switchPlayerGameMode(player, GameMode.SURVIVAL);
            if (needs_warp) {
                warpPlayerToGround(player);
            }
        }
    }

    public void warpPlayerToGround(Player player) {
        if (player.getInventory().getChestplate().getType() != Material.ELYTRA) {
            var location = player.getLocation();
            var velocity = player.getVelocity();
            var top = player.getWorld().getHighestBlockAt(location.getBlockX(), location.getBlockZ());
            var pitch = location.getPitch();
            var yaw = location.getYaw();
            var destination = top.getLocation().add(0, 1, 0);
            destination.setPitch(pitch);
            destination.setYaw(yaw);
            player.teleport(destination);
            player.setVelocity(velocity);
        }

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
        var key = getKey(player, game_mode);
        var player_pdc = player.getPersistentDataContainer();
        var new_inv = player_pdc.get(key, new ItemStackArrayDataType());
        if (new_inv != null) {
            player.getInventory().setContents(new_inv);
        }
    }

    public NamespacedKey getKey(Player player, GameMode game_mode) {
        return switch (game_mode) {
            case CREATIVE -> new NamespacedKey(this, player.getName() + "_creative_inv");
            case SURVIVAL -> new NamespacedKey(this, player.getName() + "_survival_inv");
            case ADVENTURE -> new NamespacedKey(this, player.getName() + "_adventure_inv");
            case SPECTATOR -> new NamespacedKey(this, player.getName() + "_spectator_inv");
        };
    }

    public boolean playerOnCreativeSide(Player player) {
        var world_config = getWorldConfigFromPlayer(player);
        if (world_config.creative_side.equals("negative") && playerOnNegativeSideOfBufferZone(player)) {
            return true;
        } else return world_config.creative_side.equals("positive") && playerOnPositiveSideOfBufferZone(player);
    }

    public double getRelevantPlayerPos(Player player) {
        var world_config = getWorldConfigFromPlayer(player);
        return switch (world_config.border_axis) {
            case "Y" -> player.getLocation().getY();
            case "Z" -> player.getLocation().getZ();
            default -> player.getLocation().getX();
        };
    }

    public boolean playerInBufferZone(Player player) {
        var pos = getRelevantPlayerPos(player);
        var world_config = getWorldConfigFromPlayer(player);
        return pos > world_config.border_location - (world_config.border_width / 2.0)
            && pos < world_config.border_location + (world_config.border_width / 2.0);
    }

    public boolean blockInBufferZone(Block block) {
        var world_name = block.getWorld().getName();
        if (!world_configs.containsKey(world_name) || !world_configs.get(world_name).enabled) {
            return false;
        }
        var world_config = getWorldConfigFromChunk(block.getChunk());
        var pos = switch (world_config.border_axis) {
            case "Y" -> block.getY();
            case "Z" -> block.getZ();
            default -> block.getX();
        };
        return pos > world_config.border_location - (world_config.border_width / 2.0)
            && pos < world_config.border_location + (world_config.border_width / 2.0);
    }

    public boolean playerOnNegativeSideOfBufferZone(Player player) {
        var world_config = getWorldConfigFromPlayer(player);
        var pos = getRelevantPlayerPos(player);
        return pos < world_config.border_location - (world_config.border_width / 2.0);
    }
    public boolean playerOnPositiveSideOfBufferZone(Player player) {
        var pos = getRelevantPlayerPos(player);
        var world_config = getWorldConfigFromPlayer(player);
        return pos > world_config.border_location + (world_config.border_width / 2.0);
    }

    public WorldConfig getWorldConfigFromPlayer(Player player) {
        return world_configs.get(player.getWorld().getName());
    }

    public WorldConfig getWorldConfigFromChunk(Chunk chunk) {
        return world_configs.get(chunk.getWorld().getName());
    }
}
