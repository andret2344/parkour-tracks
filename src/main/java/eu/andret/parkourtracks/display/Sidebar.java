package eu.andret.parkourtracks.display;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * The sidebar a player sees during a game. {@link PaperSidebar} on a server; tests put their own in, as MockBukkit
 * does not implement the scoreboard parts it needs.
 */
public interface Sidebar {
	/**
	 * Shows the title and the lines, top to bottom, replacing what this sidebar showed the player before. The first
	 * time, the scoreboard the player had is kept to be given back by {@link #hide}.
	 */
	void show(@NotNull Player player, @NotNull Component title, @NotNull List<Component> lines);

	/**
	 * Gives the player back the scoreboard they had before {@link #show}, unless something else replaced ours since.
	 */
	void hide(@NotNull Player player);
}
