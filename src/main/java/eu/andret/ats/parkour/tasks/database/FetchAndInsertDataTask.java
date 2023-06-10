/*
 * Copyright Andret (c) 2018. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.tasks.database;

import eu.andret.ats.parkour.ParkourPlugin;
import eu.andret.ats.parkour.parkour.ParkourGame;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public class FetchAndInsertDataTask extends AbstractParkourTask {
	@NotNull
	UUID uuid;
	double duration;
	@Nullable
	Consumer<@NotNull Result> fetchDataCallback;

	public record Result(int previousCount, double playerBestTime, double parkourBestTime) {
	}

	public FetchAndInsertDataTask(@NotNull final ParkourPlugin plugin,
								  @NotNull final ParkourGame game,
								  @NotNull final UUID uuid,
								  final double duration,
								  @Nullable final Consumer<@NotNull Result> fetchDataCallback) {
		super(plugin, game);
		this.uuid = uuid;
		this.duration = duration;
		this.fetchDataCallback = fetchDataCallback;
	}

	@Override
	public void go(@NotNull final Connection connection) {
		final Result result = getPlayerPassCount(connection);
		insertNewTime(connection);
		Optional.ofNullable(fetchDataCallback).ifPresent(callback ->
				callback.accept(Optional.ofNullable(result)
						.orElse(new Result(0, 999999999, 9999999))));
	}

	@Nullable
	private FetchAndInsertDataTask.Result getPlayerPassCount(@NotNull final Connection connection) {
		try (final PreparedStatement stat = connection.prepareStatement(plugin.load("sql/player-best-score.sql"))) {
			stat.setString(1, uuid.toString());
			stat.setString(2, game.getName());
			final ResultSet rs = stat.executeQuery();
			if (rs.next()) {
				return new Result(
						rs.getInt("count"),
						rs.getDouble("player_time"),
						rs.getDouble("parkour_time"));
			}
			return null;
		} catch (final SQLException | IOException ex) {
			ex.printStackTrace();
		}
		return null;
	}

	private void insertNewTime(@NotNull final Connection connection) {
		try (final PreparedStatement stat = connection.prepareStatement("INSERT INTO ats_parkour_records VALUES(NULL, ?, ?, ?, ?)")) {
			stat.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
			stat.setString(2, uuid.toString());
			stat.setString(3, game.getName());
			stat.setDouble(4, duration);
			stat.execute();
		} catch (final SQLException ex) {
			ex.printStackTrace();
		}
	}
}
