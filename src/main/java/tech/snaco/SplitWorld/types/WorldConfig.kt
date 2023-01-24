package tech.snaco.SplitWorld.types;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class WorldConfig implements ConfigurationSerializable {
    public String world_name;
    public boolean enabled;
    public String border_axis;
    public int border_location;
    public String creative_side;
    public int border_width;
    public boolean replace_border_blocks;
    public boolean no_creative_monsters;

    public WorldConfig(Map<String, Object> map) {
        world_name = (String) map.get("world_name");
        enabled = (boolean)map.get("enabled");
        if (enabled) {
            border_axis = (String) map.get("border_axis");
            border_location = (int) map.get("border_location");
            creative_side = (String) map.get("creative_side");
            border_width = (int) map.get("border_width");
            replace_border_blocks = (boolean) map.get("replace_border_blocks");
            no_creative_monsters = (boolean) map.get("no_creative_monsters");
        }
    }

    public String getWorldName() {
        return this.world_name;
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        return Map.of(
            "world_name", world_name,
            "enabled", enabled,
            "border_axis", border_axis,
            "border_location", border_location,
            "creative_side", creative_side,
            "border_width", border_width,
            "replace_border_blocks", replace_border_blocks,
            "no_creative_monsters", no_creative_monsters
        );
    }
}
