/*
 * Copyright Andret (c) 2018-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.parkour;

import eu.andret.ats.parkour.player.ParkourPlayer;
import eu.andret.ats.parkour.region.BasicRegion;
import eu.andret.ats.parkour.region.DirectionalRegion;
import eu.andret.ats.parkour.region.ParkourRegion;
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
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Data
@ToString
public abstract class ParkourGame implements Comparable<ParkourGame> {
	@NotNull
	private final List<DirectionalRegion> checkpoints = new ArrayList<>();
	@NotNull
	private final List<ParkourRegion> walls = new ArrayList<>();
	@NotNull
	private final Set<String> authors = new TreeSet<>();
	@NotNull
	private final transient List<ParkourPlayer> players = new ArrayList<>();
	@NotNull
	private final List<ParkourEffect> effects = new ArrayList<>();
	@NotNull
	private final List<ParkourMedalData> medals = new ArrayList<>();

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
	@Nullable
	private DirectionalRegion spawn;

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
		ParkourMedal medal;
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

	protected ParkourGame(@NotNull final String name, @NotNull final BasicRegion region, @NotNull final World world) {
		this.name = displayName = name;
		this.region = region;
		this.world = world;
	}

	@NotNull
	public List<BasicRegion> getAllRegions() {
		final List<BasicRegion> arr = new ArrayList<>();
		arr.add(region);
		arr.add(spawn);
		arr.addAll(walls);
		arr.addAll(checkpoints);
		return arr.stream().filter(Objects::nonNull).collect(Collectors.toList());
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
		return Optional.ofNullable(spawn)
				.map(spawnLocation -> spawnLocation.contains(player))
				.orElse(false);
	}

	public boolean inCheckpoint(@NotNull final ParkourPlayer player) {
		return checkpoints.stream().anyMatch(checkpoint -> checkpoint.contains(player));
	}

	@NotNull
	public Result getResult(final double time) {
		double smallest = Double.POSITIVE_INFINITY;
		ParkourMedal medal = null;
		double reward = 0;
		for (final ParkourMedalData parkourMedalData : medals) {
			if (parkourMedalData.getTime() < time) {
				continue;
			}
			reward += parkourMedalData.getReward();
			if (parkourMedalData.getTime() < smallest) {
				smallest = parkourMedalData.getTime();
				medal = parkourMedalData.getMedal();
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
