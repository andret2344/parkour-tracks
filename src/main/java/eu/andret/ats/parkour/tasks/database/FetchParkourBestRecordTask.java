/*
 * Copyright Andret (c) 2019-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.tasks.database;

import eu.andret.ats.parkour.parkour.ParkourGame;
import eu.andret.ats.parkour.parkour.ParkourRecord;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class FetchParkourBestRecordTask extends AbstractParkourTask {
	private final int count;
	private final Consumer<List<ParkourRecord>> callback;

	public FetchParkourBestRecordTask(final Connection connection, final ParkourGame parkourGame, final int count, final Consumer<List<ParkourRecord>> callback) {
		super(connection, parkourGame);
		if (count <= 0) {
			throw new IllegalArgumentException("Count must be positive, " + count + " provided");
		}
		this.count = count;
		this.callback = callback;
	}

	@Override
	public void run() {
		try (final PreparedStatement stat = connection.prepareStatement("SELECT nick, time FROM ats_parkour_records WHERE parkour = ? ORDER BY `time` LIMIT ?")) {
			stat.setString(1, parkourGame.getName());
			stat.setInt(2, count);
			final ResultSet rs = stat.executeQuery();
			final List<ParkourRecord> result = new ArrayList<>();
			for (int i = 0; i < count && rs.next(); i++) {
				result.add(new ParkourRecord(rs.getString("nick"), parkourGame, rs.getFloat("time")));
			}
			callback.accept(result);
		} catch (final SQLException ex) {
			ex.printStackTrace();
		}
	}
}
