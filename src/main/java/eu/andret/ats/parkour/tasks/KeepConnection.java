/*
 * Copyright Andret (c) 2019. Copying and modifying allowed only keeping git link reference.
 */
package eu.andret.ats.parkour.tasks;

import eu.andret.ats.parkour.ParkourPlugin;
import eu.andret.ats.parkour.util.Data;
import lombok.AllArgsConstructor;
import org.bukkit.Bukkit;

@AllArgsConstructor
public class KeepConnection implements Runnable {
	private final ParkourPlugin plugin;

	@Override
	public void run() {
		try {
			plugin.getConnection().prepareStatement("SELECT id FROM " + Data.TABLE_RECORDS + " WHERE id<0").executeQuery();
		} catch (final Exception ex) {
			Bukkit.getServer().getLogger().throwing(getClass().getName(), "run", ex);
		}
	}
}
