/*
 * Copyright Andret (c) 2019-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.tasks.database;

import eu.andret.ats.parkour.parkour.ParkourGame;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class DataBaseOperations extends AbstractParkourTask {
	Player player;
	double time;
	DataBaseOperationListener dataBaseOperationListener;

	public interface DataBaseOperationListener {
		void onDataBaseOperation(int previousCount, double previousPlayerBest, double previousParkourBest);
	}

	public DataBaseOperations(final Connection connection, final ParkourGame parkourGame, final Player player, final double time, final DataBaseOperationListener dataBaseOperationListener) {
		super(connection, parkourGame);
		this.player = player;
		this.time = time;
		this.dataBaseOperationListener = dataBaseOperationListener;
	}

	@Override
	public void run() {
		final double playerBestTime = getPlayerBestTime();
		final double parkourBestTime = getParkourBestTime();
		final int playerPassCount = getPlayerPassCount();
		insertNewTime();
		dataBaseOperationListener.onDataBaseOperation(playerPassCount, playerBestTime, parkourBestTime);
	}

	private int getPlayerPassCount() {
		try (final PreparedStatement stat = connection.prepareStatement("SELECT COUNT(*) AS result FROM ats_parkour_records WHERE nick = ? AND parkour = ?")) {
			stat.setString(1, player.getName());
			stat.setString(2, parkourGame.getName());
			final ResultSet rs = stat.executeQuery();
			if (!rs.next()) {
				return -1;
			}
			return rs.getInt("result");
		} catch (final SQLException ex) {
			ex.printStackTrace();
		}
		return -1;
	}

	private double getPlayerBestTime() {
		try (final PreparedStatement stat = connection.prepareStatement("SELECT time FROM ats_parkour_records WHERE nick = ? AND parkour = ? ORDER BY time LIMIT 1")) {
			stat.setString(1, player.getName());
			stat.setString(2, parkourGame.getName());
			final ResultSet rs = stat.executeQuery();
			if (!rs.next()) {
				return -1;
			}
			return rs.getFloat("time");
		} catch (final SQLException ex) {
			ex.printStackTrace();
		}
		return -1;
	}

	private double getParkourBestTime() {
		try (final PreparedStatement stat = connection.prepareStatement("SELECT time FROM ats_parkour_records WHERE parkour = ? ORDER BY time LIMIT 1")) {
			stat.setString(1, parkourGame.getName());
			final ResultSet rs = stat.executeQuery();
			if (!rs.next()) {
				return -1;
			}
			return rs.getFloat("time");
		} catch (final SQLException ex) {
			ex.printStackTrace();
		}
		return -1;
	}

	private void insertNewTime() {
		try (final PreparedStatement stat = connection.prepareStatement("INSERT INTO ats_parkour_records VALUES(null, ?, ?, ?, ?)")) {
			stat.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
			stat.setString(2, player.getName());
			stat.setString(3, parkourGame.getName());
			stat.setDouble(4, time);
			stat.execute();
		} catch (final SQLException ex) {
			ex.printStackTrace();
		}
	}
}
