package tech.snaco.split_world

import org.bukkit.GameMode
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player

class SplitWorldKeys(private var splitWorld: SplitWorld) {
  val noWelcome: NamespacedKey = NamespacedKey(splitWorld, "no_welcome_message")
  var splitWorldDisabled: NamespacedKey = NamespacedKey(splitWorld, "split_world_disabled")
  var firstJoin: NamespacedKey = NamespacedKey(splitWorld, "split_world_first_join")
  var firstFishAttempt: NamespacedKey = NamespacedKey(splitWorld, "first_fish_attempt")
  var spawnBuilder: NamespacedKey = NamespacedKey(splitWorld, "spawn_builder")
  var playBorderSound: NamespacedKey = NamespacedKey(splitWorld, "play_border_sound")
  var sleepInNetherScore: NamespacedKey = NamespacedKey(splitWorld, "sleep_in_nether_score")
  var netherEgg: NamespacedKey = NamespacedKey(splitWorld, "bee_movie_score")
  var netherSleepThrottle: NamespacedKey = NamespacedKey(splitWorld, "nether_sleep_throttle")
  var netherSleepTock: NamespacedKey = NamespacedKey(splitWorld, "nether_sleep_tock")

  fun getPlayerInventoryKey(player: Player): NamespacedKey {
    return when (player.gameMode) {
      GameMode.CREATIVE -> NamespacedKey(splitWorld, player.name + "_creative_inv")
      GameMode.SURVIVAL -> NamespacedKey(splitWorld, player.name + "_survival_inv")
      GameMode.ADVENTURE -> NamespacedKey(splitWorld, player.name + "_adventure_inv")
      GameMode.SPECTATOR -> NamespacedKey(splitWorld, player.name + "_spectator_inv")
    }
  }

  fun getPlayerEffectsKey(player: Player): NamespacedKey {
    return when (player.gameMode) {
      GameMode.CREATIVE -> NamespacedKey(splitWorld, player.name + "_creative_eff")
      GameMode.SURVIVAL -> NamespacedKey(splitWorld, player.name + "_survival_eff")
      GameMode.ADVENTURE -> NamespacedKey(splitWorld, player.name + "_adventure_eff")
      GameMode.SPECTATOR -> NamespacedKey(splitWorld, player.name + "_spectator_eff")
    }
  }
}
