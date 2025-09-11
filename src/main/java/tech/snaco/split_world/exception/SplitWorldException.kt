package tech.snaco.split_world.exception

import com.destroystokyo.paper.exception.ServerPluginException
import tech.snaco.split_world.SplitWorldPlugin

open class SplitWorldException(message: String) : ServerPluginException(message, null, SplitWorldPlugin())
open class SplitWorldConfigException(message: String) : SplitWorldException(message)
class MissingWorldConfigException(message: String) : SplitWorldConfigException(message)