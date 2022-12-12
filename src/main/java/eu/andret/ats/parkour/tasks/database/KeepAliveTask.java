/*
 * Copyright Andret (c) 2018-2022. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.tasks.database;

import eu.andret.ats.parkour.ParkourPlugin;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class KeepAliveTask extends AbstractTask {
	public KeepAliveTask(@NotNull final ParkourPlugin plugin) {
		super(plugin);
	}

	@Override
	public void go(@NotNull final Connection connection) {
		try (final PreparedStatement statement = connection.prepareStatement("SELECT id FROM ats_parkour_records WHERE id < 0")) {
			statement.executeQuery();
		} catch (final SQLException ex) {
			ex.printStackTrace();
		}
	}
}
