package tech.snaco.split_world.utils

import org.bukkit.Location
import java.util.*

fun Location.onDefaultSide(): Boolean {
  return if (this.world
      .splitConfig()
      .creativeSide() == "negative" && this.onPositiveSideOfBuffer()
  ) {
    true
  } else this.world
    .splitConfig()
    .creativeSide() == "positive" && this.onNegativeSideOfBuffer()
}

fun Location.onDifferentSide(otherLocation: Location): Boolean {
  val acrossDefaultSide = onDefaultSide() && !otherLocation.onDefaultSide()
  val acrossBufferZone = inBufferZone() && !otherLocation.inBufferZone()
  val acrossCreativeSide = onCreativeSide() && !otherLocation.onCreativeSide()
  return acrossCreativeSide || acrossDefaultSide || acrossBufferZone
}

fun Location.onCreativeSide(): Boolean {
  return onCreativeSide(0.0)
}

fun Location.onCreativeSide(offset: Double): Boolean {
  return if (this.world
      .splitConfig()
      .creativeSide() == "negative" && this.onNegativeSideOfBuffer(offset)
  ) {
    true
  } else this.world
    .splitConfig()
    .creativeSide() == "positive" && this.onPositiveSideOfBuffer(offset)
}

fun Location.inBufferZone(): Boolean {
  return !onNegativeSideOfBuffer() && !onPositiveSideOfBuffer()
}

fun Location.inBufferZone(extra: Double): Boolean {
  return !onNegativeSideOfBuffer(extra) && !onPositiveSideOfBuffer(extra)
}

fun Location.getRelevantPos(): Double {
  return when (this.world
    .splitConfig()
    .borderAxis()
    .uppercase(Locale.getDefault())) {
    "Y" -> this.y
    "Z" -> this.z
    else -> this.x
  }
}

fun Location.onNegativeSideOfBuffer(): Boolean {
  return onNegativeSideOfBuffer(0.0)
}

fun Location.onNegativeSideOfBuffer(extra: Double): Boolean {
  return getRelevantPos() < world.splitConfig().borderLocation() - world.splitConfig().borderWidth() - extra
}

fun Location.onPositiveSideOfBuffer(): Boolean {
  return onPositiveSideOfBuffer(0.0)
}

fun Location.onPositiveSideOfBuffer(extra: Double): Boolean {
  return getRelevantPos() > world.splitConfig().borderLocation() + world.splitConfig().borderWidth() + extra
}

fun Location.isTraversable(): Boolean {
  return !this.world.getBlockAt(this).type.isSolid
}

fun Location.addAcrossSplitAxis(value: Double): Location {
  when (this.world
    .splitConfig()
    .borderAxis()
    .uppercase(Locale.getDefault())) {
    "Y" -> y += value
    "Z" -> z += value
    else -> x += value
  }
  return this
}

fun Location.closestSolidBlockBelow(): Location {
  val loc = this.clone()
  var blockFound = false
  while (!blockFound) {
    loc.add(0.0, -1.0, 0.0)
    if (loc.block.isSolid) {
      blockFound = true
    }
    if (loc.y <= -64) break
  }
  return loc
}