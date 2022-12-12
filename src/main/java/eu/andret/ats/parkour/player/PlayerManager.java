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
		final ParkourSinglePlayer parkourSinglePlayer = new ParkourSinglePlayer(player);
		singlePlayers.put(player, parkourSinglePlayer);
		return parkourSinglePlayer;
	}

	public ParkourCompetitorPlayer getParkourCompetitorPlayer(final Player player) {
		if (competitorPlayers.containsKey(player)) {
			return competitorPlayers.get(player);
		}
		final ParkourCompetitorPlayer parkourCompetitorPlayer = new ParkourCompetitorPlayer(player);
		competitorPlayers.put(player, parkourCompetitorPlayer);
		return parkourCompetitorPlayer;
	}

	public ParkourPlayer getParkourPlayer(final Player player) {
		if (singlePlayers.containsKey(player)) {
			return singlePlayers.get(player);
		}
		if (competitorPlayers.containsKey(player)) {
			return competitorPlayers.get(player);
		}
		return null;
	}

	public ParkourPlayer remove(final Player player) {
		final ParkourPlayer parkourPlayer = singlePlayers.remove(player);
		if (parkourPlayer != null) {
			return parkourPlayer;
		}
		return competitorPlayers.remove(player);
	}

	public void teleportToCheckpoint(@NotNull final ParkourPlayer parkourPlayer, @Nullable final Checkpoint checkpoint) {
		Optional.ofNullable(checkpoint)
				.map(Checkpoint::getLocation)
				.ifPresent(parkourPlayer.getPlayer()::teleport);
	}
}
