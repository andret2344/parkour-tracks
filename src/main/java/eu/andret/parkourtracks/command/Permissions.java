package eu.andret.parkourtracks.command;

/**
 * The permission nodes, as declared in {@code plugin.yml}.
 */
public final class Permissions {
	/**
	 * Player commands; everyone by default.
	 */
	public static final String PLAY = "parkourtracks.play";
	/**
	 * Creating and editing tracks.
	 */
	public static final String EDIT = "parkourtracks.edit";
	/**
	 * Starting, stopping and removing tracks, the lobby, reloading.
	 */
	public static final String MANAGE = "parkourtracks.manage";

	private Permissions() {
	}
}
