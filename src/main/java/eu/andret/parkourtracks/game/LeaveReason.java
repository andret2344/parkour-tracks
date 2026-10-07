package eu.andret.parkourtracks.game;

/**
 * Why a player left a game.
 */
public enum LeaveReason {
	/**
	 * The exit item or {@code /ptracks leave}.
	 */
	EXIT(true),
	/**
	 * Walked out of the track's region.
	 */
	LEFT_REGION(false),
	/**
	 * Was teleported out of the track's region by something else than the plugin.
	 */
	TELEPORT(false),
	/**
	 * Disconnected, or the plugin stopped; the player comes back to the spawn without paying.
	 */
	DISCONNECT(false),
	/**
	 * An admin stopped the track.
	 */
	STOPPED(true),
	/**
	 * Reached the finish and waited for the lobby.
	 */
	FINISHED(true),
	/**
	 * Switched to spectator, or to ignoring tracks.
	 */
	NOT_A_PLAYER(false);

	private final boolean toLobby;

	LeaveReason(final boolean toLobby) {
		this.toLobby = toLobby;
	}

	/**
	 * Whether the player is sent to the lobby; otherwise they stay where they are.
	 */
	public boolean isToLobby() {
		return toLobby;
	}
}
