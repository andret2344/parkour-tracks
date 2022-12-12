/*
 * Copyright Andret (c) 2018-2022. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.tasks.database;

import eu.andret.ats.parkour.ParkourPlugin;
import eu.andret.ats.parkour.parkour.ParkourGame;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
	Consumer<FetchResult> fetchDataCallback;

	public record FetchResult(int previousCount, double playerBestTime, double parkourBestTime) {
	}

	public FetchAndInsertDataTask(@NotNull final ParkourPlugin plugin,
								  @NotNull final ParkourGame game,
								  @NotNull final UUID uuid,
								  final double duration,
								  @Nullable final Consumer<FetchResult> fetchDataCallback) {
		super(plugin, game);
		this.uuid = uuid;
		this.duration = duration;
		this.fetchDataCallback = fetchDataCallback;
	}

	@Override
	public void go(@NotNull final Connection connection) {
		final double playerBestTime = getPlayerBestTime(connection);
		final double parkourBestTime = getParkourBestTime(connection);
		final int playerPassCount = getPlayerPassCount(connection);
		insertNewTime(connection);
		Optional.ofNullable(fetchDataCallback).ifPresent(callback -> callback.accept(new FetchResult(playerPassCount, playerBestTime, parkourBestTime)));
	}

	private int getPlayerPassCount(@NotNull final Connection connection) {
		try (final PreparedStatement stat = connection.prepareStatement("SELECT COUNT(*) AS result FROM ats_parkour_records WHERE uuid = ? AND parkour = ?")) {
			stat.setString(1, uuid.toString());
			stat.setString(2, game.getName());
			final ResultSet rs = stat.executeQuery();
			if (!rs.next()) {
				return 0;
			}
			return rs.getInt("result");
		} catch (final SQLException ex) {
			ex.printStackTrace();
		}
		return -1;
	}

	private double getPlayerBestTime(@NotNull final Connection connection) {
		try (final PreparedStatement stat = connection.prepareStatement("SELECT duration FROM ats_parkour_records WHERE uuid = ? AND parkour = ? ORDER BY duration LIMIT 1")) {
			stat.setString(1, uuid.toString());
			stat.setString(2, game.getName());
			final ResultSet rs = stat.executeQuery();
			if (!rs.next()) {
				return Double.POSITIVE_INFINITY;
			}
			return rs.getFloat("duration");
		} catch (final SQLException ex) {
			ex.printStackTrace();
		}
		return -1;
	}

	private double getParkourBestTime(@NotNull final Connection connection) {
		try (final PreparedStatement stat = connection.prepareStatement("SELECT  duration FROM ats_parkour_records WHERE parkour = ? ORDER BY duration LIMIT 1")) {
			stat.setString(1, game.getName());
			final ResultSet rs = stat.executeQuery();
			if (!rs.next()) {
				return Double.POSITIVE_INFINITY;
			}
			return rs.getFloat("duration");
		} catch (final SQLException ex) {
			ex.printStackTrace();
		}
		return -1;
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
