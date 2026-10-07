package eu.andret.parkourtracks.result;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * The results of every run, in an SQLite file. Each completion is a row; best times, counts and rankings are queried
 * from them. All work runs on one thread of its own, in the order it was asked for, never on the server thread.
 */
public final class ResultStore implements AutoCloseable {
	private static final String SCHEMA = """
			CREATE TABLE IF NOT EXISTS runs (
				id INTEGER PRIMARY KEY AUTOINCREMENT,
				track TEXT NOT NULL,
				player TEXT NOT NULL,
				ticks INTEGER NOT NULL,
				finished_at INTEGER NOT NULL
			)""";
	private static final String TRACK_INDEX = "CREATE INDEX IF NOT EXISTS runs_track ON runs (track, ticks)";
	private static final String PLAYER_INDEX = "CREATE INDEX IF NOT EXISTS runs_player ON runs (player, track)";
	/**
	 * The register of medal rewards paid: a medal is paid once per player and track, whatever changes later.
	 */
	private static final String PAYOUTS = """
			CREATE TABLE IF NOT EXISTS medal_payouts (
				track TEXT NOT NULL,
				player TEXT NOT NULL,
				medal TEXT NOT NULL,
				amount REAL NOT NULL,
				paid_at INTEGER NOT NULL,
				PRIMARY KEY (track, player, medal)
			)""";

	@NotNull
	private final Connection connection;
	@NotNull
	private final ExecutorService executor;

	private ResultStore(@NotNull final Connection connection) {
		this.connection = connection;
		executor = Executors.newSingleThreadExecutor(runnable -> {
			final Thread thread = new Thread(runnable, "ParkourTracks results");
			thread.setDaemon(true);
			return thread;
		});
	}

	/**
	 * Opens the file, creating it and its tables when missing.
	 *
	 * @throws IllegalStateException when the database cannot be opened
	 */
	@NotNull
	public static ResultStore open(@NotNull final Path file) {
		try {
			// The driver is loaded by the plugin's class loader, which DriverManager does not search by itself
			Class.forName("org.sqlite.JDBC");
			final Connection connection = DriverManager.getConnection("jdbc:sqlite:" + file.toAbsolutePath());
			try (final Statement statement = connection.createStatement()) {
				statement.execute(SCHEMA);
				statement.execute(TRACK_INDEX);
				statement.execute(PLAYER_INDEX);
				statement.execute(PAYOUTS);
			} catch (final SQLException ex) {
				// Left open, the connection would keep the file locked
				connection.close();
				throw ex;
			}
			return new ResultStore(connection);
		} catch (final ClassNotFoundException | SQLException ex) {
			throw new IllegalStateException("Could not open the results database: " + ex.getMessage(), ex);
		}
	}

	@FunctionalInterface
	private interface Query<T> {
		T run(@NotNull Connection connection) throws SQLException;
	}

	@NotNull
	private <T> CompletableFuture<T> submit(@NotNull final Query<T> query) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				return query.run(connection);
			} catch (final SQLException ex) {
				throw new CompletionException(ex);
			}
		}, executor);
	}

	/**
	 * What a finished run changed: the player's and the track's best times before it, the player's completions with
	 * it, and the medals of the track already paid to the player.
	 */
	public record RunOutcome(@NotNull OptionalInt previousBest, @NotNull OptionalInt previousRecord, int completions,
							 @NotNull Set<String> paidMedals) {
		public boolean isTrackRecord(final int ticks) {
			return previousRecord.isEmpty() || ticks < previousRecord.getAsInt();
		}

		public boolean isPersonalBest(final int ticks) {
			return previousBest.isEmpty() || ticks < previousBest.getAsInt();
		}
	}

	/**
	 * Stores a completion.
	 */
	@NotNull
	public CompletableFuture<RunOutcome> recordRun(@NotNull final UUID track, @NotNull final UUID player, final int ticks,
			@NotNull final Instant finishedAt) {
		return submit(connection -> {
			final OptionalInt previousBest = queryBest(connection, track, player);
			final OptionalInt previousRecord = queryRecord(connection, track);
			try (final PreparedStatement insert = connection.prepareStatement(
					"INSERT INTO runs (track, player, ticks, finished_at) VALUES (?, ?, ?, ?)")) {
				insert.setString(1, track.toString());
				insert.setString(2, player.toString());
				insert.setInt(3, ticks);
				insert.setLong(4, finishedAt.toEpochMilli());
				insert.executeUpdate();
			}
			return new RunOutcome(previousBest, previousRecord, queryCompletions(connection, track, player),
					queryPaidMedals(connection, track, player));
		});
	}

	/**
	 * Notes a medal reward as paid; a second note of the same medal is ignored.
	 */
	@NotNull
	public CompletableFuture<Void> recordPayout(@NotNull final UUID track, @NotNull final UUID player,
			@NotNull final String medal, final double amount,
			@NotNull final Instant paidAt) {
		return submit(connection -> {
			try (final PreparedStatement insert = connection.prepareStatement(
					"INSERT OR IGNORE INTO medal_payouts (track, player, medal, amount, paid_at) VALUES (?, ?, ?, ?, ?)")) {
				insert.setString(1, track.toString());
				insert.setString(2, player.toString());
				insert.setString(3, medal);
				insert.setDouble(4, amount);
				insert.setLong(5, paidAt.toEpochMilli());
				insert.executeUpdate();
			}
			return null;
		});
	}

	/**
	 * A player who completed a track: their best time, and the medals of the track already paid to them.
	 */
	public record Standing(@NotNull UUID player, int bestTicks, @NotNull Set<String> paidMedals) {
	}

	/**
	 * Every player who completed the track, for paying medals retroactively.
	 */
	@NotNull
	public CompletableFuture<List<Standing>> fetchStandings(@NotNull final UUID track) {
		return submit(connection -> {
			final List<Standing> standings = new ArrayList<>();
			try (final PreparedStatement select = connection.prepareStatement(
					"SELECT player, MIN(ticks) FROM runs WHERE track = ? GROUP BY player ORDER BY player")) {
				select.setString(1, track.toString());
				try (final ResultSet rows = select.executeQuery()) {
					while (rows.next()) {
						final UUID player = UUID.fromString(rows.getString(1));
						standings.add(new Standing(player, rows.getInt(2), Set.of()));
					}
				}
			}
			final List<Standing> withPayouts = new ArrayList<>();
			for (final Standing standing : standings) {
				withPayouts.add(new Standing(standing.player(), standing.bestTicks(),
						queryPaidMedals(connection, track, standing.player())));
			}
			return withPayouts;
		});
	}

	/**
	 * A player's results on one track.
	 */
	public record PlayerResult(int bestTicks, int completions, @NotNull Instant lastFinished) {
	}

	@NotNull
	public CompletableFuture<Optional<PlayerResult>> fetchPlayerResult(@NotNull final UUID track, @NotNull final UUID player) {
		return submit(connection -> {
			try (final PreparedStatement select = connection.prepareStatement(
					"SELECT MIN(ticks), COUNT(*), MAX(finished_at) FROM runs WHERE track = ? AND player = ?")) {
				select.setString(1, track.toString());
				select.setString(2, player.toString());
				try (final ResultSet rows = select.executeQuery()) {
					if (!rows.next() || rows.getInt(2) == 0) {
						return Optional.empty();
					}
					return Optional.of(new PlayerResult(rows.getInt(1), rows.getInt(2),
							Instant.ofEpochMilli(rows.getLong(3))));
				}
			}
		});
	}

	/**
	 * A player's best on a track, as ranked against the other players.
	 */
	public record Ranked(@NotNull UUID player, int ticks) {
	}

	/**
	 * The player at the given place of a track's ranking: every player once, with their best time, the faster first
	 * and, at equal times, whoever got there first.
	 *
	 * @param place 1 for the record
	 */
	@NotNull
	public CompletableFuture<Optional<Ranked>> fetchRanked(@NotNull final UUID track, final int place) {
		return submit(connection -> {
			// SQLite takes the other columns of an aggregate query from the row MIN() picked
			try (final PreparedStatement select = connection.prepareStatement("""
					SELECT player, MIN(ticks) AS best, finished_at FROM runs WHERE track = ?
					GROUP BY player ORDER BY best, finished_at LIMIT 1 OFFSET ?""")) {
				select.setString(1, track.toString());
				select.setInt(2, place - 1);
				try (final ResultSet rows = select.executeQuery()) {
					return rows.next()
							? Optional.of(new Ranked(UUID.fromString(rows.getString(1)), rows.getInt(2)))
							: Optional.empty();
				}
			}
		});
	}

	/**
	 * A player's results on a track, for the summary of all tracks.
	 */
	public record TrackResult(@NotNull UUID track, int bestTicks, int completions) {
	}

	@NotNull
	public CompletableFuture<List<TrackResult>> fetchPlayerSummary(@NotNull final UUID player) {
		return submit(connection -> {
			try (final PreparedStatement select = connection.prepareStatement(
					"SELECT track, MIN(ticks), COUNT(*) FROM runs WHERE player = ? GROUP BY track")) {
				select.setString(1, player.toString());
				try (final ResultSet rows = select.executeQuery()) {
					final List<TrackResult> results = new ArrayList<>();
					while (rows.next()) {
						results.add(new TrackResult(UUID.fromString(rows.getString(1)), rows.getInt(2), rows.getInt(3)));
					}
					return results;
				}
			}
		});
	}

	/**
	 * Writes a consistent copy of the database to the file, which must not exist yet.
	 */
	@NotNull
	public CompletableFuture<Void> backUp(@NotNull final Path file) {
		return submit(connection -> {
			try (final PreparedStatement vacuum = connection.prepareStatement("VACUUM INTO ?")) {
				vacuum.setString(1, file.toAbsolutePath().toString());
				vacuum.execute();
			}
			return null;
		});
	}

	/**
	 * Waits until everything asked for so far is done; for tests.
	 */
	public void flush() {
		submit(connection -> null).join();
	}

	/**
	 * Finishes what was asked for, then closes the file.
	 */
	@Override
	public void close() {
		executor.shutdown();
		try {
			if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
				executor.shutdownNow();
			}
		} catch (final InterruptedException _) {
			Thread.currentThread().interrupt();
		}
		try {
			connection.close();
		} catch (final SQLException _) {
			// Closing on shutdown; nothing is left to save
		}
	}

	@NotNull
	private static OptionalInt queryBest(@NotNull final Connection connection, @NotNull final UUID track,
			@NotNull final UUID player) throws SQLException {
		try (final PreparedStatement select = connection.prepareStatement(
				"SELECT MIN(ticks) FROM runs WHERE track = ? AND player = ?")) {
			select.setString(1, track.toString());
			select.setString(2, player.toString());
			return querySingle(select);
		}
	}

	@NotNull
	private static OptionalInt queryRecord(@NotNull final Connection connection, @NotNull final UUID track)
			throws SQLException {
		try (final PreparedStatement select = connection.prepareStatement("SELECT MIN(ticks) FROM runs WHERE track = ?")) {
			select.setString(1, track.toString());
			return querySingle(select);
		}
	}

	@NotNull
	private static Set<String> queryPaidMedals(@NotNull final Connection connection, @NotNull final UUID track,
			@NotNull final UUID player) throws SQLException {
		try (final PreparedStatement select = connection.prepareStatement(
				"SELECT medal FROM medal_payouts WHERE track = ? AND player = ?")) {
			select.setString(1, track.toString());
			select.setString(2, player.toString());
			try (final ResultSet rows = select.executeQuery()) {
				final Set<String> medals = new HashSet<>();
				while (rows.next()) {
					medals.add(rows.getString(1));
				}
				return Set.copyOf(medals);
			}
		}
	}

	private static int queryCompletions(@NotNull final Connection connection, @NotNull final UUID track,
			@NotNull final UUID player) throws SQLException {
		try (final PreparedStatement select = connection.prepareStatement(
				"SELECT COUNT(*) FROM runs WHERE track = ? AND player = ?")) {
			select.setString(1, track.toString());
			select.setString(2, player.toString());
			return querySingle(select).orElse(0);
		}
	}

	@NotNull
	private static OptionalInt querySingle(@NotNull final PreparedStatement select) throws SQLException {
		try (final ResultSet rows = select.executeQuery()) {
			if (!rows.next()) {
				return OptionalInt.empty();
			}
			final int value = rows.getInt(1);
			return rows.wasNull() ? OptionalInt.empty() : OptionalInt.of(value);
		}
	}
}
