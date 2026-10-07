package eu.andret.parkourtracks.track;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * A parkour track: its region in one world, the spawn, the checkpoints in between, the finish, the walls and its
 * settings. Saved to the tracks file as it is; the field initializers fill in fields missing from it.
 */
public final class Track {
	/**
	 * What a track name can be made of; names are unique ignoring case.
	 */
	public static final Pattern NAME_PATTERN = Pattern.compile("[A-Za-z0-9_-]{1,32}");

	@NotNull
	private final UUID id;
	@NotNull
	private String name;
	@NotNull
	private String displayName;
	@NotNull
	private TrackType type = TrackType.SERVER;
	@NotNull
	private String world;
	@NotNull
	private Cuboid region;
	@Nullable
	private Checkpoint spawn;
	@Nullable
	private Checkpoint finish;
	@NotNull
	private final List<Checkpoint> checkpoints = new ArrayList<>();
	@NotNull
	private final List<Cuboid> walls = new ArrayList<>();
	@NotNull
	private final List<UUID> authors = new ArrayList<>();
	@NotNull
	private final Map<String, MedalThreshold> medals = new LinkedHashMap<>();
	@NotNull
	private final List<TrackEffect> effects = new ArrayList<>();
	@Nullable
	private WorldSpot lobby;
	private boolean running;
	@NotNull
	private final TrackOptions options = new TrackOptions();

	/**
	 * For Gson only: it runs the field initializers, then fills in the fields from the tracks file.
	 */
	@SuppressWarnings({"unused", "DataFlowIssue", "java:S2637"})
	private Track() {
		id = null;
		name = null;
		displayName = null;
		world = null;
		region = null;
	}

	public Track(@NotNull final UUID id, @NotNull final String name, @NotNull final String world,
			@NotNull final Cuboid region) {
		if (!NAME_PATTERN.matcher(name).matches()) {
			throw new IllegalArgumentException("Invalid track name: " + name);
		}
		this.id = id;
		this.name = name;
		this.displayName = name;
		this.world = world;
		this.region = region;
	}

	@NotNull
	public UUID getId() {
		return id;
	}

	@NotNull
	public String getName() {
		return name;
	}

	public void setName(@NotNull final String name) {
		if (!NAME_PATTERN.matcher(name).matches()) {
			throw new IllegalArgumentException("Invalid track name: " + name);
		}
		this.name = name;
	}

	/**
	 * The name shown to players, in MiniMessage. Never used in commands.
	 */
	@NotNull
	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(@NotNull final String displayName) {
		this.displayName = displayName;
	}

	@NotNull
	public TrackType getType() {
		return type;
	}

	public void setType(@NotNull final TrackType type) {
		this.type = type;
	}

	/**
	 * The name of the world every region of the track lies in.
	 */
	@NotNull
	public String getWorld() {
		return world;
	}

	@NotNull
	public Cuboid getRegion() {
		return region;
	}

	public void setRegion(@NotNull final String world, @NotNull final Cuboid region) {
		this.world = world;
		this.region = region;
	}

	@Nullable
	public Checkpoint getSpawn() {
		return spawn;
	}

	public void setSpawn(@Nullable final Checkpoint spawn) {
		this.spawn = spawn;
	}

	@Nullable
	public Checkpoint getFinish() {
		return finish;
	}

	public void setFinish(@Nullable final Checkpoint finish) {
		this.finish = finish;
	}

	/**
	 * The checkpoints between the spawn and the finish, in the order they have to be passed.
	 */
	@NotNull
	public List<Checkpoint> getCheckpoints() {
		return Collections.unmodifiableList(checkpoints);
	}

	public void addCheckpoint(final int position, @NotNull final Checkpoint checkpoint) {
		checkpoints.add(position, checkpoint);
	}

	public void setCheckpoint(final int position, @NotNull final Checkpoint checkpoint) {
		checkpoints.set(position, checkpoint);
	}

	public void removeCheckpoint(final int position) {
		checkpoints.remove(position);
	}

	@NotNull
	public List<Cuboid> getWalls() {
		return Collections.unmodifiableList(walls);
	}

	public void addWall(@NotNull final Cuboid wall) {
		walls.add(wall);
	}

	public void setWall(final int position, @NotNull final Cuboid wall) {
		walls.set(position, wall);
	}

	public void removeWall(final int position) {
		walls.remove(position);
	}

	@NotNull
	public List<UUID> getAuthors() {
		return Collections.unmodifiableList(authors);
	}

	public void setAuthors(@NotNull final List<UUID> authors) {
		this.authors.clear();
		this.authors.addAll(authors);
	}

	/**
	 * The track's thresholds by medal key. A medal without an entry is not awarded on this track.
	 */
	@NotNull
	public Map<String, MedalThreshold> getMedals() {
		return Collections.unmodifiableMap(medals);
	}

	public void setMedal(@NotNull final String medal, @NotNull final MedalThreshold threshold) {
		medals.put(medal, threshold);
	}

	public void removeMedal(@NotNull final String medal) {
		medals.remove(medal);
	}

	@NotNull
	public List<TrackEffect> getEffects() {
		return Collections.unmodifiableList(effects);
	}

	/**
	 * Sets the effect of the given type, replacing the one of that type the track had.
	 */
	public void setEffect(@NotNull final TrackEffect effect) {
		removeEffect(effect.type());
		effects.add(effect);
	}

	public void removeEffect(@NotNull final String type) {
		effects.removeIf(effect -> effect.type().equals(type));
	}

	/**
	 * The track's own lobby, used instead of the global one when set.
	 */
	@Nullable
	public WorldSpot getLobby() {
		return lobby;
	}

	public void setLobby(@Nullable final WorldSpot lobby) {
		this.lobby = lobby;
	}

	public boolean isRunning() {
		return running;
	}

	public void setRunning(final boolean running) {
		this.running = running;
	}

	@NotNull
	public TrackOptions getOptions() {
		return options;
	}

	/**
	 * Checks what Gson cannot: that the fields read from the tracks file make a valid track.
	 *
	 * @throws IllegalStateException when a required field is missing or the name is invalid
	 */
	@SuppressWarnings("ConstantValue")
	void validateLoaded() {
		if (id == null || name == null || world == null || region == null || options == null) {
			throw new IllegalStateException("A track misses its id, name, world, region or options");
		}
		if (!NAME_PATTERN.matcher(name).matches()) {
			throw new IllegalStateException("Invalid track name: " + name);
		}
		if (displayName == null) {
			displayName = name;
		}
	}

	@Override
	public boolean equals(@Nullable final Object other) {
		return other instanceof final Track track && id.equals(track.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	@NotNull
	public String toString() {
		return "Track[" + name + ", " + id + "]";
	}
}
