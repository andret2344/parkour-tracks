package eu.andret.parkourtracks.selection;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Where region selections come from. WorldEdit on a server; tests put their own selections in.
 */
public interface Selections {
	/**
	 * The player's current selection.
	 *
	 * @throws SelectionException when the player has no complete selection, or one that is not a cuboid
	 */
	@NotNull
	Selection get(@NotNull Player player);
}
