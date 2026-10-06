# Backlog

Features agreed on but deliberately left out of the first release after the migration.

## Author payouts for `PLAYERS` parkours

A `PLAYERS` parkour is built by players, but created and configured only by an admin, who also sets its authors.
Authors get no editing rights.

- The authors get a share of the entry fee. No money is created: a parkour with `fee: 0` pays the authors nothing.
- The share is a percentage set globally in `config.yml`, which a parkour can override.
- With several authors, the share is split equally between them.
- The fee is charged on every way into the game (teleport block, GUI, command, walking in from the side), so the
  authors get their share on each of them.
- An author does not pay the fee to enter their own parkour.
- Each author's part of the share is rounded down; what the rounding leaves goes to nobody.

- A payout can fail (no economy account yet, an economy without offline support, its database down, a balance
  limit). A failed payout is kept as pending in SQLite and retried when the author joins the server; every failure
  and retry is logged.

## Author statistics

- Authors ranked by the number of parkours they built.
- For each author: which players completed their parkours the most times.

Open question: where the statistics are shown (command, GUI, both).

## PlaceholderAPI

Placeholders such as `%parkourtracks_best_<track>%` (a track's record) or `%parkourtracks_player_best_<track>%` (the
viewing player's best time), so admins can show results in holograms, TAB, other scoreboards, menus and messages.

- PlaceholderAPI as a `softdepend`.
- The list of placeholders has to be designed and documented.
- Placeholders can be requested many times per second (e.g. by TAB), so they read from an in-memory cache, never
  straight from SQLite.

## Polygon regions

The first release supports cuboid regions only. A WorldEdit polygon selection for the main region would fit tracks
shaped like an L without taking in the empty space around them.

## Already in the first release

So that these features only add code, not a storage migration:

- the `PLAYERS` type and the list of authors (UUIDs) in the parkour model, set by an admin command and shown in
  `/pk info` and the GUI,
- every completion, stored in SQLite (full history).
