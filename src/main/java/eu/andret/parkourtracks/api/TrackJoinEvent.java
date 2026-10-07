package eu.andret.parkourtracks.api;

import eu.andret.parkourtracks.game.Entry;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * A player is about to join a game on a track, after their permission for it was checked and before any fee is
 * taken. Canceling it keeps them out: they are sent to the lobby, and the canceling plugin tells them why.
 */
public final class TrackJoinEvent extends PlayerEvent implements Cancellable {
	@NotNull
	private static final HandlerList HANDLERS = new HandlerList();

	@NotNull
	private final UUID trackId;
	@NotNull
	private final String trackName;
	@NotNull
	private final Entry entry;
	private boolean canceled;

	public TrackJoinEvent(@NotNull final Player player, @NotNull final UUID trackId, @NotNull final String trackName,
			@NotNull final Entry entry) {
		super(player);
		this.trackId = trackId;
		this.trackName = trackName;
		this.entry = entry;
	}

	/**
	 * The track's id, which never changes; its name can.
	 */
	@NotNull
	public UUID getTrackId() {
		return trackId;
	}

	@NotNull
	public String getTrackName() {
		return trackName;
	}

	/**
	 * How the player is getting in.
	 */
	@NotNull
	public Entry getEntry() {
		return entry;
	}

	@Override
	public boolean isCancelled() {
		return canceled;
	}

	@Override
	public void setCancelled(final boolean canceled) {
		this.canceled = canceled;
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
