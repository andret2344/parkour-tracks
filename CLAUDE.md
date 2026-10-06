# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository. Keep it up to
date - when making changes that affect architecture, data flow, build setup, testing conventions, or key constraints,
update the relevant sections here.

## What this is

ParkourTracks (successor of atsParkour and atsQuickParkour) - a Paper plugin for parkour tracks built from WorldEdit
regions. Paper-only on purpose: Spigot is not supported, Paper API is fine to use.

The plugin is being rewritten from scratch in stages. How it has to behave is agreed in `decisions.md` - read it
before changing behaviour, and follow it over the old code in the git history. Details it does not settle are chosen
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
`command` the Lamp commands; `util` formatting helpers. WorldEdit is a hard dependency (`depend` in `plugin.yml`).

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
  missing from the file get their defaults; `Track#checkLoaded` checks what Gson cannot. Gson wraps exceptions of
  record constructors, so the error message is taken from the root cause.
- `track/TrackRegistry` - all tracks and the global lobby; names are unique ignoring case, regions of tracks in one
  world never overlap, a running track cannot be removed. Callers save after each change with `save()`.

## Tests

- JUnit 6 + AssertJ + MockBukkit, `// given` / `// when` / `// then` structure. No Mockito.
- Test classes, their methods and test-only helpers are package-private. `public` stays only where Java needs it: the
  `helper/PluginTest` base class (extended from other packages) and overridden API methods.
- Tests extend `helper/PluginTest`, which starts `MockBukkit.mock()`, loads the real plugin with the shipped
  `config.yml` and adds the world `world` before every test; `writeConfig`/`writeMessages` replace the files on
  disk. WorldEdit is replaced by selections set with `select`; `admin` adds an operator standing somewhere, `tower`
  a stopped track, `messages` takes the plain text of the messages a player got.
  `MockBukkit.load` ignores `depend`, so the plugin loads without WorldEdit. Tests of plain logic (`track`, `config`)
  are plain JUnit tests without a server; Bukkit enums such as `Material` work without one.
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
- Messages are Adventure `Component`s from MiniMessage; never `ChatColor` or `&` codes.
- One word for one thing: a parkour is a *track* everywhere - code, commands, messages.
- User-facing changes go under `## Unreleased` in `CHANGELOG.md` (`org.jetbrains.changelog` format); release notes
  are extracted from it. Never bump the version or add version sections by hand.
- Apache 2.0: the jar's `META-INF` carries `LICENSE`/`NOTICE` renamed with the `-parkour-tracks` suffix so shaded
  libraries' files do not overwrite them.
- Build script reads project properties through `project.group`/`project.version` and
  `providers.gradleProperty(...)`, never `project.properties[...]`.
- CI is GitHub Actions: `build.yml` builds every push and PR and uploads the JaCoCo XML report to Codecov with the
  `CODECOV_TOKEN` secret. The release and publish workflows come before the first release.
