/*
 * Copyright Andret (c) 2019-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour;

import lombok.experimental.UtilityClass;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

@UtilityClass
public class TutorialManager {
	private final Map<Player, TutorialPlayer> players = new HashMap<>();

	public TutorialPlayer getPlayer(final Player player) {
		if (players.containsKey(player)) {
			return players.get(player);
		}
		final TutorialPlayer tutorialPlayer = new TutorialPlayer(player);
		players.put(player, tutorialPlayer);
		return tutorialPlayer;
	}

	public void removePlayer(final Player player) {
		players.remove(player);
	}

	public boolean hasPlayer(final Player player) {
		return players.containsKey(player);
	}
}
