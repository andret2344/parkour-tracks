package eu.andret.parkourtracks.track;

/**
 * Where a player goes after reaching the finish.
 */
public enum AfterFinish {
	/**
	 * The lobby, after the delay set in the config.
	 */
	LOBBY,
	/**
	 * Straight back to the spawn, for another try.
	 */
	SPAWN
}
