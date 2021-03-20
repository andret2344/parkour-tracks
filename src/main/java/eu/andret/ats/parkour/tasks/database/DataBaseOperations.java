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
import java.util.Optional;

public class DataBaseOperations extends AbstractParkourTask {
	Player player;
	float time;
	PlayerBeatenRecordListener playerBeatenRecordListener;

	public interface PlayerBeatenRecordListener {
		void onPlayerBeatenRecord(float oldTime);
	}

	public DataBaseOperations(final Connection connection, final ParkourGame parkourGame, final Player player, final float time, final PlayerBeatenRecordListener playerBeatenRecordListener) {
		super(connection, parkourGame);
		this.player = player;
		this.time = time;
		this.playerBeatenRecordListener = playerBeatenRecordListener;
	}

	@Override
	public void run() {
		final Optional<Float> playerBestTimeOnParkour = getPlayerBestTimeOnParkour();
		insertNewTime();
		if (playerBeatenRecordListener == null) {
			return;
		}
		playerBestTimeOnParkour
				.filter(i -> i > time)
				.ifPresent(i -> playerBeatenRecordListener.onPlayerBeatenRecord(i));
	}

	private Optional<Float> getPlayerBestTimeOnParkour() {
		try (final PreparedStatement stat = connection.prepareStatement("SELECT time FROM ats_parkour_records WHERE nick = ? AND parkour = ? ORDER BY time LIMIT 1")) {
			stat.setString(1, player.getName());
			stat.setString(2, parkourGame.getName());
			final ResultSet rs = stat.executeQuery();
			if (!rs.next()) {
				return Optional.empty();
			}
			return Optional.of(rs.getFloat("time"));
		} catch (final SQLException ex) {
			ex.printStackTrace();
		}
		return Optional.empty();
	}

	private void insertNewTime() {
		try (final PreparedStatement stat = connection.prepareStatement("INSERT INTO ats_parkour_records VALUES(null, ?, ?, ?, ?)")) {
			stat.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
			stat.setString(2, player.getName());
			stat.setString(3, parkourGame.getName());
			stat.setFloat(4, time);
			stat.execute();
		} catch (final SQLException ex) {
			ex.printStackTrace();
		}
	}
}
