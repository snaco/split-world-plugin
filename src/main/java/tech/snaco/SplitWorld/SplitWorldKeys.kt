package tech.snaco.SplitWorld

import org.bukkit.GameMode
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player

class SplitWorldKeys(private var split_world: SplitWorld) {
    var no_welcome: NamespacedKey = NamespacedKey(split_world, "no_welcome_message")
    var split_world_disabled: NamespacedKey = NamespacedKey(split_world, "split_world_disabled")
    var first_join: NamespacedKey = NamespacedKey(split_world, "split_world_first_join")
    var first_fish_attempt: NamespacedKey = NamespacedKey(split_world, "first_fish_attempt")
    var spawn_builder: NamespacedKey = NamespacedKey(split_world, "spawn_builder")
    var play_border_sound: NamespacedKey = NamespacedKey(split_world, "play_border_sound")
    var sleep_in_nether_score: NamespacedKey = NamespacedKey(split_world, "sleep_in_nether_score")
    var nether_egg: NamespacedKey = NamespacedKey(split_world, "bee_movie_score")
    var nether_sleep_throttle: NamespacedKey = NamespacedKey(split_world, "nether_sleep_throttle")
    var nether_sleep_tock: NamespacedKey = NamespacedKey(split_world, "nether_sleep_tock")

    fun getPlayerInventoryKey(player: Player, game_mode: GameMode): NamespacedKey {
        return when (game_mode) {
            GameMode.CREATIVE -> NamespacedKey(split_world, player.name + "_creative_inv")
            GameMode.SURVIVAL -> NamespacedKey(split_world, player.name + "_survival_inv")
            GameMode.ADVENTURE -> NamespacedKey(split_world, player.name + "_adventure_inv")
            GameMode.SPECTATOR -> NamespacedKey(split_world, player.name + "_spectator_inv")
        }
    }
    fun getPlayerEffectsKey(player: Player, game_mode: GameMode): NamespacedKey {
        return when (game_mode) {
            GameMode.CREATIVE -> NamespacedKey(split_world, player.name + "_creative_eff")
            GameMode.SURVIVAL -> NamespacedKey(split_world, player.name + "_survival_eff")
            GameMode.ADVENTURE -> NamespacedKey(split_world, player.name + "_adventure_eff")
            GameMode.SPECTATOR -> NamespacedKey(split_world, player.name + "_spectator_eff")
        }
    }
}
