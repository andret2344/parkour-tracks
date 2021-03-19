/*
 * Copyright Andret (c) 2019. Copying and modifying allowed only keeping git link reference.
 */
package eu.andret.ats.parkour.tasks;

import eu.andret.ats.parkour.ParkourPlugin;
import eu.andret.ats.parkour.parkour.ParkourGame;
import lombok.Value;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@Value
public class TopPlayersDataOperations implements Runnable {
	ParkourPlugin plugin;
	ParkourGame parkour;
	int count;
	Consumer<TopPlayersDataOperations> callback;
	Map<String, Float> result = new HashMap<>();

	@Override
	public void run() {
		try {
			final PreparedStatement stat = plugin.getConnection().prepareStatement("SELECT nick, time FROM ats_parkour_records WHERE parkour=? ORDER BY time LIMIT 10");
			stat.setString(1, parkour.getName());
			final ResultSet rs = stat.executeQuery();
			for (int i = 0; i < count && rs.next(); i++) {
				result.put(rs.getString("nick"), rs.getFloat("time"));
			}
			callback.accept(this);
		} catch (final Exception ex) {
			plugin.getServer().getLogger().throwing(getClass().getName(), "run", ex);
		}
	}

	public Map<String, Float> getResult() {
		return result;
	}
}
