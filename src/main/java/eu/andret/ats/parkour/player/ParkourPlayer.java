/*
 * Copyright Andret (c) 2019-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.player;

import eu.andret.ats.parkour.parkour.ParkourScoreboard;
import lombok.Data;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@Data
public abstract class ParkourPlayer {
	protected final Player player;
	protected ParkourScoreboard parkourScoreboard;
	protected int lastCheckpoint = -1;
	protected boolean ignoring = false;
	protected double time = 0;

	ParkourPlayer(final Player player) {
		this.player = player;
	}

	public void reset() {
		lastCheckpoint = -1;
		time = 0;
		player.setExp(0);
		player.setLevel(0);
	}

	public ParkourScoreboard getParkourScoreboard() {
		return parkourScoreboard;
	}

	public void setParkourScoreboard(@Nullable final ParkourScoreboard parkourScoreboard) {
		this.parkourScoreboard = parkourScoreboard;
		final Scoreboard scoreboard = Optional.ofNullable(parkourScoreboard)
				.map(ParkourScoreboard::build)
				.orElse(Bukkit.getScoreboardManager().getNewScoreboard());
		player.setScoreboard(scoreboard);
	}
}
