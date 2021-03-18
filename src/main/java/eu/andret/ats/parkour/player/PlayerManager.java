/*
 * Copyright Andret (c) 2019. Copying and modifying allowed only keeping git link reference.
 */
package eu.andret.ats.parkour.player;

import lombok.experimental.UtilityClass;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

@UtilityClass
public final class PlayerManager {
	private final Map<Player, ParkourSinglePlayer> singlePlayers = new HashMap<>();
	private final Map<Player, ParkourCompetitorPlayer> competitorPlayers = new HashMap<>();

	public ParkourSinglePlayer getParkourSinglePlayer(final Player player) {
		if (singlePlayers.containsKey(player)) {
			return singlePlayers.get(player);
		}
		final ParkourSinglePlayer p = new ParkourSinglePlayer(player);
		singlePlayers.put(player, p);
		return p;
	}

	public ParkourCompetitorPlayer getParkourCompetitorPlayer(final Player player) {
		if (competitorPlayers.containsKey(player)) {
			return competitorPlayers.get(player);
		}
		final ParkourCompetitorPlayer p = new ParkourCompetitorPlayer(player);
		competitorPlayers.put(player, p);
		return p;
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
}
