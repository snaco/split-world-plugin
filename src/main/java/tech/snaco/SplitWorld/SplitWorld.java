package tech.snaco.SplitWorld;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
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
    NamespacedKey chunkProcessedKey = new NamespacedKey(this, "split_world_buffered");

    @Override
    public void onEnable() {
        saveDefaultConfig();
        default_game_mode = mc.gameModeFromString(config.getString("default_game_mode"));
        world_configs = config.getList("world_configs").stream().map(item -> new WorldConfig((Map<String, Object>) item)).collect(Collectors.toMap(WorldConfig::getWorldName, item -> item));
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.getPlayer().sendMessage(Component.text("Hello " + event.getPlayer().getName()));
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        var player = event.getPlayer();
        managePlayerGameMode(player);
        convertBufferZoneBlocksAroundPlayer(player);
    }

    @EventHandler
    public void onPlayerWorldChange(PlayerChangedWorldEvent event) {
        managePlayerGameMode(event.getPlayer());
    }

//    @EventHandler
//    public void onChunkLoad(ChunkLoadEvent event) {
//        var chunk = event.getChunk();
//        var processed = chunk.getPersistentDataContainer().get(chunkProcessedKey, PersistentDataType.INTEGER);
//        if (processed != null) {
//            return;
//        }
//        for (int x = chunk.getX() * 16; x < chunk.getX() + 15; x++) {
//            for (int z = chunk.getZ() * 16; z < chunk.getZ() + 15; z++) {
//                for (int y = -64; y < 319; y++) {
//                    Location loc = new Location(chunk.getWorld(), x, y, z);
//                    var block = chunk.getWorld().getBlockAt(loc);
//                    if (blockInBufferZone(block) && block.getType() != Material.AIR) {
//                        block.setType(Material.BEDROCK);
//                    }
//                }
//            }
//        }
//        chunk.getPersistentDataContainer().set(chunkProcessedKey, PersistentDataType.INTEGER, 1);
//    }

    /* Helper Methods */

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
            switchPlayerGameMode(player, GameMode.SURVIVAL);
            warpPlayerToGround(player);
        }
    }

    public void warpPlayerToGround(Player player) {

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
