/*
 * Copyright Andret (c) 2019-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.tasks.counter;

import eu.andret.ats.parkour.ParkourPlugin;
import eu.andret.ats.parkour.parkour.ParkourGame;
import eu.andret.ats.parkour.parkour.ParkourManager;
import eu.andret.ats.parkour.player.ParkourPlayer;
import org.bukkit.Sound;

public class PlayerTimeCounter implements Runnable {
	private final ParkourPlayer parkourPlayer;
	private final ParkourPlugin parkourPlugin;
	private int counter;

	public PlayerTimeCounter(final ParkourPlayer parkourPlayer, final ParkourPlugin parkourPlugin) {
		this.parkourPlayer = parkourPlayer;
		this.parkourPlugin = parkourPlugin;
	}

	@Override
	public void run() {
		final ParkourGame parkourGame = ParkourManager.getParkour(parkourPlayer.getPlayer());
		if (parkourPlayer.isIgnoring() || parkourGame == null || !parkourGame.isRunning() || parkourPlayer.isSpectating()) {
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
		double time = 0;
		if (!parkourPlugin.getTeleportCount().containsKey(parkourPlayer.getPlayer().getUniqueId())) {
			time = counter++ / 20.;
		}
		if (counter == 1) {
			parkourPlayer.getPlayer().playSound(parkourPlayer.getPlayer().getLocation(), Sound.BLOCK_LEVER_CLICK, 0.5F, 0.5F);
		}
		parkourPlayer.getPlayer().setLevel((int) time);
		parkourPlayer.getPlayer().setExp((float) time % 1);
	}
}
