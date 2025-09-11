package tech.snaco.split_world

import org.bukkit.NamespacedKey

class SplitWorldKeys(splitWorld: SplitWorldPlugin) {
  val noWelcome: NamespacedKey = NamespacedKey(splitWorld, "no_welcome_message")
  val splitWorldDisabled: NamespacedKey = NamespacedKey(splitWorld, "split_world_disabled")
  val firstJoin: NamespacedKey = NamespacedKey(splitWorld, "split_world_first_join")
  val firstFishAttempt: NamespacedKey = NamespacedKey(splitWorld, "first_fish_attempt")
  val spawnBuilder: NamespacedKey = NamespacedKey(splitWorld, "spawn_builder")
  val playBorderSound: NamespacedKey = NamespacedKey(splitWorld, "play_border_sound")
  val sleepInNetherScore: NamespacedKey = NamespacedKey(splitWorld, "sleep_in_nether_score")
  val netherEgg: NamespacedKey = NamespacedKey(splitWorld, "bee_movie_score")
  val netherSleepThrottle: NamespacedKey = NamespacedKey(splitWorld, "nether_sleep_throttle")
  val netherSleepTock: NamespacedKey = NamespacedKey(splitWorld, "nether_sleep_tock")
}
