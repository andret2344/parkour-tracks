/*
 * Copyright Andret (c) 2018-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.parkour;

import eu.andret.ats.parkour.player.ParkourPlayer;
import eu.andret.ats.parkour.region.BasicRegion;
import eu.andret.ats.parkour.region.Checkpoint;
import eu.andret.ats.parkour.region.Wall;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import lombok.Value;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Data
@ToString
public abstract class ParkourGame implements Comparable<ParkourGame> {
	@NotNull
	private final List<Checkpoint> checkpoints = new ArrayList<>();
	@NotNull
	private final List<Wall> walls = new ArrayList<>();
	@NotNull
	private final transient List<ParkourPlayer> players = new ArrayList<>();
	@NotNull
	private final List<Effect> effects = new ArrayList<>();
	@NotNull
	private final List<ParkourMedal> medals = new ArrayList<>();
	private final int id;

	@NotNull
	private String name;
	@NotNull
	private String displayName;
	private boolean running;
	@NotNull
	private World world;
	@NotNull
	private BasicRegion region;
	@Nullable
	private Location recordsBlock;
	@Nullable
	private Location teleportBlock;

	@NotNull
	private Options options = Options.builder().build();

	public enum Type {
		SERVER,
		TRAINING,
		PLAYERS
	}

	@Value
	public static class Result {
		@Nullable
		Medal medal;
		double reward;
	}

	@Data
	@Builder
	public static class Options {
		@Builder.Default
		private boolean enabled = true;
		@Builder.Default
		private boolean sprintForced = false;
		@Builder.Default
		private boolean alwaysSpawn = false;
		@Builder.Default
		private boolean savingResults = true;
		@Builder.Default
		private boolean damageAllowed = false;
		@Builder.Default
		private boolean boat = false;
		@Builder.Default
		private boolean modifyInventory = true;
		@Builder.Default
		private boolean vipOnly = false;
		@Builder.Default
		private double fee = 0;
		@Builder.Default
		private double reward = 0;
		@Builder.Default
		private int difficulty = 1;
		@Builder.Default
		private DyeColor color = DyeColor.WHITE;
		@Builder.Default
		private Type type = Type.SERVER;
	}

	protected ParkourGame(final int id, @NotNull final String name, @NotNull final BasicRegion region, @NotNull final World world) {
		this.id = id;
		this.name = displayName = name;
		this.region = region;
		this.world = world;
	}

	@NotNull
	public List<BasicRegion> getAllRegions() {
		final List<BasicRegion> arr = new ArrayList<>();
		arr.add(region);
		arr.addAll(walls);
		arr.addAll(checkpoints);
		return arr.stream()
				.filter(Objects::nonNull)
				.toList();
	}

	public boolean addPlayer(@NotNull final ParkourPlayer parkourPlayer) {
		if (players.contains(parkourPlayer)) {
			return false;
		}
		players.add(parkourPlayer);
		return true;
	}

	public boolean removePlayer(@NotNull final ParkourPlayer parkourPlayer) {
		if (!players.contains(parkourPlayer)) {
			return false;
		}
		players.remove(parkourPlayer);
		return true;
	}

	public boolean inSpawn(@NotNull final ParkourPlayer player) {
		return Optional.of(checkpoints)
				.map(list -> list.get(0))
				.map(checkpoint -> checkpoint.contains(player))
				.orElse(false);
	}

	public boolean inCheckpoint(@NotNull final ParkourPlayer player) {
		return checkpoints.stream().anyMatch(checkpoint -> checkpoint.contains(player));
	}

	@NotNull
	public Result getResult(final double time) {
		double smallest = Double.POSITIVE_INFINITY;
		Medal medal = null;
		double reward = 0;
		for (final ParkourMedal parkourMedal : medals) {
			if (parkourMedal.getTime() < time) {
				continue;
			}
			reward += parkourMedal.getReward();
			if (parkourMedal.getTime() < smallest) {
				smallest = parkourMedal.getTime();
				medal = parkourMedal.getMedal();
			}
		}
		return new Result(medal, reward);
	}

	@Override
	public int compareTo(@NotNull final ParkourGame parkourGame) {
		if (running && !parkourGame.running) {
			return -1;
		}
		if (!running && parkourGame.running) {
			return 1;
		}
		if (options.difficulty != parkourGame.options.difficulty) {
			return options.difficulty - parkourGame.options.difficulty;
		}
		return name.compareToIgnoreCase(parkourGame.name);
	}
}
