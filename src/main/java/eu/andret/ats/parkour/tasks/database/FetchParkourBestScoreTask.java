/*
 * Copyright Andret (c) 2018-2022. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.tasks.database;

import eu.andret.ats.parkour.ParkourPlugin;
import eu.andret.ats.parkour.parkour.ParkourGame;
import eu.andret.ats.parkour.parkour.ParkourScore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public class FetchParkourBestScoreTask extends AbstractParkourTask {
	private final int count;
	@Nullable
	private final Consumer<List<ParkourScore>> callback;

	public FetchParkourBestScoreTask(@NotNull final ParkourPlugin plugin, @NotNull final ParkourGame game, final int count, @Nullable final Consumer<List<ParkourScore>> callback) {
		super(plugin, game);
		if (count <= 0) {
			throw new IllegalArgumentException("Count must be positive, " + count + " provided!");
		}
		this.count = count;
		this.callback = callback;
	}

	@Override
	public void go(@NotNull final Connection connection) {
		try (final PreparedStatement stat = connection.prepareStatement(plugin.load("sql/sign.sql"))) {
			stat.setString(1, game.getName());
			stat.setInt(2, count);
			final ResultSet rs = stat.executeQuery();
			final List<ParkourScore> result = new ArrayList<>();
			for (int i = 0; i < count && rs.next(); i++) {
				result.add(new ParkourScore(UUID.fromString(rs.getString("uuid")), game, rs.getFloat("duration")));
			}
			Optional.ofNullable(callback).ifPresent(cb -> cb.accept(result));
		} catch (final SQLException | IOException ex) {
			ex.printStackTrace();
		}
	}
}
