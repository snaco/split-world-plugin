package tech.snaco.split_world

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitRunnable
import tech.snaco.split_world.utils.inBufferZone
import tech.snaco.split_world.utils.isSplit
import tech.snaco.split_world.utils.splitConfig
import tech.snaco.split_world.utils.splitWorldPlugin

class BufferZoneVisualizer(plugin: Plugin) : Listener {
  private var offsetCycle = 0
  private val offsets = listOf(0.0, 0.25, -0.25, 0.5, -0.5, 0.75, -0.75)
  
  init {
    object : BukkitRunnable() {
      override fun run() {
        if (!splitWorldPlugin().splitServerConfig.borderParticles()) return

        val currentOffset = offsets[offsetCycle]
        offsetCycle = (offsetCycle + 1) % offsets.size

        for (world in Bukkit.getWorlds()) {
          if (!world.isSplit()) continue

          val borderLocation = world.splitConfig().borderLocation()
          val borderWidth = world.splitConfig().borderWidth()
          val axis = world.splitConfig().borderAxis().uppercase()

          for (player in world.players) {
            val playerLoc = player.location

            val distanceToBuffer = when (axis) {
              "X" -> kotlin.math.abs(playerLoc.x - borderLocation)
              "Z" -> kotlin.math.abs(playerLoc.z - borderLocation)
              else -> continue
            }

            // client side limitation, particles are not rendered after about 30 blocks
            if (distanceToBuffer > 30 + borderWidth) continue

            if (playerLoc.inBufferZone()) {
              renderBufferEdge(player, borderLocation - borderWidth, axis, currentOffset)
              renderBufferEdge(player, borderLocation + borderWidth, axis, currentOffset)
            } else {
              val playerPosition = when (axis) {
                "X" -> playerLoc.x
                "Z" -> playerLoc.z
                else -> continue
              }
              if (playerPosition < borderLocation - borderWidth) {
                renderBufferEdge(player, borderLocation - borderWidth, axis, currentOffset)
              } else if (playerPosition > borderLocation + borderWidth) {
                renderBufferEdge(player, borderLocation + borderWidth, axis, currentOffset)
              }
            }
          }
        }
      }
    }.runTaskTimer(plugin, 0, 10L)
  }

  private fun renderBufferEdge(
    player: Player,
    position: Double,
    axis: String,
    offset: Double,
  ) {
    val particleRadius = 15.0
    val playerLoc = player.location
    val centerHeight = player.eyeLocation.y
    when (axis) {
      "X" -> {
        var y = centerHeight - particleRadius + offset
        while (y <= centerHeight + particleRadius) {
          var z = playerLoc.z - particleRadius + offset
          while (z <= playerLoc.z + particleRadius) {
            val loc = Location(player.world, position, y, z)
            if (loc.distance(playerLoc) <= 32) {
              player.spawnParticle(Particle.PORTAL, loc, 1, 0.0, 0.0, 0.0, 0.0)
            }
            z += 1
          }
          y += 1
        }
      }
      "Z" -> {
        var y = centerHeight - particleRadius + offset
        while (y <= centerHeight + particleRadius) {
          var x = playerLoc.x - particleRadius + offset
          while (x <= playerLoc.x + particleRadius) {
            val loc = Location(player.world, x, y, position)
            if (loc.distance(playerLoc) <= 32) {
              player.spawnParticle(Particle.PORTAL, loc, 1, 0.0, 0.0, 0.0, 0.0)
            }
            x += 1
          }
          y += 1
        }
      }
    }
  }
}
