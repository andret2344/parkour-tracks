/*
 * Copyright Andret (c) 2019. Copying and modifying allowed only keeping git link reference.
 */
package eu.andret.parkour.tasks;

import eu.andret.parkour.ParkourPlugin;
import eu.andret.parkour.parkour.ParkourGame;
import eu.andret.parkour.util.Data;
import lombok.Getter;
import org.bukkit.Bukkit;
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

	public ParkourDataOperations(ParkourPlugin plugin, ParkourGame parkour, Player player, Consumer<ParkourDataOperations> callback) {
		this.parkour = parkour;
		this.player = player;
		this.callback = callback;
		this.plugin = plugin;
	}

	@Override
	public void run() {
		ResultSet rs;
		try {
			PreparedStatement stat = plugin.getConnection().prepareStatement(String.format("SELECT time FROM %s "
					+ "WHERE parkour=? ORDER BY time LIMIT 1", Data.TABLE_RECORDS));
			stat.setString(1, parkour.getName());
			rs = stat.executeQuery();
			time = rs.next() ? rs.getFloat("time") : 0;

			stat = plugin.getConnection().prepareStatement(String.format("SELECT * FROM %s "
					+ "WHERE parkour=? AND nick=?", Data.TABLE_RECORDS));
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
		} catch (Exception ex) {
			Bukkit.getLogger().throwing(getClass().getName(), "run", ex);
		}
	}
}
