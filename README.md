# Split World
This is a plugin that allows for both creative and survival to coexist in the same world. You define an axis and a location and one side will be survival and the other will be creative. The mod automatically manages saving, and loading inventories on either side of the border as well as potion effects.

# GameMode Switching:
* Player will be switched to either survival or creative depending on the side of the buffer zone they are on
* The border axis is padded by the buffer zone, all solid blocks in the buffer zone are converted to bedrock (can be disabled in the config) and liquids are removed
* Sound indicator for when player switches GameMode, can be enabled or disabled with a command (see below)
* When crossing to the survival side, if the player is in the air they will be safely teleported to the closest solid block to their feet
* If a player was flying when crossing to the survival side and the player has an elytra equipped, they will automatically start gliding instead of being teleported to the ground

# Buffer Zone
* Defined by picking an axis, X, Y or Z, picking a border-location which is the value of the specified axis coordinate where the border will be, a border-width which will determine how wide the buffer zone will be
  * i.e: ```border_axis: X, border_location: 0, border_width: 10, creative_side: negative``` will mean the player will be set to creative mode when their X coordinate is less than -5, adventure mode (buffer zone) between X=-5 and X=5, and in survival when X>5
* Players cannot die in the buffer zone
* Players cannot regain health in the buffer zone
* Players will not lose hunger in the buffer zone
* Players are put in adventure mode with flying enabled
* Monsters can not enter the buffer zone, instead they will stop at the border
* Skeletons cannot shoot players in the buffer zone
* Player cannot pick up items if they are in the buffer zone
* Liquids will not flow into the buffer zone
* Blocks in the buffer zone are converted to bedrock (can be disabled in the config)

# Commands
* Players can use /fill /setblock /clone on the creative side, you must give the permission for this command with [LuckPerms](https://luckperms.net/), the plugin will prevent the commands if they contain any blocks not on the creative side and prevent their execution on the survival side.
* /understood: (you should give permission for this to everyone)
  ```
    description: You understand the nature of the split world and don't want to see the welcome message anymore.
    usage: /understood
    permission: split-world.understood
  ```
* /disable-split-world: (you should give this to admins/ops only)
  ```
    description: disables split world game mode switching
    usage: /disable-split-world
    permission: split-world.disable-split-world
  ```
* /enable-split-world: (you should give this to admins/ops only)
  ```
    description: enables split world game mode switching
    usage: /enable-split-world
    permission: split-world.enable-split-world
  ```
* /play-border-sound: (you should give permission for this to everyone)
  ```
    description: plays a sound when you cross a split world border
    usage: /play-border-sound <true|false>
    permission: split-world.play-border-sound
  ```

# Cheat Prevention
* Items dropped into the buffer zone are deleted
* Fluid cannot flow into the buffer zone
* Items/Entities can not be sent through portals on the creative side
  * Re-allow portals to be created on the creative side
* GameMode is set when a player crosses through a portal
* GameMode is set when a player teleports to a location or player (should work with any tp plugin tested with simple tpa)
* Inventories and Potion Effects are saved when entering the buffer zone and restored when you return
* Players are prevented from using a fishing pole to pull items from the creative side to the survival side

# Miscellaneous
* when a player dies they will drop 1/4 of their XP instead of the static amount in vanilla.

# Easter Eggs
* they be a thing, you'll have to find them yourself.