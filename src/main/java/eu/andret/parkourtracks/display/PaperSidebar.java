package eu.andret.parkourtracks.display;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A sidebar on a scoreboard of its own per player. Lines are scores with a custom name and no number shown.
 */
public final class PaperSidebar implements Sidebar {
	private static final String OBJECTIVE = "parkourtracks";

	@NotNull
	private final Map<UUID, Scoreboard> previous = new HashMap<>();
	@NotNull
	private final Map<UUID, Scoreboard> shown = new HashMap<>();

	@Override
	public void show(@NotNull final Player player, @NotNull final Component title, @NotNull final List<Component> lines) {
		final Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
		final Objective objective = board.registerNewObjective(OBJECTIVE, Criteria.DUMMY, title);
		objective.setDisplaySlot(DisplaySlot.SIDEBAR);
		objective.numberFormat(NumberFormat.blank());
		for (int i = 0; i < lines.size(); i++) {
			// The entries only have to be unique; what shows is the custom name
			final Score score = objective.getScore("line" + i);
			score.customName(lines.get(i));
			score.setScore(lines.size() - i);
		}
		previous.putIfAbsent(player.getUniqueId(), player.getScoreboard());
		shown.put(player.getUniqueId(), board);
		player.setScoreboard(board);
	}

	@Override
	public void hide(@NotNull final Player player) {
		final Scoreboard board = shown.remove(player.getUniqueId());
		final Scoreboard before = previous.remove(player.getUniqueId());
		if (board != null && before != null && player.getScoreboard() == board) {
			player.setScoreboard(before);
		}
	}
}
