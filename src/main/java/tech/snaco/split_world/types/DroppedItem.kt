package tech.snaco.split_world.types

import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.entity.Item
import org.bukkit.entity.Player

data class DroppedItem(
  val item: Item,
  var previousLocation: Location,
  val originatingSide: GameMode,
  val cheater: Player,
)
