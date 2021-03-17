/*
 * Copyright Andret (c) 2019. Copying and modifying allowed only keeping git link reference.
 */
package eu.andret.ats.parkour.tasks;

import eu.andret.ats.parkour.ParkourPlugin;
import eu.andret.ats.parkour.player.ParkourPlayer;

public class TeleportCount implements Runnable {
	private final ParkourPlugin plugin;
	private final ParkourPlayer player;
	private int i = 5;

	public TeleportCount(ParkourPlugin plugin, ParkourPlayer player) {
		this.plugin = plugin;
		this.player = player;
	}

	@Override
	public void run() {
		if (player == null) {
			return;
		}
		if (i == 5) {
			player.getPlayer().sendMessage(plugin.msg("10sek", false));
		}
		if (i == 0) {
			plugin.getListeners().stopScheduling(player.getPlayer());
		}
		i--;
	}

	public int getLeastTime() {
		return i;
	}
}
