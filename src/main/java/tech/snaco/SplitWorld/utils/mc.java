package tech.snaco.SplitWorld.utils;

import org.bukkit.GameMode;

public class mc {
    public static GameMode gameModeFromString(String game_mode) {
        return switch (game_mode) {
            case "creative" -> GameMode.CREATIVE;
            case "adventure" -> GameMode.ADVENTURE;
            case "spectator" -> GameMode.SPECTATOR;
            default -> GameMode.SURVIVAL;
        };
    }
}
