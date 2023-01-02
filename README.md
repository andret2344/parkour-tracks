# atsParkour

[TOC]

## Description

This plugin allows to create multiple parkours (obstacle paths that players
pass) on the server. Each parkour can have checkpoints (regions that save the
progress), walls (regions that force teleporting back), results stored in a
database and many more.

## Details

Each parkour is completely independent on others. The only setting that is
shared across all parkours is the lobby, to where the player is teleported after
finishing a parkour or leaving on demand (R-click on designed item in the
hot-bar).

### Settings

Each parkour can be configured in many ways, here is the description of all
possible settings.

#### Basic settings

This section describes settings that don't require any additional piece of code.

| setting           | default  | description                                                                                     |
|-------------------|----------|-------------------------------------------------------------------------------------------------|
| `springForced`    | `false`  | Running always turns the sprint on                                                              |
| `alwaysSpawn`     | `false`  | Hitting a wall will always teleport to spawn, ignoring checkpoints                              |
| `savingResults`   | `true`   | Results will be persisted in a database.                                                        |
| `damageAllowed`   | `false`  | Players will take damage in all usual ways.                                                     |
| `boat`            | `false`  | Players will always spawn as a passenger of a boat that can't be left.                          |
| `modifyInventory` | `true`   | Entering and leaving a parkour will clear the inventory and place special items in the hot-bar. |
| `type`            | `SERVER` | From who and from whom is the parkour. Possible are: `SERVER`, `TRAINING`, `PLAYERS`.           |

#### Economy-related settings

This section describes settings that require the FinanceProvider implementation.
See [Extending -> Financial extension](#financial-extension) for more details.

| setting  | default | description                                                                                   |
|----------|---------|-----------------------------------------------------------------------------------------------|
| `fee`    | `0`     | How much does it cost to enter the parkour 1 time (will be denied if player can't afford it). |
| `reward` | `0`     | How much will player get for finishing the parkour.                                           |

#### Rank-related settings

This section describes settings that require the RankProvider implementation.
See [Extending -> Financial extension](#financial-extension) for more details.

| setting   | default | description                                                      |
|-----------|---------|------------------------------------------------------------------|
| `vipOnly` | `false` | Only a user that has the vip rank assigned can join the parkour. |

#### QuickParkour-related settings

This section describes settings that are used in
the [atsQuickParkour](https://gitlab.com/andret-tools-system/spigot/ats-quick-parkour/)
plugin. See [atsQuickParkour](#atsquickparkour) for more details.

| setting      | default | description                                                                         |
|--------------|---------|-------------------------------------------------------------------------------------|
| `difficulty` | `1`     | The difficulty level of the parkour.                                                |
| `color`      | `WHITE` | In the inventory GUI, what color should be the wool that is assigned to the parkour |

### Extending

This plugin can be extended in many ways. All sample extensions are
available [here](https://gitlab.com/andret-tools-system/spigot/ats-parkour/-/tree/master/sample/src/main/java/eu/andret/ats/parkour/sample).

#### Financial extension

If parkours should have a possibility to require fees or give financial rewards,
the [FinancialProvider](https://gitlab.com/andret-tools-system/spigot/ats-parkour/-/blob/master/src/main/java/eu/andret/ats/parkour/api/FinancialProvider.java)
class must be inherited and its implementation has to be injected into
the [plugin's main class](https://gitlab.com/andret-tools-system/spigot/ats-parkour/-/blob/master/src/main/java/eu/andret/ats/parkour/ParkourPlugin.java).

#### Rank extension

If parkours should have the possibility to be limited so that only players with
VIP rank can enter them,
the [RankProvider](https://gitlab.com/andret-tools-system/spigot/ats-parkour/-/blob/master/src/main/java/eu/andret/ats/parkour/api/RankProvider.java)
class must be inherited and its implementation has to be injected into
the [plugin's main class](https://gitlab.com/andret-tools-system/spigot/ats-parkour/-/blob/master/src/main/java/eu/andret/ats/parkour/ParkourPlugin.java).

#### Custom parkour metadata

If parkour should be extended of a custom metadata, it's
class [ParkourGame](https://gitlab.com/andret-tools-system/spigot/ats-parkour/-/blob/master/src/main/java/eu/andret/ats/parkour/parkour/ParkourGame.java)
must be inherited with custom implementation. Then the class creators (with and
without the data) must be injected in two different places (see below and
the [sample code](https://gitlab.com/andret-tools-system/spigot/ats-parkour/-/tree/master/sample/src/main/java/eu/andret/ats/parkour/sample)).

```java
public class MyPlugin extends JavaPlugin {
	@Override
	public void onEnable() {
		getPlugin(ParkourPlugin.class).getParkourManager().setCreator(SampleParkour::new);
		getPlugin(ParkourPlugin.class).getJsonIO().set(new SampleParkourInstanceCreator(SampleParkour.class, this));
	}
}
```

#### atsQuickParkour

[atsQuickParkour](https://gitlab.com/andret-tools-system/spigot/ats-quick-parkour/)
is another custom plugin which is used an extension to the atsParkour plugin. It
adds the inventory-based GUI in which players can teleport to a parkour of their
choice. In the GUI, each parkour is represented as colored block of wool set as
an option which can be seen in the [Basic setting](#basic-settings). If a player
completed a parkour, its wool block glows with an enchanting effect. Hovering a
cursor over this wool block, the popup will contain all the data that can be got
from database and a part of data from parkour's settings.

### Medals

In the config, medals can be configured. Their names don't matter, but
the `importance` does. It shows which medal is more or less important than
others to be able to control their required times and rewards. With importance,
rewards must increase and required times must decrease. Config keys are used in
commands.

### Config

The present `config.yml` file contains variety of elemental configuration
settings such as database credentials, finances settings (will be removed),
medals, and so on. All configs are described in the file with their types and
defaults.

## Downloading

The plugin is not yet available for downloading.

## Contributing

If you wish to contribute it with me, just send the join request :)
