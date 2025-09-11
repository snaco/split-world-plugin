package tech.snaco.split_world.utils

import org.bukkit.Bukkit
import org.bukkit.Server
import org.bukkit.plugin.ServicePriority
import tech.snaco.split_world.SplitWorldPlugin
import tech.snaco.split_world.config.SplitServer

fun Server.loadSplitWorldConfig(plugin: SplitWorldPlugin) {
  servicesManager.register(
    SplitServer::class.java,
    SplitServer(plugin),
    plugin,
    ServicePriority.Normal
  )
}

inline fun <reified T : Any> service(): T = Bukkit
  .getServicesManager()
  .load(T::class.java) ?: error("Service ${T::class.java.simpleName} not found!")

fun splitWorldConfig(): SplitServer = Bukkit
  .getServicesManager()
  .load(SplitServer::class.java) ?: error("Split world config not registered!")
