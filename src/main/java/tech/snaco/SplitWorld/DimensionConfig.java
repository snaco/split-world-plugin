package tech.snaco.SplitWorld;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DimensionConfig implements ConfigurationSerializable {
    String dimension_name;
    boolean enabled;
    String border_axis;
    int border_location;
    String creative_side;
    int border_width;
    boolean replace_border_blocks;

    @Override
    public @NotNull Map<String, Object> serialize() {
        return Map.of(
            "dimension_name", dimension_name,
            "enabled", enabled,
            "border_axis", border_axis,
            "border_location", border_location,
            "creative_side", creative_side,
            "border_width", border_width,
            "replace_border_blocks", replace_border_blocks
        );
    }
}
