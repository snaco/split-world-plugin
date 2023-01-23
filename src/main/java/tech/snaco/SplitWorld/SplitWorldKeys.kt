package tech.snaco.SplitWorld

import org.bukkit.NamespacedKey

class SplitWorldKeys(split_world: SplitWorld) {
    var noWelcome: NamespacedKey = NamespacedKey(split_world, "no_welcome_message")
    var splitWorldDisabled: NamespacedKey = NamespacedKey(split_world, "split_world_disabled")
    var firstJoin: NamespacedKey = NamespacedKey(split_world, "split_world_first_join")
    var firstFishAttempt: NamespacedKey = NamespacedKey(split_world, "first_fish_attempt")
    var competitionEnded: NamespacedKey = NamespacedKey(split_world, "competition_ended")
    var competitionParticipant: NamespacedKey = NamespacedKey(split_world, "competition_participant")
    var receivedRewards: NamespacedKey = NamespacedKey(split_world, "received_rewards")
    var spawnBuilder: NamespacedKey = NamespacedKey(split_world, "spawn_builder")
    var playBorderSound: NamespacedKey = NamespacedKey(split_world, "play_border_sound")
}
