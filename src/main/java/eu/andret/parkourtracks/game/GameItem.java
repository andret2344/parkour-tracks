package eu.andret.parkourtracks.game;

import eu.andret.parkourtracks.message.Message;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * An item in the hotbar during a game, by its key in {@code config.yml} under {@code game-items}.
 */
public enum GameItem {
	/**
	 * Back to the last checkpoint.
	 */
	BACK(Message.ITEM_BACK),
	/**
	 * Back to the spawn; the run starts over.
	 */
	RESTART(Message.ITEM_RESTART),
	/**
	 * Opens the track selection menu.
	 */
	MENU(Message.ITEM_MENU),
	/**
	 * Hides the other players of the track, or shows them again.
	 */
	HIDE(Message.ITEM_HIDE),
	/**
	 * Leaves the game, to the lobby.
	 */
	EXIT(Message.ITEM_EXIT);

	@NotNull
	private final Message name;

	GameItem(@NotNull final Message name) {
		this.name = name;
	}

	/**
	 * The message holding the item's name.
	 */
	@NotNull
	public Message getName() {
		return name;
	}

	@NotNull
	public String getKey() {
		return name().toLowerCase(Locale.ROOT);
	}
}
