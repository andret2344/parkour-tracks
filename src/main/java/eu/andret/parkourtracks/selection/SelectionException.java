package eu.andret.parkourtracks.selection;

import org.jetbrains.annotations.NotNull;

/**
 * Thrown when a player's selection cannot be used.
 */
public final class SelectionException extends RuntimeException {
	public enum Reason {
		/**
		 * Nothing, or only one corner, is selected.
		 */
		INCOMPLETE,
		/**
		 * The selection is not a cuboid; only cuboid regions are supported.
		 */
		NOT_CUBOID
	}

	@NotNull
	private final Reason reason;

	public SelectionException(@NotNull final Reason reason) {
		super(reason.name());
		this.reason = reason;
	}

	@NotNull
	public Reason getReason() {
		return reason;
	}
}
