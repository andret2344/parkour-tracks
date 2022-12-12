/*
 * Copyright Andret (c) 2018-2022. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.player;

import eu.andret.ats.parkour.parkour.ParkourScoreboard;
import lombok.Data;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@Data
public abstract class ParkourPlayer {
	@NotNull
	protected final Player player;
	protected int lastCheckpoint = 0;
	protected boolean ignoring = false;
	protected double time = 0;
	protected boolean hidden = false;

	ParkourPlayer(@NotNull final Player player) {
		this.player = player;
	}

	public void reset() {
		lastCheckpoint = 0;
		time = 0;
		player.setExp(0);
		player.setLevel(0);
	}

	public void setParkourScoreboard(@Nullable final ParkourScoreboard parkourScoreboard) {
		final Scoreboard scoreboard = Optional.ofNullable(parkourScoreboard)
				.map(ParkourScoreboard::build)
				.orElse(Bukkit.getScoreboardManager().getNewScoreboard());
		player.setScoreboard(scoreboard);
	}
}
