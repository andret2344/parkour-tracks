/*
 * Copyright Andret (c) 2019-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.player;

import lombok.Data;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@Data
public abstract class ParkourPlayer {
	@NotNull
	protected final Player player;
	protected int lastCheckpoint = -1;
	protected boolean ignoring = false;
	protected double time = 0;
	protected boolean hidden = false;

	ParkourPlayer(@NotNull final Player player) {
		this.player = player;
	}

	public void reset() {
		lastCheckpoint = -1;
		time = 0;
		player.setExp(0);
		player.setLevel(0);
	}
}
