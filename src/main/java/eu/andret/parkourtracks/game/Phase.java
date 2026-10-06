package eu.andret.parkourtracks.game;

/**
 * Where a player in a game is with their run.
 */
public enum Phase {
	/**
	 * On the spawn, the run not started yet.
	 */
	WAITING,
	/**
	 * Left the spawn; the timer runs.
	 */
	RUNNING,
	/**
	 * Reached the finish; waiting to be sent to the lobby.
	 */
	FINISHED
}
