/*
 * Copyright Andret (c) 2019-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.tasks.database;

import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class KeepAliveTask extends AbstractTask {
	public KeepAliveTask(@NotNull final Connection connection) {
		super(connection);
	}

	@Override
	public void run() {
		try (final PreparedStatement statement = connection.prepareStatement("SELECT id FROM ats_parkour_records WHERE id < 0")) {
			statement.executeQuery();
		} catch (final SQLException ex) {
			ex.printStackTrace();
		}
	}
}
