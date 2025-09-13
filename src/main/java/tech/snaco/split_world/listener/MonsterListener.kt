package tech.snaco.split_world.listener

import com.destroystokyo.paper.event.entity.EndermanEscapeEvent
import io.papermc.paper.event.entity.EntityMoveEvent
import org.bukkit.entity.Monster
import org.bukkit.entity.Player
import org.bukkit.entity.Skeleton
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.event.entity.EntityShootBowEvent
import org.bukkit.event.entity.EntityTargetLivingEntityEvent
import tech.snaco.split_world.utils.*

class MonsterListener : Listener {

  @EventHandler
  fun onCreatureSpawn(event: CreatureSpawnEvent) {
    val world = event.location.world
    if (!world.isSplit()) {
      return
    }
    if (!event.location.onDefaultSide()
      && event.spawnReason == CreatureSpawnEvent.SpawnReason.NATURAL
      && world
        .splitConfig()
        .noCreativeMonsters()
      && event.entity is Monster
    ) {
      event.isCancelled = true
    }
  }

  @EventHandler
  fun onTarget(event: EntityTargetLivingEntityEvent) {
    if (!event.entity.world.isSplit()) {
      return
    }
    if (event.entity.location.onDifferentSide(event.target!!.location)) {
      event.isCancelled = true
    }
  }

  @EventHandler
  fun onEntityMove(event: EntityMoveEvent) {
    if (!event.entity.world.isSplit()) {
      return
    }
    // only do this if players are online
    if (event.entity.server.onlinePlayers.isEmpty()) {
      return
    }
    // don't do for players (JIC)
    if (event.entity is Player) {
      return
    }
    // stop no crossing unless you are a player
    if (event.to.inBufferZone(0.5)) {
      event.isCancelled = true
    }
    // only remove monsters on the creative side
    if (event.entity is Monster
      && event.to.onCreativeSide()
      && event.entity.world
        .splitConfig()
        .noCreativeMonsters()
    ) {
      event.entity.remove()
    }
  }

  @EventHandler
  fun onEndermanEscape(event: EndermanEscapeEvent) {
    if (!event.entity.world.isSplit()) {
      return
    }
    if (event.entity.location.onCreativeSide()) {
      event.isCancelled = true
    }
  }

  @EventHandler
  fun onSkeletonShoots(event: EntityShootBowEvent) {
    if (event.entity is Skeleton) {
      val skeleton = event.entity as Skeleton
      if (skeleton.target != null && !skeleton.target!!.location.onDefaultSide()) {
        event.isCancelled = true
      }
    }
  }
}
