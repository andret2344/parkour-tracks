package eu.andret.parkourtracks.api;

import org.bukkit.OfflinePlayer;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * A player completed a track and the run was saved. Fired once the results database answered, so the player may
 * have left meanwhile. Training tracks keep no results and fire none.
 */
public final class TrackCompleteEvent extends Event {
	@NotNull
	private static final HandlerList HANDLERS = new HandlerList();

	@NotNull
	private final OfflinePlayer player;
	@NotNull
	private final UUID trackId;
	@NotNull
	private final String trackName;
	private final int ticks;
	@Nullable
	private final String medal;
	private final boolean personalBest;
	private final boolean trackRecord;

	public TrackCompleteEvent(@NotNull final OfflinePlayer player, @NotNull final UUID trackId,
			@NotNull final String trackName, final int ticks, @Nullable final String medal,
			final boolean personalBest, final boolean trackRecord) {
		this.player = player;
		this.trackId = trackId;
		this.trackName = trackName;
		this.ticks = ticks;
		this.medal = medal;
		this.personalBest = personalBest;
		this.trackRecord = trackRecord;
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

	/**
	 * The time of the run in game ticks, 20 per second.
	 */
	public int getTicks() {
		return ticks;
	}

	/**
	 * The key of the best medal the run earned, as in the plugin's {@code config.yml}, or {@code null} for none.
	 */
	@Nullable
	public String getMedal() {
		return medal;
	}

	/**
	 * Whether the run beat the player's previous best on the track; their first completion does.
	 */
	public boolean isPersonalBest() {
		return personalBest;
	}

	/**
	 * Whether the run beat everyone's previous best on the track; the first completion of the track does.
	 */
	public boolean isTrackRecord() {
		return trackRecord;
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
