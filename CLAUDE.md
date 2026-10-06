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

Package `eu.andret.parkourtracks`. `ParkourTracksPlugin` is the entry point. WorldEdit is a hard dependency
(`depend` in `plugin.yml`).

## Tests

- JUnit 6 + AssertJ + MockBukkit, `// given` / `// when` / `// then` structure. No Mockito.
- Test classes, their methods and test-only helpers are package-private. `public` stays only where Java needs it: the
  `helper/PluginTest` base class (extended from other packages) and overridden API methods.
- Tests extend `helper/PluginTest`, which starts `MockBukkit.mock()`, loads the real plugin and adds the world `world`
  before every test. `MockBukkit.load` ignores `depend`, so the plugin loads without WorldEdit.
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
- Build script reads project properties through `project.group`/`project.version` and
  `providers.gradleProperty(...)`, never `project.properties[...]`.
- CI is GitHub Actions: `build.yml` builds every push and PR and uploads the JaCoCo XML report to Codecov with the
  `CODECOV_TOKEN` secret. The release and publish workflows come before the first release.
