package tech.snaco.split_world.utils

import org.bukkit.Location
import org.bukkit.block.Block

val Block.inBufferZone: Boolean
  get() = !location.onNegativeSideOfBuffer() && !location.onPositiveSideOfBuffer(-1.0)

val Block.onPositiveSide: Boolean
  get() = location.onPositiveSideOfBuffer(-1.0)

val Block.onNegativeSide: Boolean
  get() = location.onNegativeSideOfBuffer()

val Block.onDefaultSide: Boolean
  get() = location.onNegativeSideOfBuffer() && world
    .splitConfig()
    .creativeSide() != "negative"
      || location.onPositiveSideOfBuffer(-1.0) && world
    .splitConfig()
    .creativeSide() != "positive"

val Block.onCreativeSide: Boolean
  get() = location.onNegativeSideOfBuffer() && world
    .splitConfig()
    .creativeSide() == "negative"
      || location.onPositiveSideOfBuffer(-1.0) && world
    .splitConfig()
    .creativeSide() == "positive"

fun Block.onDifferentSide(location: Location): Boolean =
  (onCreativeSide && !location.onCreativeSide()) || (onDefaultSide && !location.onDefaultSide())

