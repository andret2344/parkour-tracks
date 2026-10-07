# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository. Keep it up to
date - when making changes that affect architecture, data flow, build setup, testing conventions, or key constraints,
update the relevant sections here.

## What this is

ParkourTracks (successor of atsParkour and atsQuickParkour) - a Paper plugin for parkour tracks built from WorldEdit
regions. Paper-only on purpose: Spigot is not supported, Paper API is fine to use.

The plugin is being rewritten from scratch in stages. How it has to behave is agreed in `decisions.md` - read it
before changing behavior, and follow it over the old code in the git history. Details it does not settle are chosen
during implementation and recorded in its "Chosen during implementation" section. Deferred features are in
`backlog.md`.

## Commands

```sh
./gradlew build                      # compile, test, coverage check, shadowJar -> build/libs/ParkourTracks-<version>.jar
./gradlew test                       # JUnit tests, then jacoco coverage verification (min 80%)
./gradlew test --tests "eu.andret.parkourtracks.ParkourTracksPluginTest" -x jacocoTestCoverageVerification
```

- `test` is `finalizedBy` `jacocoTestCoverageVerification`, so running a subset of tests fails the 80% coverage rule
  unless that task is excluded with `-x`.
- Java toolchain, Paper API and all dependency versions live in `gradle/libs.versions.toml`. Project `version`,
  `group`, `artifact` and `minecraftVersions` (what releases are marked as supporting) live in `gradle.properties`;
  `${version}` is expanded into `plugin.yml`. MockBukkit (`mockbukkit-v26.2`) must match the Paper API version.
- The `jar` task is disabled on purpose: the shadow jar is the only jar. There is no Maven publishing.

## Architecture

Packages under `eu.andret.parkourtracks`: the root holds the plugin; `config` the content of `config.yml`; `message`
the texts of `messages.yml`; `track` the track model, its rules and its storage; `selection` WorldEdit selections;
`command` the Lamp commands; `game` the games played on tracks; `result` the results database and backups;
`display` the sidebar and the record signs; `economy` money through Vault; `menu` the track selection menu and the
signs that open it or enter a track; `api` the events other plugins listen to; `util` formatting helpers. WorldEdit is a hard dependency (`depend` in `plugin.yml`).

- `ParkourTracksPlugin` - entry point. `onEnable` saves the default `config.yml` and `messages.yml`, reads both by
  hand (Bukkit's `getConfig()` only logs a broken file and goes on empty), loads the tracks and registers the
  commands; an invalid file throws, which stops the plugin. `reload()` reads both files and replaces the settings and
  the messages only when both are valid. Selections come from `WorldEditSelections` unless replaced with
  `setSelections` (tests).
- `message/Messages` - every `Message` enum constant is a key of `messages.yml` (the name in lowercase with dashes);
  a key missing from the admin's file falls back to the shipped one. Values go into the MiniMessage templates as
  placeholders (`Placeholder.unparsed`, never parsed as tags).
- `selection/WorldEditSelections` - the only class touching the WorldEdit API: the player's cuboid selection, or a
  `SelectionException` (incomplete, not a cuboid).
- `command/` - three Lamp command classes on `/parkourtracks` (alias `/ptracks`), registered in code, not in
  `plugin.yml`: `TrackCommand` (help placeholder, list, info, create, region, rename, remove, start, stop, lobbies,
  reload), `TrackPartsCommand` (spawn, finish, checkpoints, walls; positions count from 1) and `TrackSettingsCommand`
  (options, medals, effects, authors). They share `CommandSupport`: messages, the edit lock (`requireStopped`),
  selections and saving after every change. A failure is thrown as `MessageException`, a Lamp `SendableException`
  that replies with a component. `TrackParameterType`, `MedalParameterType` and `OptionParameterType` resolve the
  arguments; `@OptionValue` and `@EffectType` mark arguments completed by providers set up in
  `ParkourTracksPlugin#setUpCommands`. `PlaceholderCondition` stops the help placeholder from swallowing errors of
  subcommands. Lamp needs `-parameters` and is shaded and relocated under `eu.andret.parkourtracks.lamp`.
- `game/GameManager` - the one place deciding who is in which game and what happens to them; `GameListener` only
  reports events to it. A `GameSession` holds the track, the `Phase` (`WAITING` on the spawn, `RUNNING`, `FINISHED`),
  the last checkpoint passed (`SPAWN` = -1), the ticks and the pause flag. `move` handles a move: outside a session a
  running track's region is entered (`Entry.SPAWN` through the spawn, `Entry.SIDE` elsewhere, then sent to the spawn);
  in one, leaving the region ends the game, leaving the spawn starts the run, and the walls, the spawn, the checkpoints
  and the finish the move went through are handled in the order it reached them (`Segments.findEntry`, a ray-box test, so
  a fast move cannot skip a thin region). Skipping follows `SkipMode`; the finish is the checkpoint after the last.
  `goBack` sends to the last checkpoint, or to the spawn (restarting the run) before the first one and on hardcore
  tracks. `handleTeleport` handles foreign teleports (the plugin marks its own with `isTeleporting`): out of the region
  ends the game, inside it goes back instead (`setTo`), an allowed ender pearl is handled a tick later as a jump.
  `join` stores a `Snapshot` (YAML text) and the session marker (track UUID) in the player's PDC; `leave` restores
  the snapshot and clears the marker, except on `DISCONNECT`, which keeps it so `arrive` (server join, or plugin
  start for online players) returns the player to the spawn as `Entry.RETURN`. A snapshot still in the PDC on
  `arrive` is from a crash and is given back first. `tick` runs every tick from the plugin: it counts the ticks of
  running, unpaused, non-training runs and shows them per `timer-display`, and checks `sprintForced` (ticks without
  sprinting outside the spawn, checkpoints and finish, over `sprint-grace-ticks`, send back). `shutdown` (plugin
  disable) ends every game as a disconnect. Hiding is per session and kept consistent on join and leave.
  Every teleport onto the track goes through `sendTo`, which on a boat track removes the old boat and puts the player
  into a new, non-persistent one (`isBoating` lets the vehicle listener allow it; nested calls keep the flag); the
  tick sends a player found without a valid boat back. `GameListener` follows riders through `VehicleMoveEvent`
  (ignoring `PlayerMoveEvent` while riding), cancels getting out of a game boat, anyone else getting in, game
  players getting into any other vehicle, and damage to game boats.
- `game/GameItems` - makes the game items (`GameItem`: back, restart, hide, exit) for the slots in `Settings` and
  recognizes them by the `parkourtracks:game-item` tag in their PDC; `GameItemListener` uses them on a main-hand click
  and stops them from being moved, dropped, swapped or put away, and players in a game from picking anything up.
- `game/TrackGuard` - the edit lock on the world: every block change in the region of a running track is canceled
  (explosions lose the track's blocks from their list; pistons are checked on both sides of the region's edge).
- `result/ResultStore` - the SQLite file `results.db`, one row per completion in `runs`. Every query runs on one
  thread of its own and returns a `CompletableFuture`; callers hand results back to the server thread with
  `ParkourTracksPlugin#runOnMainThread` (skipped once the plugin is disabled). `recordRun` reads the player's and the
  track's best before inserting, so the finish can announce records. The driver comes from `libraries` in
  `plugin.yml` (its coordinates are expanded from the version catalog); tests have it on the runtime classpath.
- `result/Backups` - copies `tracks.json` and the database (`ResultStore#backUp`, `VACUUM INTO` on the database
  thread) to `backups/<timestamp>/` on a schedule restarted by `reload`, pruning to the newest `backup-keep`.
- `display/Sidebar` - the in-game sidebar; `PaperSidebar` builds a scoreboard per player (blank number format,
  custom line names) and gives back the previous one on `hide` if ours is still shown. `GameManager` fills it
  (`refreshSidebar`) on joining and after completions; tests replace it with `helper/FakeSidebar`.
- `display/RecordSigns` - record signs: `[ptracks]`, track, place written on a side (`SignChangeEvent`) store
  `track;place;side` under `parkourtracks:record` in the sign's PDC. Signs of loaded chunks are indexed by track
  (chunk load and unload, `loadAll` on start), so `refresh(track)` touches only that track's signs, asking the
  database for each place and writing the lines on the server thread.
- `economy/Bank` - the economy: `VaultBank` (the only class touching Vault, created only when Vault is installed),
  else `NoBank` (no fees, no rewards); tests use `helper/FakeBank` through `setBank`. `GameManager#join` checks the
  track's permission and takes the fee (not for `Entry.RETURN`) and returns `null` when the player is refused;
  stopping refunds per `refund-on-stop`; the finish pays the reward, and the medal rewards `MedalPayouts.findDue` lists
  for the run, noting each paid one in `medal_payouts` (`ResultStore#recordPayout`).
- `command/ReconcileCommand` - pays owed medal rewards: the preview stores what it showed with a code; the code pays
  only when the recomputed payments equal it. Callbacks after a database query run outside Lamp, so they send their
  failures instead of throwing `MessageException`.
- `menu/Menus` - builds the menu windows: categories, pages of tracks (results fetched first, then the window opens
  on the server thread), the fee confirmation. Each window's holder is a `Menu` mapping slots to actions;
  `MenuListener` cancels every click and drag in one and runs the action of the clicked top slot. `MenuSigns` makes
  and handles `[ptmenu]` and `[ptjoin]` signs. Multi-line messages (`menu-track-lore`) are split on `<br>` via
  `Messages#getTemplate`.
- `api/` - the public events, a promise not to break other plugins: change them only by adding. They carry the
  track's id and name, never the mutable `Track`. `GameManager` fires `TrackJoinEvent` (cancelable) in `join`,
  `TrackLeaveEvent` at the end of `leave`, `TrackCompleteEvent` in `announce` and `TrackPaymentEvent` (`reportPayment`) after
  every successful money movement; `ReconcileCommand` fires the reconciliation payments. `game.Entry` and
  `game.LeaveReason` are part of the API.
- `display/Markers` - labels over the spawn, checkpoints and finish of stopped tracks: marker armor stands,
  non-persistent, hidden from players without `parkourtracks.edit`; `CommandSupport#save` refreshes them after every
  change, a chunk load of a track makes its ones.
- `track/TrackOption` - every option `/ptracks set` changes: how its value is parsed, checked against the other
  options (`boat` excludes `sprintForced` and `enderPearls`), stored and shown. Problems are `OptionException`s the
  command turns into messages.
- `track/TrackRules` - where parts of a track may lie (in its world, inside its region, the spot inside the area),
  whether a new region overlaps another track or leaves parts out, the order of medal times and what a track misses
  before it can start.
- `config/SettingsLoader` - parses `config.yml` into the `Settings` record; errors are `IllegalArgumentException`s
  naming the config path. `medals` is a map from key to MiniMessage display name; its order (best first) is the order
  of the medals.
- `track/Track` - a mutable track: id (UUID, the key of everything stored about it), name, display name, type, world
  name, main region, spawn, checkpoints in between, finish, walls, authors, medal thresholds by medal key, effects,
  own lobby, running flag and `TrackOptions`. `Cuboid` (block box, both corners included, no world - a track's regions
  all lie in its world), `Spot` (position and direction, no world), `Checkpoint` (area plus spot) and `WorldSpot` are
  records. Equality is by id only.
- `track/TrackStore` - reads and writes `tracks.json` with Gson. A write goes to `tracks.json.tmp` and is moved over
  the file atomically. Gson runs the field initializers through the private no-arg constructor of `Track`, so fields
  missing from the file get their defaults; `Track#validateLoaded` checks what Gson cannot. Gson wraps exceptions of
  record constructors, so the error message is taken from the root cause.
- `track/TrackRegistry` - all tracks and the global lobby; names are unique ignoring case, regions of tracks in one
  world never overlap, a running track cannot be removed. Callers save after each change with `save()`.

## Tests

- JUnit 6 + AssertJ + MockBukkit, `// given` / `// when` / `// then` structure. No Mockito.
- Test classes, their methods and test-only helpers are package-private. `public` stays only where Java needs it: the
  `helper/PluginTest` base class (extended from other packages) and overridden API methods.
- Tests extend `helper/PluginTest`, which starts `MockBukkit.mock()`, loads the real plugin with the shipped
  `config.yml` and adds the world `world` before every test; `writeConfig`/`writeMessages` replace the files on
  disk. WorldEdit is replaced by selections set with `select`, the sidebar by `FakeSidebar` (`sidebar.of(player)`),
  and the world is a `TileEntityWorld`, whose chunks implement `getTileEntities` (MockBukkit leaves it
  unimplemented); MockBukkit counts a chunk as loaded only after `Chunk#load`. `admin` adds an operator standing
  somewhere, `tower` a stopped track, `messages` takes the plain text of the messages a player got, `move` moves a
  player through MockBukkit's `PlayerSimulation` (never the deprecated `PlayerMock#simulatePlayerMove`).
  `MockBukkit.load` ignores `depend`, so the plugin loads without WorldEdit. Tests of plain logic (`track`, `result`)
  are plain JUnit tests without a server; Bukkit enum constants such as `Material.LADDER` work without one, but
  anything reaching Paper's registries (`Material#isItem`, potion effect types) needs `MockBukkit.mock()`, and a
  failed registry load breaks every later test in the same JVM.
- `helper/TestTracks` builds playable tracks: `tower` (the one `GameTest` uses) and `small`.
- Game tests extend `game/GameTest`: a running track along x (spawn, two one-block checkpoints, finish, a wall at
  y 0), the lobby set and the world spawn moved off the track (new players appear there). `onSpawn`/`running` give
  players at those stages; `walkTo` moves one block per `move`. MockBukkit's `simulatePlayerMove`
  sets the location before calling the event, so `PlayerMoveEvent#setTo` would not show in tests: going back from a
  move teleports. MockBukkit ignores `PlayerDeathEvent#setKeepInventory` (it follows only the game rule), so death
  tests check the event. MockBukkit cannot teleport a vehicle with a passenger, so boat tests (`BoatTest#driveTo`)
  fire `VehicleMoveEvent`s with the positions instead.
- Results come back asynchronously: tests call `plugin.getResults().flush()` (waits for the queued queries) and then
  `server.getScheduler().performTicks(1)` (runs the callbacks on the server thread).
- MockBukkit's `enablePlugin` lets an exception from `onEnable` through (a real server catches it and disables the
  plugin), so "does not start" tests assert that enabling throws.
- Every rule in `decisions.md` has its own test.
- MockBukkit throws `UnimplementedOperationException`, a `TestAbortedException`, from what it does not implement, and
  JUnit reports that as **skipped**. The `test` task fails the build when any test is skipped, so a test never passes
  without running.

## Conventions

- Tabs for indentation, LF line endings (`.gitattributes`), always braces, no wildcard imports, no `var`, `final` on
  parameters and locals, `@NotNull`/`@Nullable` from `org.jetbrains.annotations` on everything. No copyright headers
  in files. No Lombok.
- A method name starts with a verb in the imperative: `get`, `set`, `find`, `create`, `check`, `format`, `fetch`...
  A method returning a `boolean` starts with `is`, `has` or `can` instead. Exceptions: record accessors, overridden
  API methods, static factories named `of`, event handlers (named after the event) and Lamp command methods (named
  after the subcommand).
- Messages are Adventure `Component`s from MiniMessage; never `ChatColor` or `&` codes.
- One word for one thing: a parkour is a *track* everywhere - code, commands, messages.
- User-facing changes go under `## Unreleased` in `CHANGELOG.md` (`org.jetbrains.changelog` format); release notes
  are extracted from it. Never bump the version or add version sections by hand.
- Apache 2.0: the jar's `META-INF` carries `LICENSE`/`NOTICE` renamed with the `-parkour-tracks` suffix so shaded
  libraries' files do not overwrite them.
- Build script reads project properties through `project.group`/`project.version` and
  `providers.gradleProperty(...)`, never `project.properties[...]`.
- CI is GitHub Actions: `build.yml` builds every push and PR and uploads the JaCoCo XML report to Codecov with the
  `CODECOV_TOKEN` secret. `release.yml` is run by hand from the Actions tab on `main` with a `version` input: it sets
  the version in `gradle.properties`, builds and tests, runs `patchChangelog` (fails when "Unreleased" is empty),
  commits both files as `Released <version>.` by github-actions and creates a **draft** GitHub release with the jar.
  Releases are never made by pushing tags. Uploading to Modrinth and Hangar (`publish.yml`) is not set up yet.
