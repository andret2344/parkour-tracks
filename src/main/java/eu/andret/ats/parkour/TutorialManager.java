/*
 * Copyright Andret (c) 2019-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour;

import lombok.Value;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

@Value
public class TutorialManager {
	@NotNull
	Map<Player, TutorialPlayer> players = new HashMap<>();
	@NotNull
	ParkourPlugin plugin;

	@NotNull
	public TutorialPlayer getPlayer(@NotNull final Player player) {
		if (players.containsKey(player)) {
			return players.get(player);
		}
		final TutorialPlayer tutorialPlayer = new TutorialPlayer(player, this);
		players.put(player, tutorialPlayer);
		return tutorialPlayer;
	}

	public void removePlayer(@NotNull final Player player) {
		players.remove(player);
	}

	public boolean hasPlayer(@NotNull final Player player) {
		return players.containsKey(player);
	}
}
