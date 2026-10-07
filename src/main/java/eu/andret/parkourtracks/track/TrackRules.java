package eu.andret.parkourtracks.track;

import eu.andret.parkourtracks.config.Medal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The rules a track's configuration has to follow, checked before a change is made.
 */
public final class TrackRules {
	private TrackRules() {
	}

	/**
	 * Why a part of a track cannot be put where it was selected.
	 */
	public enum PlacementProblem {
		/**
		 * The selection, or the player, is in another world than the track.
		 */
		OTHER_WORLD,
		/**
		 * The selection is not wholly inside the track's region.
		 */
		OUTSIDE_REGION,
		/**
		 * The player, whose position becomes the spot of a checkpoint, stands outside the selected area.
		 */
		SPOT_OUTSIDE_AREA
	}

	/**
	 * Checks a wall, which has no spot.
	 */
	@NotNull
	public static Optional<PlacementProblem> validateWall(@NotNull final Track track, @NotNull final String world,
			@NotNull final Cuboid area) {
		if (!track.getWorld().equals(world)) {
			return Optional.of(PlacementProblem.OTHER_WORLD);
		}
		if (!track.getRegion().contains(area)) {
			return Optional.of(PlacementProblem.OUTSIDE_REGION);
		}
		return Optional.empty();
	}

	/**
	 * Checks a spawn, finish or checkpoint: its area selected in {@code selectionWorld}, its spot where the player
	 * stands, in {@code playerWorld}.
	 */
	@NotNull
	public static Optional<PlacementProblem> validateCheckpoint(@NotNull final Track track,
			@NotNull final String selectionWorld,
			@NotNull final String playerWorld,
			@NotNull final Checkpoint checkpoint) {
		if (!track.getWorld().equals(playerWorld)) {
			return Optional.of(PlacementProblem.OTHER_WORLD);
		}
		final Optional<PlacementProblem> area = validateWall(track, selectionWorld, checkpoint.area());
		if (area.isPresent()) {
			return area;
		}
		final Spot spot = checkpoint.spot();
		if (!checkpoint.area().contains(spot.x(), spot.y(), spot.z())) {
			return Optional.of(PlacementProblem.SPOT_OUTSIDE_AREA);
		}
		return Optional.empty();
	}

	/**
	 * Why a track's main region cannot be changed to the selected one.
	 *
	 * @param overlapping the track overlapped, for {@link RegionProblem.Kind#OVERLAP}
	 */
	public record RegionProblem(@NotNull Kind kind, @Nullable Track overlapping) {
		public enum Kind {
			/**
			 * The region shares blocks with the region of another track.
			 */
			OVERLAP,
			/**
			 * Something of the track (spawn, finish, a checkpoint or a wall) would be left outside.
			 */
			LEAVES_OUT
		}
	}

	/**
	 * Checks a new main region for a track, or for a new track when {@code track} is {@code null}.
	 */
	@NotNull
	public static Optional<RegionProblem> validateRegion(@NotNull final TrackRegistry registry,
			@Nullable final Track track, @NotNull final String world,
			@NotNull final Cuboid region) {
		final Optional<Track> overlapping = registry.findOverlapping(world, region, track);
		if (overlapping.isPresent()) {
			return Optional.of(new RegionProblem(RegionProblem.Kind.OVERLAP, overlapping.get()));
		}
		if (track == null) {
			return Optional.empty();
		}
		// The parts lie in the track's world, so a region in another world leaves out any of them
		final boolean leavesOut = collectParts(track).anyMatch(part -> !track.getWorld().equals(world) || !region.contains(part));
		return leavesOut ? Optional.of(new RegionProblem(RegionProblem.Kind.LEAVES_OUT, null)) : Optional.empty();
	}

	/**
	 * The areas of the spawn, the finish, the checkpoints and the walls.
	 */
	@NotNull
	private static Stream<Cuboid> collectParts(@NotNull final Track track) {
		final Stream<Checkpoint> checkpoints = Stream.concat(
				Stream.concat(Stream.ofNullable(track.getSpawn()), Stream.ofNullable(track.getFinish())),
				track.getCheckpoints().stream());
		return Stream.concat(checkpoints.map(Checkpoint::area), track.getWalls().stream());
	}

	/**
	 * Finds a medal whose time would break the order with the given new time: a better medal (earlier in the config)
	 * needs a shorter time than every worse one. Medals without a time set are not compared.
	 *
	 * @return the medal the new time clashes with
	 */
	@NotNull
	public static Optional<Medal> findMedalOrderClash(@NotNull final List<Medal> medals,
			@NotNull final Map<String, MedalThreshold> thresholds,
			@NotNull final Medal medal, final int ticks) {
		final int position = medals.indexOf(medal);
		for (int i = 0; i < medals.size(); i++) {
			final Medal other = medals.get(i);
			final MedalThreshold threshold = thresholds.get(other.key());
			if (i == position || threshold == null || !threshold.hasTime()) {
				continue;
			}
			final boolean better = i < position;
			if (better && threshold.ticks() >= ticks || !better && threshold.ticks() <= ticks) {
				return Optional.of(other);
			}
		}
		return Optional.empty();
	}

	/**
	 * The best medal a run of the given ticks earns on a track: the first medal, in the config's order, whose
	 * threshold has a time the run did not exceed.
	 */
	@NotNull
	public static Optional<Medal> findBestMedal(@NotNull final List<Medal> medals,
			@NotNull final Map<String, MedalThreshold> thresholds, final int ticks) {
		return medals.stream()
				.filter(medal -> {
					final MedalThreshold threshold = thresholds.get(medal.key());
					return threshold != null && threshold.hasTime() && ticks <= threshold.ticks();
				})
				.findFirst();
	}

	/**
	 * What a track misses before it can start.
	 */
	public enum Missing {
		SPAWN,
		FINISH,
		LOBBY
	}

	@NotNull
	public static List<Missing> findMissingForStart(@NotNull final TrackRegistry registry, @NotNull final Track track) {
		final List<Missing> missing = new ArrayList<>();
		if (track.getSpawn() == null) {
			missing.add(Missing.SPAWN);
		}
		if (track.getFinish() == null) {
			missing.add(Missing.FINISH);
		}
		if (registry.getLobby() == null) {
			missing.add(Missing.LOBBY);
		}
		return missing;
	}
}
