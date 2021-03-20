/*
 * Copyright Andret (c) 2019-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.tasks.database;

import eu.andret.ats.parkour.ParkourPlugin;
import eu.andret.ats.parkour.parkour.Medal;
import eu.andret.ats.parkour.parkour.ParkourGame;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class DataBaseOperations extends AbstractParkourTask {
	ParkourPlugin plugin;
	Player player;
	float time;

	public DataBaseOperations(final Connection connection, final ParkourGame parkour, final ParkourPlugin plugin, final Player player, final float time) {
		super(connection, parkour);
		this.plugin = plugin;
		this.player = player;
		this.time = time;
	}

	@Override
	public void run() {
		if (!player.hasPermission("ats.parkour.ignoreRecords")) {
			verifyParkourBestTime();
		}
		// Collect players data
		try (final PreparedStatement stat = connection.prepareStatement("SELECT * FROM ats_parkour_records WHERE nick = ? AND parkour = ?")) {
			stat.setString(1, player.getName());
			stat.setString(2, parkourGame.getName());

			final ResultSet rs = stat.executeQuery();
			Medal lastMedal = Medal.NONE;
			// Was there the record?
			if (rs.next()) {
				final int count = rs.getInt("count");
				final float f = rs.getFloat("time");
				lastMedal = verifyPlayerBestTime(count, f);
			} else {
				insertNewRecord();
			}
			rs.close();
			calculateMedals(lastMedal);
		} catch (final SQLException ex) {
			ex.printStackTrace();
		}
	}

	private void calculateMedals(final Medal lastMedal) {
		// What medal to give?
		final Medal current = parkourGame.getOptions().getMedalByTime(0/*PlayerManager.getParkourSinglePlayer(player).getTime()*/);
		if (lastMedal.ordinal() > current.ordinal()) {
			// How much does it cost?
			int price = 0;
			for (final Medal medal : Medal.values()) {
				price += medal.getPrice();
			}
			player.sendMessage(plugin.msg("achieveMedal", false).replace("%MEDAL%", current.name()).replace("%PRICE%", "" + price));
		}
	}

	private Medal verifyPlayerBestTime(final int count, final float f) throws SQLException {
		final String s;
		// Is best?
		if (f > time) {
			s = "UPDATE ats_parkour_records SET time = " + time + ", count = ?, date = ? WHERE nick = ? AND parkour = ?";
			player.sendMessage(plugin.msg("newRecord", false));
		} else {
			s = "UPDATE ats_parkour_records SET count = ?, date = ? WHERE nick = ? AND parkour = ?";
		}
		final PreparedStatement stat = connection.prepareStatement(s);
		stat.setInt(1, count + 1);
		stat.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
		stat.setString(3, player.getName());
		stat.setString(4, parkourGame.getName());
		stat.execute();
		// What medal was previously?
		player.sendMessage(plugin.msg("howMany", false).replace("%COUNT%", "1"));
		return parkourGame.getOptions().getMedalByTime(f);
	}

	private void insertNewRecord() {
		try (final PreparedStatement stat = connection.prepareStatement("INSERT INTO ats_parkour_records VALUES(null, ?, ?, ?, ?, 1)")) {
			stat.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
			stat.setString(2, player.getName());
			stat.setString(3, parkourGame.getName());
			stat.setFloat(4, time);
			stat.execute();
			player.sendMessage(plugin.msg("newRecord", false));
			player.sendMessage(plugin.msg("howMany", false).replace("%COUNT%", "1"));
		} catch (final SQLException ex) {
			ex.printStackTrace();
		}
	}

	private void verifyParkourBestTime() {
		try (final PreparedStatement stat = connection.prepareStatement("SELECT time FROM ats_parkour_records WHERE parkour = ? ORDER BY time LIMIT 1")) {
			stat.setString(1, parkourGame.getName());
			final ResultSet rs = stat.executeQuery();
			if (!rs.next() || rs.getFloat("time") > time) {
				// First attempt or the best time
				player.sendMessage(plugin.msg("generalRecord", false));
				plugin.updateSign(player.getName(), time, parkourGame);
			}
			rs.close();
		} catch (final SQLException ex) {
			ex.printStackTrace();
		}
	}
}
