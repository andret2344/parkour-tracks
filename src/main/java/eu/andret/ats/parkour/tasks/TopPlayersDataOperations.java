/*
 * Copyright Andret (c) 2019. Copying and modifying allowed only keeping git link reference.
 */
package eu.andret.ats.parkour.tasks;

import eu.andret.ats.parkour.ParkourPlugin;
import eu.andret.ats.parkour.parkour.ParkourGame;
import eu.andret.ats.parkour.util.Data;
import lombok.AllArgsConstructor;
import org.bukkit.Bukkit;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@AllArgsConstructor
public class TopPlayersDataOperations implements Runnable {
	private final ParkourPlugin plugin;
	private final ParkourGame parkour;
	private final int count;
	private final Consumer<TopPlayersDataOperations> callback;
	private final Map<String, Float> result = new HashMap<>();

	@Override
	public void run() {
		try {
			PreparedStatement stat = plugin.getConnection().prepareStatement(String.format("SELECT nick, time FROM %s "
					+ "WHERE parkour=? ORDER BY time LIMIT 10", Data.TABLE_RECORDS));
			stat.setString(1, parkour.getName());
			ResultSet rs = stat.executeQuery();
			for (int i = 0; i < count && rs.next(); i++) {
				result.put(rs.getString("nick"), rs.getFloat("time"));
			}
			callback.accept(this);
		} catch (Exception ex) {
			Bukkit.getServer().getLogger().throwing(getClass().getName(), "run", ex);
		}
	}

	public Map<String, Float> getResult() {
		return result;
	}
}
