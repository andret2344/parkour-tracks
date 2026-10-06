package eu.andret.parkourtracks.config;

/**
 * Where the running time of a run is shown.
 */
public enum TimerDisplay {
	/**
	 * The text above the hotbar.
	 */
	ACTION_BAR,
	/**
	 * The experience bar: the level is the seconds, the bar their fraction.
	 */
	XP_BAR,
	BOTH;

	public boolean showsActionBar() {
		return this != XP_BAR;
	}

	public boolean showsXpBar() {
		return this != ACTION_BAR;
	}
}
