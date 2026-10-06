package eu.andret.parkourtracks.game;

/**
 * How a player got into a game.
 */
public enum Entry {
	/**
	 * Walked or was teleported into the spawn.
	 */
	SPAWN,
	/**
	 * Came into the region anywhere but the spawn, and was sent to the spawn.
	 */
	SIDE,
	/**
	 * Was in the game when they disconnected, or when the plugin stopped, and came back into its region.
	 */
	RETURN,
	/**
	 * Was sent to the spawn by the plugin: a command, a teleport block, the menu.
	 */
	DIRECT
}
