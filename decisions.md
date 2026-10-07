# Decisions

Design decisions for the migration, agreed on before coding. Deferred features are GitHub issues.

## Product

- The name is ParkourTracks: package `eu.andret.parkourtracks`, PDC namespace and permission root `parkourtracks`,
  command `/parkourtracks` with the alias `/ptracks`, repository `git@github.com:andret2344/parkour-tracks.git`.
- The name is final before the first release: the PDC namespace holds inventory snapshots, renaming it later loses
  players' items.
- One word for one thing: a parkour is a *track* everywhere, in code (`ParkourGame` becomes `Track`), commands
  (`/ptracks create <track>`) and messages. Below, "parkour" means a track.
- ats-parkour and ats-quick-parkour become one plugin; the parkour selection GUI is part of it.
- The plugin is public (no own server runs it), so it has to work for any admin out of the box.
- It was never public: no import of old MySQL data, no compatibility with the old `setting.json` format.

## Storage

- MySQL is removed. Results live in an SQLite file in the plugin folder.
- Every completion is stored (full history); best times, completion counts and records are queried from it.
- Results are keyed by the parkour's UUID, not its name, so a rename keeps the history.
- Tracks (regions, options, medals, authors) live in a JSON file, saved on every change, atomically (temporary file,
  then rename).
- Removing a track keeps everything it left in SQLite: results, the payout register, all of it.
- Backups stay and cover the SQLite file too, copied safely through the database (the medal payout register is money).

## Events

- Events are the public API for those who want results in their own database, on Discord and so on.
- Notifications after the fact (e.g. a completion with its time, medal and record flag) plus cancelable events before
  an action (e.g. joining a game).
- The game logic itself does not run through Bukkit events, so a foreign listener cannot break it halfway.
- The set of events is kept small; each one is a promise not to break other plugins' code. Four of them:
  - joining a game, before the fact and cancelable (e.g. a combat tag plugin denies the entry),
  - leaving a game, after the fact, with the reason: exit item, leaving the region, teleport, disconnect, parkour
    stopped,
  - a completion, after the fact: player, parkour, time in ticks, medal, whether it is a personal or a parkour record,
  - a payout, after the fact: medal, fee, reward or reconciliation, with the amount.
- Left out until someone asks: checkpoints (split times), the start of a run, going back to a checkpoint, an admin
  starting or stopping a parkour. Adding an event later costs nothing; removing one breaks someone's code.

## Economy

- Economy through Vault as a `softdepend`; without Vault there are no fees and no rewards. `FinancialProvider`,
  `RankProvider` and the `sample` module are removed.
- `vipOnly` is replaced by a per-parkour permission option; who has it is up to the permission plugin.
- The fee is always the admin's decision (0 is fine, `TRAINING` included). It is charged on every way into the game:
  teleport block, GUI, command, walking in from the side.
- The fee is made visible in the message when it is charged, in `/pk info`, in the GUI and in the admin documentation
  (draw the region so it does not overlap paths, entering from the side costs).
- The flat completion `reward` stays.

## Medals

- One set of medals for the whole server, in `config.yml` (key and display name); each track sets its own time
  thresholds and rewards by command.
- The medals are ordered by their position in the config list, best first; `importance` is removed.
- Setting a threshold that breaks the order is rejected: a better medal needs a shorter time. Rewards are free (a
  prestige-only gold next to a paid bronze is fine).
- `/ptracks info` and the GUI list a track's medals with thresholds and rewards; the GUI also shows which one the
  player has.
- A medal is paid once. Every payout goes to a register in SQLite: player, parkour, medal, when, how much.
- A medal is identified by its config key: renaming the key makes a new medal (documented, or warned about on start).
- A raised reward of an already paid medal is not topped up; tightened thresholds take nothing back.
- A medal without a time set is simply not awarded; `require-medals` is removed.
- A run that beats a threshold pays the medal right away. There are no automatic retroactive payouts.
- Retroactive payouts only through an admin command, per parkour:
  - a preview with numbers first: how many players, the total, the split by medal, the biggest recipients,
  - the confirmation is bound to that preview: it expires, only the same sender can give it, and it is void when
    the thresholds or the results change in between,
  - offline players are paid through Vault; only successful payouts are recorded, failures are reported and the same
    command retries them,
  - every reconciliation is logged (who, when, how much, to whom).

## Parkour types

- `type` stays. `SERVER` is the basic one. `TRAINING` has no timer, no results, no medals and no completion reward.
  `PLAYERS` has authors.
- `savingResults` is removed; the type decides.
- `PLAYERS`: an admin creates the parkour and sets its authors (UUIDs, several allowed). Authors get no editing rights.
  Payouts and statistics for authors are issues #1 and #2.

## Rules of play

- Time is counted in ticks.
- Pausing the timer on checkpoints is a per-parkour option.
- Skipping a checkpoint is a per-parkour option: `ALLOW`, `NOTIFY`, `FAIL` (back to the last checkpoint). Default
  `FAIL`.
- Nothing counts until the player has passed the spawn: a run starts only when they leave the spawn region.
- A player in the region who did not come through the spawn is teleported to the spawn and charged the fee. Not
  affected: a stopped parkour, players in `ignore` mode, players in spectator.
- Survival, adventure and creative players are parkour players; spectator is treated like `ignore`. A creative player
  plays in creative: flying is blocked like for everyone (turned off on entering, switching it on is canceled), and
  nothing else is. Accepted on purpose: their runs count in the rankings, give medals and rewards, and they can take
  any item from the creative inventory during a game.
- Switching to creative during a game keeps the game going; switching to spectator ends it.
- Entering a region is checked along the segment from `from` to `to`, so fast movement cannot skip thin regions.
- Ender pearls are a per-parkour option. Chorus fruit, elytra and riptide tridents are blocked. Gliding into the
  region turns gliding off.
- A teleport during a run that does not come from the plugin: a destination outside the region leaves the game, a
  destination inside it sends the player back to the last checkpoint.
- `sprintForced`: stopping sprinting outside a checkpoint sends the player back to the last checkpoint, after a short
  grace period (about 5 ticks).
- Flying stays blocked.
- Leaving the region during a run leaves the game and the run is lost. Walls are the admin's responsibility.
- Death during a run: respawn on the last checkpoint, the run goes on, nothing is dropped.
- Session marker: entering a game writes the parkour's UUID into the player's PDC, leaving clears it. A player who
  rejoins (or is online when the plugin starts) in the region of the parkour of their marker goes to its spawn without
  paying; anyone else follows the "not through the spawn" rule. A marker of a removed parkour is ignored and cleared.

- Lobby: a global lobby is required before any track can start (listed in `/ptracks info`); a track can override it
  with its own.
- During a run only the track's effects apply; the snapshot on entering takes off the player's own.
- `alwaysSpawn` becomes `hardcore`: every way back (wall, death, stopping sprinting, a foreign teleport inside the
  region, a skipped checkpoint under `FAIL`, the back item) sends the player to the spawn and the run starts over.
- `damageAllowed` is about damage from the environment (cactus, falling and so on). Damage from other players (hits,
  arrows, knocking off the track) is always blocked during a game.
- Hunger is always off during a game, whatever `damageAllowed` says.

## Track geometry and lifecycle

- Tracks never overlap: creating or changing a region that overlaps another track's region is rejected.
- The spawn, the finish, checkpoints and walls lie wholly inside the track's region and in its world; a command that
  breaks it is rejected, a change of the main region that would leave something outside included.
- Regions are cuboids only.
- After the finish: a per-track option, either the lobby after X seconds (default) or straight back to the spawn for
  another try.
- `stop` sends the players to the lobby with a message, their runs are lost. Whether they get their fee back is a
  global config setting (no per-track override), on by default.
- `remove` works only on a stopped track.

## Edit lock

- Always on, no option. While a parkour runs, nobody changes its route or its configuration, `ignore` players and
  admins included (`ignore` only means not being a parkour player); to edit, stop the parkour.
- While a parkour runs, every block change in its region is blocked: players, explosions, pistons, fire, mobs,
  liquids, leaf decay.

## Boat mode

- Stays and works on ice and on water.
- The run always starts in a boat, on every way into the game.
- Getting out is blocked during the game. Leaving through the exit item, leaving the region or a teleport out removes
  the boat.
- Back to a checkpoint: the old boat is removed, the player gets a new one with zero velocity.
- No passengers.
- The boat type is a per-parkour option.
- Destroying the boat is blocked; if it happens anyway, back to the last checkpoint.
- `boat` together with `sprintForced` or with allowed ender pearls is rejected when the option is set, with a message
  saying why.

## Inventory

- The inventory is always modified; `modifyInventory` is removed.
- On entering, a snapshot is stored in the player's PDC: inventory, armor, off hand, XP, potion effects, health, food.
  It is restored on leaving.
- Five game items: exit, hide players, menu, back to the checkpoint, restart. Each has a configurable slot and can be
  turned off.
- Game items are recognized by a PDC tag and cannot be moved, dropped, swapped to the other hand or put in a container.
- Hiding players hides only the players of the same parkour, those joining later included, and ends when the player
  leaves the game.

## GUI

- Opened with `/pk menu` and with a block or a sign that opens it. No item outside the game: servers that want one bind
  it to the command with a plugin like ItemJoin.
- A main menu with a category per `type`; empty categories are hidden.
- Stopped parkours are hidden; parkours the player has no permission for are shown with a lock.
- `icon` replaces `color`: any material per parkour, by default wool colored by difficulty (1 lime, 2 yellow,
  3 orange, 4 red, 5 black), the mapping fixed in code.
- Difficulty is 1-5; anything else is rejected.
- The lore is a MiniMessage template with placeholders in the config.
- Clicking a parkour with a fee opens a second window to confirm the payment.
- Pages when a category has more parkours than fit.

## What the player sees

- Record signs are made by writing on a sign (with `parkourtracks.edit`): `[ptracks]`, the track, the ranking place.
  Signs for places 1, 2, 3 side by side make a podium. The data lives in the sign's PDC, not in the tracks file; a track
  can have any number of signs.
- The sidebar scoreboard during a game can be turned off in the config, on by default. Leaving the game restores the
  scoreboard the player had before, not the server's main one.
- Sounds come back as the old plugin had them: `config.yml` sets a sound for joining, starting the run, reaching a
  checkpoint, completing the track and leaving, each a sound or none. By default only starting
  (`block.lever.click`) and completing (`entity.player.levelup`) make a sound. Only the player hears it, at volume
  and pitch 0.5.
- `[ptjoin]` signs can show every option of their track: each option is a placeholder named after it (`<hardcore>`,
  `<skipMode>`, ...) in the sign's lines in `messages.yml`. Values that are words (true and false, the choices of
  `type`, `skipMode`, `afterFinish`) are shown through texts in `messages.yml`, so the admin words them; numbers stay
  numbers. They can also show the track's record: its time and the player who holds it, or a text from
  `messages.yml` while there is none (always on `training` tracks, which keep no results).
- `[ptjoin]` signs refresh like record signs: after any change of their track, after a completion on it and when
  their chunk loads. `/ptracks sign refresh [track]` redraws the signs of one track, or of all, by hand.
- The running time is shown in the action bar, the XP bar or both, set in the config; the action bar by default.
- Checkpoint markers of a stopped track are visible only to players with `parkourtracks.edit`.

## Commands

- Lamp, registered in code.
- Every command has the form `<noun> <action> <track> [arguments]`, the noun in the singular: `checkpoint add`,
  `wall remove`, `medal time`, `region set`, `lobby set`. A noun with a single action leaves it out (`spawn <track>`,
  `finish <track>`, `option <track> ...`). An optional argument always comes last. This form is binding: new
  commands follow it, and no command gets a form of its own (no `setlobby`, no `track lobby set`).
- Track options: one `/ptracks option <track> [option] [value]`, with value completion by the option's type. Without
  an option it lists all options of the track with their values; without a value it shows that option's value; with
  a value it sets it.
- The spawn and the finish are separate from the checkpoint list: `/ptracks spawn <track>`, `/ptracks finish <track>`,
  and `/ptracks checkpoint add <track> [position]` for checkpoints in between, insertable in the middle. Adding a
  checkpoint never turns the finish into a checkpoint.
- Grouped by noun: `checkpoint add|set|remove`, `wall add|set|remove`, `region set`, `lobby set|clear`.
- Lobbies: `/ptracks lobby set [track]` sets the lobby where the admin stands, the global one without a track
  (`parkourtracks.manage`), the track's own with one (`parkourtracks.edit`). `/ptracks lobby clear <track>` makes the
  track use the global lobby again; `lobby clear` without a track is refused, since every track needs the global
  lobby: it can only be moved with `lobby set`.
- Player commands: `menu`, `enter [track]`, `leave` (the same as the exit item) and `stats [track]`. `enter` with a
  track enters it like a `[ptjoin]` sign (the track's permission, the fee confirmed first, a game the player is in
  ended); without a track it ends the player's game, if they are in one, and sends them to the lobby (the track's
  own, else the global one). There is no `/ptracks lobby` for players. `stats` without a track gives a short summary
  of all tracks the player completed, with one the details (best time, completions, last completion, medal, and the
  track record to compare with). `stats [track] [player]` shows another player's results, under its own permission
  `parkourtracks.stats.others`, granted to everyone by default.
- Permissions in a few groups: `parkourtracks.play` (player commands, everyone by default), `parkourtracks.edit`
  (creating and editing tracks), `parkourtracks.manage` (start, stop, remove, the global lobby),
  `parkourtracks.ignore`, and `parkourtracks.reconcile` on its own, since it pays out money.

## Messages and config

- Every text is MiniMessage, with MiniMessage placeholders (`<time>`, `<track>`); no `&` codes, no `ChatColor`.
- English only; the admin translates `messages.yml` if they want another language.
- Two files: `config.yml` (settings, game item slots included) and `messages.yml` (every text: messages, scoreboard,
  record sign, GUI lore).
- `/ptracks reload` reloads both; an invalid file gives a message and changes nothing.
- The tracks file is storage, not for editing by hand; tracks change only through commands.
- An invalid config on start stops the plugin with a readable message (no silent start on defaults, even though a
  stopped plugin means no edit lock).

## Project

- The git history of ats-parkour moves to the new repository; the history of ats-quick-parkour does not.
- The same stack as TeleSign, BlockGens and BlastPotion: Java 25, Paper only (26.2), Lamp, JUnit 6 with AssertJ and
  MockBukkit, JaCoCo, shadow, `org.jetbrains.changelog`, GitHub Actions (`build`, `release`, `publish` to Modrinth and
  Hangar).
- bStats is left out until everything else is done and the plugin is about to be published; it then gets a new ID (the
  old `10700` was shared by both old plugins).
- No Lombok: records for data, plain classes for state.
- Test coverage: 80% as a hard threshold, and every rule in this file has its own test.
- WorldEdit stays a hard dependency for selecting regions (FAWE works too).

## Features

- Removed: the tutorial, `require-medals`, `fix`, the MySQL keep-alive.
- `/pk info` grows instead of the tutorial: what a parkour still needs before it can start, and what can be set.
- Stay: medals, the record sign, the scoreboard, hiding players, effects, `damageAllowed`, `hardcore` (was `alwaysSpawn`), the lobby, the
  flight block, the teleport block (as one of the ways into the game).

## To verify before coding

- Boat mode follows a player's moves through `VehicleMoveEvent` and ignores `PlayerMoveEvent` while riding; on a
  real server, check that `VehicleMoveEvent` fires for a boat the player steers, on ice and on water.
- The sidebar (`PaperSidebar`): MockBukkit implements neither hiding the score numbers nor custom line names, so it
  is only tested through a fake; check on a real server that it shows without red numbers and that leaving gives
  back the scoreboard of another plugin.
- Vault (`VaultBank`) is only tested through a fake economy; check fees, rewards and payouts with a real economy
  plugin, including paying an offline player.

## Chosen during implementation

Details `decisions.md` did not settle, chosen while coding. Review them; anything here can still change.

- The first version is `1.0.0`; `minecraftVersions` is `26.2,26.3`, like the other plugins.
- A track name is 1-32 letters, digits, `_` or `-`; names are unique and looked up ignoring case.
- Medal thresholds are stored in ticks; a threshold of 0 ticks means "no time set yet" and is never awarded.
- Medals in `config.yml` are a map from the key (lowercase letters, digits, `_`, `-`) to the MiniMessage display
  name, in order, best first.
- `tracks.json` holds the tracks and the global lobby. Worlds are stored by name, effects by the key of their type
  (e.g. `minecraft:jump_boost`).
- A broken `tracks.json` stops the plugin and is never overwritten, like a broken `config.yml`.
- The tracks file is written on the main thread right after each change: it is small, and writing it in order is
  simpler than ordering asynchronous writes.
- The old `enabled` option is gone: starting and stopping a track covers it.
- Commands: `list`, `info`, `create`, `region set`, `rename`, `spawn`, `finish`, `checkpoint add|set|remove`,
  `wall add|set|remove`, `option`, `medal time|reward|remove`, `effect set|remove`, `author add|remove`,
  `lobby set <track>` and `lobby clear <track>` need `parkourtracks.edit`; `remove`, `start`, `stop`, `lobby set`
  without a track and `reload` need `parkourtracks.manage`.
- Positions of checkpoints and walls in commands count from 1; `checkpoint add` without a position appends.
- The spot of the spawn, the finish or a checkpoint is where the admin stands; they have to stand inside the selected
  area, in the track's world.
- Neither the global lobby nor a track's own lobby can lie inside any track's region: players sent there would walk
  straight into it.
- Effects are set by level as the game shows it (1 is the weakest, up to 256); the stored amplifier is one less.
- Medal times are typed in seconds (`30.5`) and rounded to the nearest tick; a better medal needs a strictly shorter
  time than every worse one that has a time.
- Authors are added by the name of a player who has played on the server, and only on tracks of type `players`.
- Track options in `option`: `displayName` (MiniMessage), `type`, `difficulty`, `icon` (an item or `none`),
  `permission` (lowercase node of letters, digits, `_`, `.`, `-`, or `none`), `fee`, `reward`, `hardcore`,
  `skipMode`, `pauseOnCheckpoints`, `sprintForced`, `damageAllowed`, `enderPearls`, `boat`, `boatType` (a boat or
  raft without a chest) and `afterFinish`.
- Lamp's own errors (a wrong argument type, a missing argument, no permission) keep Lamp's English texts; everything
  the plugin says itself is in `messages.yml`.
- Walking back into the spawn during a run starts the run over.
- A teleport is the plugin's own only while the plugin makes it; any other one (commands, other plugins, chorus
  fruit, an ender pearl the track does not allow) is foreign. An allowed ender pearl counts where it lands, as if the
  player had walked there in one step, without checking what it flew over.
- Leaving the region by walking out or by a foreign teleport leaves the player where they are; the exit item,
  `/ptracks leave`, `/ptracks enter` without a track, stopping the track and the finish send them to the lobby (the track's own,
  else the global one).
- `/ptracks ignore` (permission `parkourtracks.ignore`) switches ignoring tracks; it is kept in the player's PDC,
  so it lasts over a restart and a reconnect: creative no longer keeps builders out of a running track, `ignore` does.
- `/ptracks option <track> <option>` shows the value even while the track runs; only setting it needs the track
  stopped.
- A player who switched to creative during a game keeps creative flight after leaving, although the snapshot was
  taken without it.
- `/ptracks enter <track>` of a stopped track says the track is not running.
- Entering a game takes the snapshot, then clears the inventory and the effects, fills health and food, turns flight
  off (toggling flight is canceled during the game) and gives the track's effects with an infinite duration.
- After a death the player comes back to life where going back would take them (the last checkpoint, or the spawn
  before the first one and on hardcore tracks).
- A player who finished and waits for the lobby is no longer checked against walls, checkpoints or teleports inside
  the region.
- `finish-delay` in `config.yml` (default 5 seconds) is the wait before the lobby; `timer-display` (`action-bar` by
  default, `xp-bar` or `both`) is where the running time shows. On the XP bar the level is the seconds, the bar their
  fraction.
- The session marker is `parkourtracks:session` (the track's UUID) and the snapshot `parkourtracks:snapshot` (YAML
  text) in the player's PDC. Disabling the plugin (a restart) ends every game like a disconnect: everyone gets their
  state back right away and returns to the spawn without paying.
- The game items so far: back (slot 0, slime ball), restart (slot 1, clock), hide (slot 7, ender eye), exit (slot 8,
  red bed); the menu item comes with the menu. Slots and materials are in `config.yml` under `game-items`, each can be
  turned off; the names are in `messages.yml`. An item is used by clicking with it in the main hand.
- A game item found outside a game (a leftover) is removed when used.
- Players in a game pick up no items: their inventory is given back as it was when they leave.
- Hiding hides only the other players of the same track, players joining later included, and ends when the hider
  leaves; a player leaving the game becomes visible again to those hiding them.
- The edit lock on the world covers breaking and placing blocks, buckets, fire (burning, igniting, spreading),
  melting and forming (ice, snow), liquids flowing in, leaf decay, mobs and falling blocks changing blocks,
  explosions (the track's blocks are taken out of the explosion), pistons pushing or pulling across the region's edge
  and item frames or paintings breaking. Using doors, trapdoors, buttons and levers is not blocked.
- `sprintForced` is checked every tick of a running run: outside the spawn, the checkpoints and the finish, a player
  not sprinting for more than `sprint-grace-ticks` (default 5) is told so and sent back.
- Chorus fruit teleports, starting to glide and riptide are canceled during a game.
- On a boat track, every way onto the spawn or a checkpoint (entering, going back, restarting, the finish's spawn
  option, coming back after a disconnect) removes the old boat and puts the player into a new one of the track's
  type, standing still, never saved with the world. A player found without their boat (it was removed by something
  else, or they left it somehow) goes back, checked every tick.
- Players in a game get into no other vehicle, on any track: their moves would not be checked while riding.
- The SQLite driver (`org.xerial:sqlite-jdbc`) is declared under `libraries` in `plugin.yml`: Paper downloads it
  from Maven Central on the first start, whether or not the server ships one.
- Results are in `results.db` in the plugin folder: one row per completion (track UUID, player UUID, ticks, when).
  All database work runs on one thread of its own, in order; results come back to the server thread. A broken
  `results.db` stops the plugin, like a broken config.
- A track's ranking has every player once, with their best time; at equal times whoever got there first is ahead.
- After a completion the player is told about a new track record, or else a new personal best (the first completion
  is one), and about a medal better than the one their previous best earned.
- The medal a player has on a track is the best one their best time earns under the current thresholds.
- `/ptracks stats` lists the tracks by name with the best time and completions; with a track it shows the best time,
  completions, the last completion (`yyyy-MM-dd HH:mm`, the server's time zone), the medal and the track record.
  Results of removed tracks are kept but not shown. Training runs are not saved.
- The sidebar shows the track's display name, the player's best, the record and its holder, the medal and the
  completions; an empty text in `messages.yml` leaves its line out. It refreshes on joining and after any completion
  on the track, for everyone on it. No sidebar on training tracks. `scoreboard: false` in `config.yml` turns it off.
- A record sign is a side whose lines are `[ptracks]` (any case), the track name and the place (1-1000, empty for
  1), written by a player with `parkourtracks.edit`; others get a message and the sign stays as written. Its lines
  come from `sign-line-1` to `sign-line-4` in `messages.yml`. Signs refresh when made, after a completion on the
  track, when the track is renamed or its display name changes, and when their chunk loads. A sign of a removed
  track says so.
- Backups go to `backups/<yyyy-MM-dd_HH-mm-ss-SSS>/` with `tracks.json` and `results.db` (copied by SQLite's
  `VACUUM INTO`), every `backup-frequency` minutes (default 1440, 0 for never, counted from the start), keeping the
  newest `backup-keep` (default 10).
- The economy is Vault's, looked up on every use (an economy plugin may register after ParkourTracks starts). The Vault
  API comes from JitPack (`com.github.MilkBowl:VaultAPI`).
- The track's `permission` is checked before the fee; a player refused either way gets a message and is sent to the
  lobby (a teleport into the track goes to the lobby instead). Coming back after a disconnect pays no fee.
- The completion `reward` is paid on every completion (not on training tracks).
- Medal rewards are paid when a run's time earns them, every unpaid one it earns (a gold time also earns silver).
  Only payments that went through are noted; a medal with a reward of 0 is never noted, so raising its reward later
  makes it owed. A failed payment stays owed.
- `/ptracks reconcile <track>` (permission `parkourtracks.reconcile`) shows the owed rewards (players, total, per
  medal, the five biggest recipients) and a four-digit code; `/ptracks reconcile <track> <code>` pays them, within a
  minute (1200 ticks), for the same sender, and only if exactly the same payments are still owed. Every payment and
  every failure is logged.
- The "block or sign" that opens the menu and the teleport block are signs: `[ptmenu]` opens the menu, `[ptjoin]`
  with a track name on the second line enters that track (confirming a fee first, like the menu). Writing them needs
  `parkourtracks.edit`; what a sign does lives in its PDC (`parkourtracks:sign`), and right-clicking it uses it
  instead of opening the sign editor. Their lines are rendered when written and refreshed after every change of the
  track.
- The menu game item (slot 4, compass) is the fifth game item. Choosing another track from a game switches to it.
- With only one category holding running tracks the menu opens it right away. Categories follow the order of the
  types (server, training, players). Tracks are sorted by difficulty, then name; a page holds 45, with previous,
  back and next in the bottom row. Completed tracks glow. The default category icons are a nether star (server), a
  player head (players) and leather boots (training).
- The track lore is one message, `menu-track-lore`, its lines separated by `<br>`; tracks by players get the
  `menu-track-authors` line too.
- The events are `TrackJoinEvent` (cancelable, after the track permission and before the fee; a canceled join
  sends the player to the lobby, the canceling plugin says why), `TrackLeaveEvent` (with the `LeaveReason`),
  `TrackCompleteEvent` (once the run is saved: ticks, best medal key, personal best, track record; none for
  training) and `TrackPaymentEvent` (`FEE`, `REFUND`, `REWARD`, `MEDAL`, `RECONCILIATION`, with the medal key and the
  amount), all in `eu.andret.parkourtracks.api`. They give the track's id and name, not the internal `Track`,
  which other plugins must not change. `game.Entry` and `game.LeaveReason` are part of this API.
- Markers are invisible marker armor stands with their name shown, one block above the spot of the spawn, each
  checkpoint and the finish of stopped tracks, never saved with the world. They are hidden from every player without
  `parkourtracks.edit` (when made and when a player joins), made again after every change saved by a command, and
  made when a chunk of a track loads (none are made in unloaded chunks). A text display would face one way only;
  MockBukkit cannot make one face the viewer.
- Releasing works like the other plugins (`release.yml`, a draft GitHub release). Publishing to Modrinth and Hangar
  (`publish.yml`, `hangarPublish`) comes with bStats, once the projects exist.

## Open questions

- `enderPearls` cannot be used yet: the inventory is cleared on entering and nothing is picked up in a game, so
  players have no pearls. Options: the track gives a number of pearls when a run starts (recommended), pearls alone
  can be picked up in a game, or the option goes.
- Doors, trapdoors, buttons and levers can be used during a game; one player opening a trapdoor changes the track for
  the others. Keep it, block it, or make it a track option?
