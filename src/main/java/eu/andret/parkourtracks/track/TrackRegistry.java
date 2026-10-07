package eu.andret.parkourtracks.track;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * All tracks and the global lobby. Every change is saved right away by the caller through {@link #save()}.
 */
public final class TrackRegistry {
	@NotNull
	private final TrackStore store;
	@NotNull
	private final List<Track> tracks = new ArrayList<>();
	@Nullable
	private WorldSpot lobby;

	public TrackRegistry(@NotNull final TrackStore store) {
		this.store = store;
	}

	/**
	 * Replaces everything held with the content of the tracks file.
	 *
	 * @throws IllegalStateException when the file is invalid or two tracks share an id or a name
	 */
	public void load() {
		final TrackStore.Content content = store.load();
		final Set<UUID> ids = new HashSet<>();
		final Set<String> names = new HashSet<>();
		for (final Track track : content.tracks()) {
			if (!ids.add(track.getId()) || !names.add(normalize(track.getName()))) {
				throw new IllegalStateException("Two tracks share the id or the name of " + track);
			}
		}
		tracks.clear();
		tracks.addAll(content.tracks());
		lobby = content.lobby();
	}

	public void save() {
		store.save(new TrackStore.Content(lobby, List.copyOf(tracks)));
	}

	@NotNull
	public List<Track> getTracks() {
		return Collections.unmodifiableList(tracks);
	}

	/**
	 * Finds a track by its name, ignoring case.
	 */
	@NotNull
	public Optional<Track> find(@NotNull final String name) {
		final String normalized = normalize(name);
		return tracks.stream()
				.filter(track -> normalize(track.getName()).equals(normalized))
				.findFirst();
	}

	@NotNull
	public Optional<Track> find(@NotNull final UUID id) {
		return tracks.stream()
				.filter(track -> track.getId().equals(id))
				.findFirst();
	}

	/**
	 * Finds a track, other than the given one, whose region shares a block with the given region.
	 */
	@NotNull
	public Optional<Track> findOverlapping(@NotNull final String world, @NotNull final Cuboid region,
			@Nullable final Track except) {
		return tracks.stream()
				.filter(track -> !track.equals(except))
				.filter(track -> track.getWorld().equals(world))
				.filter(track -> track.getRegion().overlaps(region))
				.findFirst();
	}

	/**
	 * Adds a new, stopped track.
	 *
	 * @throws IllegalArgumentException when the name is invalid or taken, or the region overlaps another track
	 */
	@NotNull
	public Track create(@NotNull final String name, @NotNull final String world, @NotNull final Cuboid region) {
		if (find(name).isPresent()) {
			throw new IllegalArgumentException("A track named " + name + " already exists");
		}
		if (findOverlapping(world, region, null).isPresent()) {
			throw new IllegalArgumentException("The region overlaps another track");
		}
		final Track track = new Track(UUID.randomUUID(), name, world, region);
		tracks.add(track);
		return track;
	}

	/**
	 * Removes a stopped track. What it left in the results database stays there.
	 *
	 * @throws IllegalStateException when the track is running
	 */
	public void remove(@NotNull final Track track) {
		if (track.isRunning()) {
			throw new IllegalStateException("A running track cannot be removed");
		}
		tracks.remove(track);
	}

	/**
	 * Renames a track.
	 *
	 * @throws IllegalArgumentException when the name is invalid or taken by another track
	 */
	public void rename(@NotNull final Track track, @NotNull final String name) {
		final Optional<Track> other = find(name);
		if (other.isPresent() && !other.get().equals(track)) {
			throw new IllegalArgumentException("A track named " + name + " already exists");
		}
		track.setName(name);
	}

	@Nullable
	public WorldSpot getLobby() {
		return lobby;
	}

	public void setLobby(@Nullable final WorldSpot lobby) {
		this.lobby = lobby;
	}

	@NotNull
	private static String normalize(@NotNull final String name) {
		return name.toLowerCase(Locale.ROOT);
	}
}
