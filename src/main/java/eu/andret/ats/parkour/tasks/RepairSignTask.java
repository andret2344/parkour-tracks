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

@AllArgsConstructor
public class RepairSignTask implements Runnable {
	private final ParkourPlugin plugin;
	private final ParkourGame parkour;

	@Override
	public void run() {
		try {
			final PreparedStatement stat = plugin.getConnection().prepareStatement(String.format("SELECT * FROM %s "
					+ "WHERE parkour=? ORDER BY `time` ASC LIMIT 1", Data.TABLE_RECORDS));
			stat.setString(1, parkour.getName());
			final ResultSet rs = stat.executeQuery();
			if (!rs.next()) {
				DataBaseOperations.updateSign(plugin, "========", 0.00, parkour);
			} else {
				DataBaseOperations.updateSign(plugin, rs.getString("nick"), rs.getFloat("time"), parkour);
			}
			rs.close();
		} catch (final Exception ex) {
			Bukkit.getServer().getLogger().throwing(getClass().getName(), "run", ex);
		}
	}
}
