package tech.snaco.SplitWorld.utils;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public class SplitWorldConfig implements ConfigurationSerializable {
    String default_game_mode;
    List<WorldConfig> dimension_configs;

    public SplitWorldConfig(Map<String, Object> map) {
        default_game_mode = (String) map.get("default_game_mode");
        var list = (List<Object>) map.get("world_configs");
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        return Map.of(
            "default_game_mode", "survival",
            "world_configs", dimension_configs
        );
    }
}
