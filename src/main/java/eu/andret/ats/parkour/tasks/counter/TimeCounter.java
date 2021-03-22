/*
 * Copyright Andret (c) 2019-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.tasks.counter;

import eu.andret.ats.parkour.parkour.ParkourGame;
import eu.andret.ats.parkour.player.ParkourPlayer;
import lombok.NonNull;
import org.bukkit.Sound;
import org.jetbrains.annotations.NotNull;

public class TimeCounter implements Runnable {
	@NonNull
	@NotNull
	private final ParkourPlayer parkourPlayer;
	@NonNull
	@NotNull
	private final ParkourGame parkourGame;
	private int counter = 0;

	public TimeCounter(@NonNull @NotNull final ParkourPlayer parkourPlayer, @NonNull @NotNull final ParkourGame parkourGame) {
		this.parkourPlayer = parkourPlayer;
		this.parkourGame = parkourGame;
	}

	@Override
	public void run() {
		if (parkourPlayer.isIgnoring() || !parkourGame.isRunning()) {
			return;
		}
		if (parkourGame.getCheckpoints().size() == parkourPlayer.getLastCheckpoint() - 1) {
			return;
		}
		final boolean playerInSpawn = parkourGame.getSpawn() != null && parkourGame.getSpawn().contains(parkourPlayer.getPlayer().getLocation());
		final boolean playerInAnyCheckpoint = parkourGame.getCheckpoints().stream().anyMatch(c -> c.contains(parkourPlayer.getPlayer().getLocation()));
		if (playerInSpawn) {
			counter = 0;
		}
		if (playerInAnyCheckpoint || playerInSpawn) {
			return;
		}
		if (counter == 1) {
			parkourPlayer.getPlayer().playSound(parkourPlayer.getPlayer().getLocation(), Sound.BLOCK_LEVER_CLICK, 0.5F, 0.5F);
		}
		final double time = counter++ / 20.;
		parkourPlayer.setTime(time);
		parkourPlayer.getPlayer().setLevel((int) time);
		parkourPlayer.getPlayer().setExp((float) time % 1);
	}
}
