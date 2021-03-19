/*
 * Copyright Andret (c) 2019. Copying and modifying allowed only keeping git link reference.
 */
package eu.andret.ats.parkour.player;

import eu.andret.ats.parkour.parkour.ParkourGame;
import eu.andret.ats.parkour.parkour.ParkourManager;
import eu.andret.ats.parkour.region.Checkpoint;
import eu.andret.ats.parkour.util.SchedulerManager;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = true)
public class ParkourSinglePlayer extends ParkourPlayer implements Runnable {
	private float time = 0;
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private int i = 0;

	ParkourSinglePlayer(final Player player) {
		super(player);
	}

	@Override
	public void run() {
		//FIXME: Remove thread thing from here
		final ParkourGame p = ParkourManager.getParkour(player);
		if (ignoring || PlayerManager.getParkourPlayer(player) == null || p == null || !p.isRunning() || spectating) {
			return;
		}
		for (final Checkpoint c : p.getCheckpointList()) {
			if (c.contains(player.getLocation())) {
				return;
			}
		}
		if (p.getSpawn().contains(player.getLocation())) {
			i = 0;
		}
		if (!SchedulerManager.TELEPORT_COUNT.containsKey(player.getUniqueId())) {
			time = (i++) / 20F;
		}
		if (i == 1) {
			player.playSound(player.getLocation(), Sound.BLOCK_LEVER_CLICK, 0.5F, 0.5F);
		}
		player.setLevel((int) time);
		player.setExp(time % 1);
	}

	@Override
	public void reset() {
		super.reset();
		time = 0;
		i = 0;
	}
}
