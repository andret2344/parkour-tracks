/*
 * Copyright Andret (c) 2018-2022. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.player;

import eu.andret.ats.parkour.region.Checkpoint;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class PlayerManager {
	@NotNull
	private final Map<Player, ParkourSinglePlayer> players = new HashMap<>();

	@NotNull
	public ParkourPlayer getParkourPlayer(@NotNull final Player player) {
		if (players.containsKey(player)) {
			return players.get(player);
		}
		final ParkourSinglePlayer parkourPlayer = new ParkourSinglePlayer(player);
		players.put(player, parkourPlayer);
		return parkourPlayer;
	}

	public ParkourPlayer remove(final Player player) {
		return players.remove(player);
	}

	public void teleportToCheckpoint(@NotNull final ParkourPlayer parkourPlayer, @Nullable final Checkpoint checkpoint) {
		Optional.ofNullable(checkpoint)
				.map(Checkpoint::getLocation)
				.ifPresent(parkourPlayer.getPlayer()::teleport);
	}
}
