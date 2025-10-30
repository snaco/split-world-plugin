# Split World

This is a plugin that allows for both creative and survival to coexist in the same world. You define an axis and a
location and one side will be survival and the other will be creative. The mod automatically manages saving, and loading
inventories on either side of the border as well as potion effects.

# GameMode Switching:

* Game mode can be set per world
* A world can have two game modes with a border dividing the world into a creative side and a configurable default game mode 
* Player's inventory and potion effects are managed per-game mode
* Player will be switched to either survival or creative depending on the side of the buffer zone they are on
* The border axis is padded by the buffer zone, the boundary is visualized with optional particle effects to create a 
visible wall and/or solid blocks will be rendered as if they were black concrete
* Sound indicator for when player switches GameMode, can be enabled or disabled with a command (see below)
* Many methods to prevent cheating are included (see below)

# Buffer Zone

* Defined by border axis and location, with `border_axis: 'X'` and `border_location: 0` the dividing border will be placed
at x=0, resulting in a border perpendicular to the x-axis
* Players cannot die in the buffer zone
* Players cannot regain health in the buffer zone
* Players will not lose hunger in the buffer zone
* Players are put in adventure mode with flying enabled
* Monsters cannot enter the buffer zone, instead they will stop at the border
* Monsters cannot target players in the buffer zone
* Players cannot pick up items if they are in the buffer zone
* Players cannot interact with chests in the buffer zone, or with chests that are not on the same side as the player 
* Liquids will not flow into the buffer zone
* Vehicles cannot enter the buffer zone

# Commands

* Players can use /fill /setblock /clone on the creative side, you must give the permission for this command
  with [LuckPerms](https://luckperms.net/), the plugin will prevent the commands if they contain any blocks not on the
  creative side and prevent their execution on the survival side.
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

* When leaving either side of the border, the player's inventory, active potion effects, and ender chest contents are
  saved. Then their saved data for the other side is loaded.
* Items dropped in the buffer zone are returned to the user's inventory
* Fluid cannot flow into the buffer zone
* Items/Entities can not be sent through portals on the creative side
* GameMode is set when a player crosses through a portal
* GameMode is set when a player teleports to a location or player (should work with any tp plugin tested with simple
  tpa)
* Players are prevented from using a fishing pole to pull items from the creative side to the survival side.
* Players cannot interact with chests that are not on the same side as them

# Config Examples

## Full Config

```yaml
default_game_mode: "survival"     # global default 
disable_welcome_message: false    # globally disable welcome message
enable_xp_mod: false              # changes XP loss behavior on death
xp_loss_percentage: 25.0          # percentage of XP lost on death
custom_respawn: false             # customize global respawn location
respawn_location:                 # 
  world: "split_world"            # which world players should spawn in (if they have not set their spawn point) 
  x: 140.0                        # x coordinate
  y: 53.0                         # y coordinate
  z: -64.0                        # z coordinate
  yaw: 0.0                        # player facing direction
  pitch: 0.0                      # player facing direction
border_particles: true            # toggles rendering particle wall for boundary
border_blocks: true               # toggles client-side buffer-zone block replacement, this will not alter your world
enable_easter_eggs: false         # had some fun while coding, enable this to see if you can find them all
world_configs:                    # list of worlds to configure
  - world_name: "world"           # name of the world
    enabled: true                 # should the world be split
    default_game_mode: "survival" # optional override for the global default_game_mode, this can be set even if the world is not split
    border_axis: "X"              # axis the border is placed on
    border_location: 0            # location on the axis to place the border
    creative_side: "negative"     # can be positive or negative. when split, determines if the creative side is for coordinates less than border_location or greater than border_location
    border_width: 5               # how wide the buffer zone should be. some cheat prevention methods are less reliable for smaller buffer zones
    no_creative_monsters: true    # stops monsters from spawining on the creative side
```

## Minimal Per-World Example 

```yaml
default_game_mode: "survival"
world_configs:
  - world_name: "creative_world"
    default_game_mode: "creative"
  - world_name: "adventure_world"
    default_game_mode: "adventure"
```

## Minimal Split World Example

```yaml
default_game_mode: "survival"
world_configs:
  - world_name: "split_world"
    enabled: true
```

## Minimal Mixed Example

```yaml
default_game_mode: "spectator"
world_configs:
  - world_name: "split_world"
    enabled: true
    default_game_mode: "survival"
  - world_name: "world"
    default_game_mode: "survival"
  - world_name: "world_nether"
    default_game_mode: "survival"
  - world_name: "world_the_end"
    default_game_mode: "survival"
```