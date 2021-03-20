/*
 * Copyright Andret (c) 2019-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.tasks;

import eu.andret.ats.parkour.parkour.ParkourGame;
import lombok.Value;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@Value
public class TopPlayersDataOperations implements Runnable {
	Connection connection;
	ParkourGame parkour;
	int count;
	Consumer<Map<String, Float>> callback;

	@Override
	public void run() {
		try (final PreparedStatement stat = connection.prepareStatement("SELECT nick, time FROM ats_parkour_records WHERE parkour=? ORDER BY time LIMIT 10")) {
			stat.setString(1, parkour.getName());
			final ResultSet rs = stat.executeQuery();
			final Map<String, Float> result = new HashMap<>();
			for (int i = 0; i < count && rs.next(); i++) {
				result.put(rs.getString("nick"), rs.getFloat("time"));
			}
			callback.accept(result);
		} catch (final SQLException ex) {
			ex.printStackTrace();
		}
	}
}
