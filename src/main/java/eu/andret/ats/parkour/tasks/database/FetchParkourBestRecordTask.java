/*
 * Copyright Andret (c) 2018-2022. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.tasks.database;

import eu.andret.ats.parkour.parkour.ParkourGame;
import eu.andret.ats.parkour.parkour.Score;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public class FetchParkourBestRecordTask extends AbstractParkourTask {
	private final int count;
	@Nullable
	private final Consumer<List<Score>> callback;

	public FetchParkourBestRecordTask(@NotNull final Connection connection, @NotNull final ParkourGame parkourGame, final int count, @Nullable final Consumer<List<Score>> callback) {
		super(connection, parkourGame);
		if (count <= 0) {
			throw new IllegalArgumentException("Count must be positive, " + count + " provided!");
		}
		this.count = count;
		this.callback = callback;
	}

	@Override
	public void run() {
		try (final PreparedStatement stat = connection.prepareStatement("SELECT uuid, duration FROM ats_parkour_records WHERE parkour = ? ORDER BY duration LIMIT ?")) {
			stat.setString(1, game.getName());
			stat.setInt(2, count);
			final ResultSet rs = stat.executeQuery();
			final List<Score> result = new ArrayList<>();
			for (int i = 0; i < count && rs.next(); i++) {
				result.add(new Score(UUID.fromString(rs.getString("uuid")), game, rs.getFloat("duration")));
			}
			Optional.ofNullable(callback).ifPresent(cb -> cb.accept(result));
		} catch (final SQLException ex) {
			ex.printStackTrace();
		}
	}
}
