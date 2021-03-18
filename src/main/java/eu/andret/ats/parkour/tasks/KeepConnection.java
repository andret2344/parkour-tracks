/*
 * Copyright Andret (c) 2019. Copying and modifying allowed only keeping git link reference.
 */
package eu.andret.ats.parkour.tasks;

import eu.andret.ats.parkour.ParkourPlugin;
import lombok.AllArgsConstructor;
import org.bukkit.Bukkit;

import java.sql.PreparedStatement;

@AllArgsConstructor
public class KeepConnection implements Runnable {
	private final ParkourPlugin plugin;

	@Override
	public void run() {
		try (final PreparedStatement statement = plugin.getConnection().prepareStatement("SELECT id FROM ats_parkour_records WHERE id < 0")) {
			statement.executeQuery();
		} catch (final Exception ex) {
			Bukkit.getServer().getLogger().throwing(getClass().getName(), "run", ex);
		}
	}
}
