/*
 * Copyright Andret (c) 2019. Copying and modifying allowed only keeping git link reference.
 */
package eu.andret.ats.parkour.tasks;

import eu.andret.ats.parkour.ParkourPlugin;
import eu.andret.ats.parkour.parkour.ParkourGame;
import lombok.Getter;
import org.bukkit.entity.Player;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.function.Consumer;

public class ParkourDataOperations implements Runnable {
	private final Consumer<ParkourDataOperations> callback;
	private final ParkourGame parkour;
	private final Player player;
	private final ParkourPlugin plugin;
	@Getter
	private float time;
	@Getter
	private float bestTime;
	@Getter
	private int count;
	@Getter
	private int earned;

	public ParkourDataOperations(final ParkourPlugin plugin, final ParkourGame parkour, final Player player, final Consumer<ParkourDataOperations> callback) {
		this.parkour = parkour;
		this.player = player;
		this.callback = callback;
		this.plugin = plugin;
	}

	@Override
	public void run() {
		ResultSet rs;
		try {
			PreparedStatement stat = plugin.getConnection().prepareStatement("SELECT time FROM ats_parkour_records WHERE parkour=? ORDER BY time LIMIT 1");
			stat.setString(1, parkour.getName());
			rs = stat.executeQuery();
			time = rs.next() ? rs.getFloat("time") : 0;

			stat = plugin.getConnection().prepareStatement("SELECT * FROM ats_parkour_records WHERE parkour = ? AND nick = ?");
			stat.setString(1, parkour.getName());
			stat.setString(2, player.getName());
			rs = stat.executeQuery();
			if (rs.next()) {
				bestTime = rs.getFloat("time");
				count = rs.getInt("count");
				earned = rs.getInt("earned");
			} else {
				bestTime = 0;
				count = 0;
				earned = 0;
			}
			callback.accept(this);
			rs.close();
		} catch (final Exception ex) {
			plugin.getServer().getLogger().throwing(getClass().getName(), "run", ex);
		}
	}
}
