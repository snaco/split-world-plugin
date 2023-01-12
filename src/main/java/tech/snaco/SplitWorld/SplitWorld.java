package tech.snaco.SplitWorld;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class SplitWorld extends JavaPlugin implements Listener {
    FileConfiguration config = getConfig();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.getPlayer().sendMessage(Component.text("Hello " + event.getPlayer().getName()));
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        var player_location = event.getPlayer().getLocation();
        var player_dimension = player_location.getWorld().getName();
        var survival_key = new NamespacedKey(this, event.getPlayer().getName() + "_survival_inv");
        var creative_key = new NamespacedKey(this, event.getPlayer().getName() + "_creative_inv");
        var player_pdc = event.getPlayer().getPersistentDataContainer();
        var player_inv = event.getPlayer().getInventory();

        if (player_location.getX() < 0) {
            player_pdc.set(survival_key, new InventoryDataType(), event.getPlayer().getInventory());
            player_inv.clear();
            event.getPlayer().setGameMode(GameMode.CREATIVE);
            var creative_inv = event.getPlayer().getPersistentDataContainer().get(creative_key, new InventoryDataType());
            player_inv.setContents((creative_inv.getContents()));
        } else {
            event.getPlayer().getPersistentDataContainer().set(creative_key, new InventoryDataType(), event.getPlayer().getInventory());
            player_inv.clear();
            event.getPlayer().setGameMode(GameMode.SURVIVAL);
            var survival_inv = event.getPlayer().getPersistentDataContainer().get(survival_key, new InventoryDataType());
            player_inv.setContents((survival_inv.getContents()));
        }
    }
}
