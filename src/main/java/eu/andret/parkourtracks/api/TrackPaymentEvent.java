package eu.andret.parkourtracks.api;

import org.bukkit.OfflinePlayer;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Money moved between a player and the economy because of a track, after the economy took or gave it.
 */
public final class TrackPaymentEvent extends Event {
	@NotNull
	private static final HandlerList HANDLERS = new HandlerList();

	/**
	 * Why the money moved.
	 */
	public enum Kind {
		/**
		 * The player paid the fee to enter.
		 */
		FEE,
		/**
		 * The player got their fee back when an admin stopped the track.
		 */
		REFUND,
		/**
		 * The player got the reward for completing the track.
		 */
		REWARD,
		/**
		 * The player got a medal's reward for a run.
		 */
		MEDAL,
		/**
		 * The player got a medal's reward owed from before, paid by an admin's reconciliation.
		 */
		RECONCILIATION
	}

	@NotNull
	private final OfflinePlayer player;
	@NotNull
	private final UUID trackId;
	@NotNull
	private final String trackName;
	@NotNull
	private final Kind kind;
	@Nullable
	private final String medal;
	private final double amount;

	public TrackPaymentEvent(@NotNull final OfflinePlayer player, @NotNull final UUID trackId,
							 @NotNull final String trackName, @NotNull final Kind kind, @Nullable final String medal,
							 final double amount) {
		this.player = player;
		this.trackId = trackId;
		this.trackName = trackName;
		this.kind = kind;
		this.medal = medal;
		this.amount = amount;
	}

	@NotNull
	public OfflinePlayer getPlayer() {
		return player;
	}

	@NotNull
	public UUID getTrackId() {
		return trackId;
	}

	@NotNull
	public String getTrackName() {
		return trackName;
	}

	@NotNull
	public Kind getKind() {
		return kind;
	}

	/**
	 * The key of the medal paid, for {@link Kind#MEDAL} and {@link Kind#RECONCILIATION}; {@code null} otherwise.
	 */
	@Nullable
	public String getMedal() {
		return medal;
	}

	/**
	 * The amount moved, always positive; a {@link Kind#FEE} was taken from the player, every other kind given to them.
	 */
	public double getAmount() {
		return amount;
	}

	@NotNull
	@Override
	public HandlerList getHandlers() {
		return HANDLERS;
	}

	@NotNull
	public static HandlerList getHandlerList() {
		return HANDLERS;
	}
}
