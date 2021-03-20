/*
 * Copyright Andret (c) 2019-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.tasks;

import eu.andret.ats.parkour.parkour.ParkourGame;
import lombok.AllArgsConstructor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.BiConsumer;

@AllArgsConstructor
public class RepairSignTask implements Runnable {
	private final Connection connection;
	private final ParkourGame parkour;
	private final BiConsumer<String, Float> callback;

	@Override
	public void run() {
		try (final PreparedStatement stat = connection.prepareStatement("SELECT nick, time FROM ats_parkour_records WHERE parkour = ? ORDER BY `time` LIMIT 1")) {
			stat.setString(1, parkour.getName());
			final ResultSet rs = stat.executeQuery();
			if (!rs.next()) {
				callback.accept("========", 0F);
			} else {
				callback.accept(rs.getString("nick"), rs.getFloat("time"));
			}
			rs.close();
		} catch (final SQLException ex) {
			ex.printStackTrace();
		}
	}
}
