<div align="center">

# ParkourTracks

Parkour tracks for Paper servers, built from WorldEdit regions: checkpoints, walls, a timer counted in ticks, medals,
records, a track selection menu, and optional fees and rewards through Vault.

[![Build](https://img.shields.io/github/actions/workflow/status/andret2344/parkour-tracks/build.yml?branch=main&logo=githubactions&logoColor=white)](https://github.com/andret2344/parkour-tracks/actions/workflows/build.yml)
[![Coverage](https://img.shields.io/codecov/c/github/andret2344/parkour-tracks?logo=codecov&logoColor=white)](https://codecov.io/gh/andret2344/parkour-tracks)
[![Paper](https://img.shields.io/badge/Paper-26.2%2B-blue)](https://papermc.io/software/paper)
[![Java](https://img.shields.io/badge/Java-25-orange?logo=openjdk&logoColor=white)](https://adoptium.net/)
[![License](https://img.shields.io/github/license/andret2344/parkour-tracks)](LICENSE)

[Report a bug](https://github.com/andret2344/parkour-tracks/issues)

</div>

Successor of atsParkour and atsQuickParkour.

## Features

- **Tracks from regions** - a track is a WorldEdit cuboid with a spawn, checkpoints in between, a finish and walls
  that send players back. Players join by walking into the spawn, through the menu, a sign or a command; whoever comes
  in from anywhere else is sent to the spawn.
- **Fair timing** - the time is counted in game ticks from leaving the spawn to reaching the finish; a fast move
  cannot skip a thin checkpoint or wall. Checkpoints have to be passed in order (or not, per track).
- **Medals and records** - the server's medals (platinum, gold, silver, bronze by default) get a time per track. Every
  completion is saved; players see their best, the record, their medal and a sidebar, and record signs show the
  podium.
- **Track options** - hardcore (every failure restarts the run), forced sprinting, pausing the timer on checkpoints,
  boats on ice or water, potion effects, damage, ender pearls, a permission to join, difficulty and icon.
- **A menu** - categories for server, player-built and training tracks, with each player's results, a lock on
  tracks they may not join and a confirmation before paying a fee.
- **Money with Vault** - entry fees, a reward for every completion, medal rewards paid once, refunds when an admin
  stops a track, and a safe way to pay rewards owed from before.
- **Safe for players' things** - entering takes a snapshot of the inventory, experience, effects, health and food,
  kept with the player's own data; leaving, disconnecting, a restart and even a crash give it back.
- **Locked while running** - a running track cannot be edited, and nothing changes its blocks: players, explosions,
  pistons, fire, liquids or mobs.
- **For other plugins** - events for joining (cancellable), leaving, completing and every payment.

## Installation

1. Put the jar into the `plugins` folder of a Paper 26.2+ server running Java 25.
2. Install [WorldEdit](https://enginehub.org/worldedit) (or FAWE), which ParkourTracks needs for selecting regions.
3. Optionally install [Vault](https://www.spigotmc.org/resources/vault.34315/) and an economy plugin for fees and
   rewards; without them every track is free and pays nothing.
4. Start the server. ParkourTracks downloads its SQLite driver from Maven Central on the first start.

## Building a track

1. Stand where players should go after a game and run `/ptracks setlobby`.
2. Select the whole track with WorldEdit and run `/ptracks create <name>`.
3. Select the spawn area, stand inside it facing the track, and run `/ptracks spawn <name>`. Do the same for the
   finish with `/ptracks finish <name>` and for each checkpoint with `/ptracks checkpoint add <name>`.
4. Select the places that send players back (lava, the ground under jumps) and run `/ptracks wall add <name>`.
5. Optionally give medals a time: `/ptracks medal time <name> gold 30.5`.
6. Check `/ptracks info <name>`, which lists everything that can be set and what is still missing, then
   `/ptracks start <name>`.

While a track is stopped, editors see labels over its spawn, checkpoints and finish.

**Draw the region with care.** Anyone who walks into a running track's region joins it and is sent to its spawn, and
pays its fee when it has one. Keep the region off paths players use to walk past.

## Commands and permissions

`/ptracks` is an alias of `/parkourtracks`.

| Command                                                         | Permission                   | Description                                      |
|-----------------------------------------------------------------|------------------------------|--------------------------------------------------|
| `menu`                                                          | `parkourtracks.play`         | Opens the track selection menu                   |
| `leave`, `lobby`                                                | `parkourtracks.play`         | Leaves the track, goes to the lobby              |
| `stats [track] [player]`                                        | `parkourtracks.play`         | Shows results (another player's needs the next)  |
|                                                                 | `parkourtracks.stats.others` | Seeing other players' results                    |
| `ignore`                                                        | `parkourtracks.ignore`       | Walks through tracks without joining them        |
| `list`, `info <track>`                                          | `parkourtracks.edit`         | Lists the tracks, shows one                      |
| `create <name>`, `rename <track> <name>`, `region set <track>`  | `parkourtracks.edit`         | Creates a track, renames it, moves its region    |
| `spawn`, `finish`, `checkpoint add/set/remove`, `wall add/set/remove` | `parkourtracks.edit`   | The parts of a track                             |
| `set <track> <option> <value>`                                  | `parkourtracks.edit`         | Sets an option (`info` lists them)               |
| `medal time/reward/remove`, `effect set/remove`, `author add/remove` | `parkourtracks.edit`    | Medals, effects, authors                         |
| `track lobby set/clear <track>`                                 | `parkourtracks.edit`         | A lobby of the track's own                       |
| `start`, `stop`, `remove <track>`                               | `parkourtracks.manage`       | Runs, stops, removes a track                     |
| `setlobby`, `reload`                                            | `parkourtracks.manage`       | Sets the lobby, loads the config again           |
| `reconcile <track> [code]`                                      | `parkourtracks.reconcile`    | Pays medal rewards owed from before              |

Players have `parkourtracks.play` and `parkourtracks.stats.others` by default; everything else is for operators.

## Signs

Write these on a sign with `parkourtracks.edit`:

| Lines                                 | What the sign does                                         |
|---------------------------------------|------------------------------------------------------------|
| `[ptmenu]`                            | Opens the menu                                             |
| `[ptjoin]`, track                     | Enters the track (after confirming its fee)                |
| `[ptracks]`, track, place (empty = 1) | Shows that place of the track's ranking, kept up to date   |

## Configuration

- `config.yml` - the medals, the wait before the lobby after a finish, where the timer shows, the game items, the
  sidebar, backups and refunds. Every setting is explained in the file. `/ptracks reload` applies changes; a broken
  file is reported and changes nothing.
- `messages.yml` - every text, in [MiniMessage](https://docs.papermc.io/adventure/minimessage/format/). A message
  missing from the file is taken from the plugin's own copy.

The plugin keeps its tracks in `tracks.json`, which is not meant to be edited by hand, and every completion in
`results.db`. Both are copied to `backups/` once a day by default.

## For developers

Listen to `TrackJoinEvent` (cancellable), `TrackLeaveEvent`, `TrackCompleteEvent` and `TrackPaymentEvent` from
`eu.andret.parkourtracks.api`.

Building needs Java 25: `./gradlew build` makes `build/libs/ParkourTracks-<version>.jar`.
