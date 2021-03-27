/*
 * Copyright Andret (c) 2019-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.tasks.database;

import eu.andret.ats.parkour.parkour.ParkourGame;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.UUID;

public class FetchAndInsertDataTask extends AbstractParkourTask {
	UUID uuid;
	double time;
	FetchDataCallback fetchDataCallback;

	public interface FetchDataCallback {
		void onFetchData(int previousCount, double previousPlayerBest, double previousParkourBest);
	}

	public FetchAndInsertDataTask(final Connection connection, final ParkourGame game, final UUID uuid, final double time, final FetchDataCallback fetchDataCallback) {
		super(connection, game);
		this.uuid = uuid;
		this.time = time;
		this.fetchDataCallback = fetchDataCallback;
	}

	@Override
	public void run() {
		final double playerBestTime = getPlayerBestTime();
		final double parkourBestTime = getParkourBestTime();
		final int playerPassCount = getPlayerPassCount();
		insertNewTime();
		fetchDataCallback.onFetchData(playerPassCount, playerBestTime, parkourBestTime);
	}

	private int getPlayerPassCount() {
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

	private double getPlayerBestTime() {
		try (final PreparedStatement stat = connection.prepareStatement("SELECT time FROM ats_parkour_records WHERE uuid = ? AND parkour = ? ORDER BY time LIMIT 1")) {
			stat.setString(1, uuid.toString());
			stat.setString(2, game.getName());
			final ResultSet rs = stat.executeQuery();
			if (!rs.next()) {
				return Double.POSITIVE_INFINITY;
			}
			return rs.getFloat("time");
		} catch (final SQLException ex) {
			ex.printStackTrace();
		}
		return -1;
	}

	private double getParkourBestTime() {
		try (final PreparedStatement stat = connection.prepareStatement("SELECT time FROM ats_parkour_records WHERE parkour = ? ORDER BY time LIMIT 1")) {
			stat.setString(1, game.getName());
			final ResultSet rs = stat.executeQuery();
			if (!rs.next()) {
				return Double.POSITIVE_INFINITY;
			}
			return rs.getFloat("time");
		} catch (final SQLException ex) {
			ex.printStackTrace();
		}
		return -1;
	}

	private void insertNewTime() {
		try (final PreparedStatement stat = connection.prepareStatement("INSERT INTO ats_parkour_records VALUES(NULL, ?, ?, ?, ?)")) {
			stat.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
			stat.setString(2, uuid.toString());
			stat.setString(3, game.getName());
			stat.setDouble(4, time);
			stat.execute();
		} catch (final SQLException ex) {
			ex.printStackTrace();
		}
	}
}
