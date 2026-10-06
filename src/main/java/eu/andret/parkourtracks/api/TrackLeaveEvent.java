package eu.andret.parkourtracks.api;

import eu.andret.parkourtracks.game.LeaveReason;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * A player left a game on a track, and got their own state back.
 */
public final class TrackLeaveEvent extends PlayerEvent {
	@NotNull
	private static final HandlerList HANDLERS = new HandlerList();

	@NotNull
	private final UUID trackId;
	@NotNull
	private final String trackName;
	@NotNull
	private final LeaveReason reason;

	public TrackLeaveEvent(@NotNull final Player player, @NotNull final UUID trackId, @NotNull final String trackName,
						   @NotNull final LeaveReason reason) {
		super(player);
		this.trackId = trackId;
		this.trackName = trackName;
		this.reason = reason;
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
	public LeaveReason getReason() {
		return reason;
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
