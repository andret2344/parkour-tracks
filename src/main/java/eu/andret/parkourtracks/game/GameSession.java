package eu.andret.parkourtracks.game;

import eu.andret.parkourtracks.track.Track;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * A player in a game of a track, and how far they got.
 */
public final class GameSession {
	/**
	 * The value of {@link #getLastCheckpoint()} before the first checkpoint: the spawn.
	 */
	public static final int SPAWN = -1;

	@NotNull
	private final UUID player;
	@NotNull
	private final Track track;
	@NotNull
	private Phase phase = Phase.WAITING;
	private int lastCheckpoint = SPAWN;
	private int ticks;
	private boolean paused;
	private int finishTask = -1;

	public GameSession(@NotNull final UUID player, @NotNull final Track track) {
		this.player = player;
		this.track = track;
	}

	@NotNull
	public UUID getPlayer() {
		return player;
	}

	@NotNull
	public Track getTrack() {
		return track;
	}

	@NotNull
	public Phase getPhase() {
		return phase;
	}

	/**
	 * The index of the last checkpoint passed, {@link #SPAWN} before the first one.
	 */
	public int getLastCheckpoint() {
		return lastCheckpoint;
	}

	/**
	 * The ticks the run has taken so far, not counting paused ones.
	 */
	public int getTicks() {
		return ticks;
	}

	public boolean isPaused() {
		return paused;
	}

	/**
	 * Back on the spawn: the run, if any, is gone.
	 */
	void resetToSpawn() {
		phase = Phase.WAITING;
		lastCheckpoint = SPAWN;
		ticks = 0;
		paused = false;
	}

	void start() {
		phase = Phase.RUNNING;
		lastCheckpoint = SPAWN;
		ticks = 0;
	}

	void reach(final int checkpoint) {
		lastCheckpoint = checkpoint;
	}

	void finish() {
		phase = Phase.FINISHED;
		paused = false;
	}

	void tick() {
		ticks++;
	}

	void setPaused(final boolean paused) {
		this.paused = paused;
	}

	int getFinishTask() {
		return finishTask;
	}

	void setFinishTask(final int finishTask) {
		this.finishTask = finishTask;
	}
}
