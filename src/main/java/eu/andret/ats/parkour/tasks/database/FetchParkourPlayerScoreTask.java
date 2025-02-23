/*
 * Copyright Andret Tools System (c) 2025. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.tasks.database;

import eu.andret.ats.parkour.ParkourPlugin;
import eu.andret.ats.parkour.parkour.ParkourGame;
import eu.andret.ats.parkour.player.ParkourPlayer;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.function.Consumer;

public class FetchParkourPlayerScoreTask extends AbstractParkourTask {
	@NotNull
	private final ParkourPlayer player;
	@NotNull
	private final Consumer<Result> consumer;

	public record Result(double playerTime, double parkourTIme, @NotNull LocalDateTime lastRun, int count) {
	}

	public FetchParkourPlayerScoreTask(@NotNull final ParkourPlugin plugin,
									   @NotNull final ParkourGame game,
									   @NotNull final ParkourPlayer player,
									   @NotNull final Consumer<Result> consumer) {
		super(plugin, game);
		this.player = player;
		this.consumer = consumer;
	}

	@Override
	public void go(@NotNull final Connection connection) {
		try (final PreparedStatement stat = connection.prepareStatement(plugin.load("sql/player-best-score.sql"))) {
			stat.setString(1, player.getPlayer().getUniqueId().toString());
			stat.setString(2, game.getName());
			final ResultSet rs = stat.executeQuery();
			if (rs.next()) {
				consumer.accept(new Result(
						rs.getDouble("player_time"),
						rs.getDouble("parkour_time"),
						rs.getTimestamp("last_run").toLocalDateTime(),
						rs.getInt("count")));
			}
		} catch (final SQLException | IOException ex) {
			ex.printStackTrace();
		}
	}
}
