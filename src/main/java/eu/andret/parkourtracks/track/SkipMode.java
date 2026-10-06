package eu.andret.parkourtracks.track;

/**
 * What happens when a player enters a checkpoint without having passed the one before it.
 */
public enum SkipMode {
	/**
	 * The skip counts as progress.
	 */
	ALLOW,
	/**
	 * The skip counts as progress, and the player is told which checkpoint they missed.
	 */
	NOTIFY,
	/**
	 * The player goes back to their last checkpoint.
	 */
	FAIL
}
