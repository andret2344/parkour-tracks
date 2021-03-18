/*
 * Copyright Andret (c) 2019. Copying and modifying allowed only keeping git link reference.
 */
package eu.andret.ats.parkour.tasks;

import eu.andret.ats.parkour.ParkourPlugin;
import eu.andret.ats.parkour.parkour.ParkourGame;
import eu.andret.ats.parkour.player.PlayerManager;
import eu.andret.ats.parkour.util.Medal;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;

public class DataBaseOperations implements Runnable {
	private final Player player;
	private final ParkourGame parkour;
	private final ParkourPlugin plugin;

	private final float time;
	private Medal lastMedal = Medal.NONE;
	private final Connection sql;

	public DataBaseOperations(final ParkourPlugin plugin, final Player player, final ParkourGame parkour, final float time) {
		this.player = player;
		this.parkour = parkour;
		this.time = time;
		this.plugin = plugin;
		sql = plugin.getConnection();
	}

	@Override
	public void run() {
		final ParkourGame.Options o = parkour.getOptions();
		try {
			//przeszedlem kolejny raz
			PreparedStatement stat;
			if (!player.hasPermission("ats.parkour.ignorerecords")) {
				//czy moj czas jest najlepszy?
				stat = sql.prepareStatement("SELECT time FROM ats_parkour_records WHERE parkour = ? ORDER BY time ASC LIMIT 1");
				stat.setString(1, parkour.getName());
				final ResultSet rs = stat.executeQuery();
				if (!rs.next() || rs.getFloat("time") > time) {
					//tak, jest najlepszy lub nie bylo zadnego
					player.sendMessage(plugin.msg("generalRecord", false));
					updateSign(plugin, player.getName(), time, parkour);
				}
				rs.close();
			}
			//biore wszystkie dane gracza
			stat = sql.prepareStatement("SELECT * FROM ats_parkour_records WHERE nick = ? AND parkour = ?");
			stat.setString(1, player.getName());
			stat.setString(2, parkour.getName());
			final ResultSet rs = stat.executeQuery();

			int c = 0;
			//czy byl wpis?
			if (rs.next()) {
				c = rs.getInt("count");
				final float f = rs.getFloat("time");
				final String s;
				//czy najlepszy?
				if (f > time) {
					s = "UPDATE ats_parkour_records SET time = " + time + ", count = ?, date = ? WHERE nick = ? AND parkour = ?";
					player.sendMessage(plugin.msg("newRecord", false));
				} else {
					s = "UPDATE ats_parkour_records SET count = ?, date = ? WHERE nick = ? AND parkour = ?";
				}
				stat = sql.prepareStatement(s);
				stat.setInt(1, c + 1);
				stat.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
				stat.setString(3, player.getName());
				stat.setString(4, parkour.getName());
				stat.execute();
				//a jaki byl ostatni medal?
				lastMedal = o.getMedalByTime(f);
			} else {
				stat = sql.prepareStatement("INSERT INTO ats_parkour_records VALUES(null, ?, ?, ?, ?, 1)");
				stat.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
				stat.setString(2, player.getName());
				stat.setString(3, parkour.getName());
				stat.setFloat(4, time);
				stat.execute();
				player.sendMessage(plugin.msg("newRecord", false));
			}
			player.sendMessage(plugin.msg("howMany", false).replace("%COUNT%", c + 1 + ""));
			//jaki dac medal?
			final Medal current = o.getMedalByTime(PlayerManager.getParkourSinglePlayer(player).getTime());
			if (lastMedal.ordinal() > current.ordinal()) {
				//ile za niego i poprzednie zarabia?
				int price = 0;
				for (final Medal medal : Medal.values()) {
					price += medal.getPrice();
				}
				player.sendMessage(plugin.msg("achieveMedal", false).replace("%MEDAL%", current.name()).replace("%PRICE%", "" + price));
			}
			rs.close();
		} catch (final Exception ex) {
			Bukkit.getServer().getLogger().throwing(getClass().getName(), "run", ex);
		}
	}

	public static void updateSign(final ParkourPlugin plugin, final String player, final double time, final ParkourGame parkour) {
		final Location l = parkour.getBestRecord();
		if (l != null) {
			final Sign s = (Sign) l.getBlock().getState();
			final FileConfiguration c = plugin.getConfig();
			s.setLine(0, replace(c.getString("recordSign.line1"), player, time).replace('&', '\u00A7'));
			s.setLine(1, replace(c.getString("recordSign.line2"), player, time).replace('&', '\u00A7'));
			s.setLine(2, replace(c.getString("recordSign.line3"), player, time).replace('&', '\u00A7'));
			s.setLine(3, replace(c.getString("recordSign.line4"), player, time).replace('&', '\u00A7'));
			s.update();
		}
	}

	private static String replace(String s, final String nick, final double time) {
		s = s.replace("%NICK%", nick);
		final int mins = (int) time / 60;
		s = s.replace("%MINUTES%", ("" + (mins < 10 ? "0" + mins : mins)).substring(0, 2));
		final int secs = (int) time % 60;
		s = s.replace("%SECONDS%", "" + ("" + (secs < 10 ? "0" + secs : secs)).substring(0, 2));
		final int milisecs = (int) Math.round((time % 1) * 100);
		s = s.replace("%MILISECONDS%", "" + ("" + (milisecs < 10 ? "0" + milisecs : milisecs)).substring(0, 2));
		return s;
	}
}
