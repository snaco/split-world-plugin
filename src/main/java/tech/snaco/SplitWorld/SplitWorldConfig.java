package tech.snaco.SplitWorld;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SplitWorldConfig implements ConfigurationSerializable {
    String default_game_mode;
    List<DimensionConfig> dimension_configs;

    public SplitWorldConfig(Map<String, Object> map) {
        default_game_mode = (String) map.get("default_game_mode");
        var list = (List<Object>) map.get("dimension_configs");
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        return Map.of(
            "default_game_mode", "survival",
            "dimension_configs", dimension_configs
        );
    }
}
