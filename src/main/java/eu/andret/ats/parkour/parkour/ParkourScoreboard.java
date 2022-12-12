/*
 * Copyright Andret (c) 2019-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.parkour;

import lombok.Builder;
import lombok.With;
import org.bukkit.Bukkit;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import java.util.List;

@With
@Builder
public class ParkourScoreboard {
	private final String displayName;
	private final List<String> pattern;

	private final String parkour;
	private final double bestTime;
	private final double playerTime;
	private final Medal medal;
	private final int count;
	private final ParkourGame.Type parkourType;
	private final double time;
	private final String player;

	public Scoreboard build() {
		final ScoreboardManager scoreboardManager = Bukkit.getScoreboardManager();
		if (scoreboardManager == null) {
			throw new IllegalArgumentException("Something went wrong with scoreboard manager");
		}
		final Scoreboard board = scoreboardManager.getNewScoreboard();
		final Objective objective = board.registerNewObjective("parkour", Criteria.DUMMY, "parkour");
		objective.setDisplaySlot(DisplaySlot.SIDEBAR);
		objective.setDisplayName(replacePattern(displayName));
		for (int i = 0; i < pattern.size(); i++) {
			final Score score = objective.getScore(replacePattern(pattern.get(i)));
			score.setScore(i + 1);
		}
		return board;
	}

	private String replacePattern(final String text) {
		return text.replace("%PARKOUR%", parkour)
				.replace("%BEST_TIME%", String.valueOf(bestTime))
				.replace("%PLAYER_TIME%", String.valueOf(playerTime))
				.replace("%MEDAL%", medal.getDisplayName())
				.replace("%COUNT%", String.valueOf(count))
				.replace("%TYPE%", parkourType.name())
				.replace("%PLAYER%", player);
	}
}
