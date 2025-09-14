package tech.snaco.split_world.event

import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.bukkit.event.player.PlayerEvent


open class SplitWorldGameModeChangeEvent(
  player: Player,
  val gameMode: GameMode
) : PlayerEvent(player) {
  val HANDLER_LIST = HandlerList()

  override fun getHandlers(): HandlerList {
    return HANDLER_LIST
  }
}

class BeforeGameModeChangeEvent(
  player: Player,
  newGameMode: GameMode
) : SplitWorldGameModeChangeEvent(player, newGameMode) {}

class AfterGameModeChangeEvent(
  player: Player,
  oldGameMode: GameMode,
) : SplitWorldGameModeChangeEvent(player, oldGameMode) {}