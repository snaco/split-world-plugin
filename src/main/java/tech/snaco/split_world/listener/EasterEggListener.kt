package tech.snaco.split_world.listener

import io.papermc.paper.event.player.PlayerDeepSleepEvent
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerBedEnterEvent
import org.bukkit.event.player.PlayerBedEnterEvent.BedEnterResult
import org.bukkit.event.player.PlayerBedLeaveEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitRunnable
import tech.snaco.split_world.extras.easter_eggs.Messages
import tech.snaco.split_world.utils.getPdcInt
import tech.snaco.split_world.utils.splitWorldConfig

class EasterEggListener(plugin: Plugin) : Listener {
  private val playersSleepingInNether = HashSet<Player>()
  private val enabled = splitWorldConfig().easterEggsEnabled()

  init {
    if (enabled) {
      object : BukkitRunnable() {
        override fun run() {
          Messages.runNetherSleepTask(playersSleepingInNether, splitWorldConfig().keys)
        }
      }.runTaskTimer(plugin, 20L, 1L)
    }
  }

  @EventHandler
  fun onDeath(event: PlayerDeathEvent) {
    if (enabled) {
      playersSleepingInNether.remove(event.player)
    }
  }

  @EventHandler
  fun itemInteract(event: PlayerInteractEvent) {
    if (enabled && event.item?.type == Material.DIAMOND) {
      event.player.playSound(event.player.location, Sound.ENTITY_GENERIC_EAT, 1.0f, 1.0f)
      event.player.inventory
        .getItem(event.player.inventory.indexOf(event.item!!))!!
        .subtract()
      event.player.giveExpLevels(2)
      event.player.sendMessage("You ate a diamond you absolute madlad!")
    }
  }

  @EventHandler
  fun enterBed(event: PlayerBedEnterEvent) {
    if (enabled) {
      val netherEggComplete = event.player.getPdcInt(splitWorldConfig().keys.netherEgg)
      if (netherEggComplete != null && netherEggComplete >= 1561) {
        return
      }
      if (event.bedEnterResult == BedEnterResult.NOT_POSSIBLE_HERE) {
        event.setUseBed(Event.Result.ALLOW)
        event.player.sendMessage("Didn't expect that did you?")
      }
    }
  }

  @EventHandler
  fun enterDeepSleep(event: PlayerDeepSleepEvent) {
    if (splitWorldConfig().easterEggsEnabled()) {
      if (event.player.world.name.endsWith("_nether")) {
        playersSleepingInNether.add(event.player)
        event.isCancelled = true
      }
    }
  }

  @EventHandler
  fun leaveBed(event: PlayerBedLeaveEvent) {
    if (splitWorldConfig().easterEggsEnabled()) {
      if (event.player.world.name.endsWith("_nether")) {
        playersSleepingInNether.remove(event.player)
      }
    }
  }
}
