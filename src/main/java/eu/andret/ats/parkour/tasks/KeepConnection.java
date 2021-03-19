/*
 * Copyright Andret (c) 2019. Copying and modifying allowed only keeping git link reference.
 */
package eu.andret.ats.parkour.tasks;

import lombok.AllArgsConstructor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@AllArgsConstructor
public class KeepConnection implements Runnable {
	private final Connection connection;

	@Override
	public void run() {
		try (final PreparedStatement statement = connection.prepareStatement("SELECT id FROM ats_parkour_records WHERE id < 0")) {
			statement.executeQuery();
		} catch (final SQLException ex) {
			ex.printStackTrace();
		}
	}
}
