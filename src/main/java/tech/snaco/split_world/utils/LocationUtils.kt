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
  return if (this.world
      .splitConfig()
      .creativeSide() == "negative" && this.onNegativeSideOfBuffer()
  ) {
    true
  } else this.world
    .splitConfig()
    .creativeSide() == "positive" && this.onPositiveSideOfBuffer()
}

fun Location.inBufferZone(): Boolean {
//  val worldConfig = this.world.splitConfig()
//  val pos = this.getRelevantPos()
//  val borderLocation = worldConfig.borderLocation()
  return !onNegativeSideOfBuffer() && !onPositiveSideOfBuffer()
//  return (pos > borderLocation - worldConfig.borderWidth() && pos <= borderLocation + worldConfig.borderWidth())
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
  val pos = this.getRelevantPos()
  val worldConfig = this.world.splitConfig()
  return pos <= worldConfig.borderLocation() - worldConfig.borderWidth() - extra
}

fun Location.onPositiveSideOfBuffer(): Boolean {
  return onPositiveSideOfBuffer(0.0)
}

fun Location.onPositiveSideOfBuffer(extra: Double): Boolean {
  val pos = this.getRelevantPos()
  val worldConfig = this.world.splitConfig()
  return pos > worldConfig.borderLocation() + worldConfig.borderWidth() + extra
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