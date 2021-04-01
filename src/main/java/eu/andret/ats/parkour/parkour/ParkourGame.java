/*
 * Copyright Andret (c) 2019-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.parkour;

import eu.andret.ats.parkour.player.ParkourPlayer;
import eu.andret.ats.parkour.region.BasicRegion;
import eu.andret.ats.parkour.region.DirectionalRegion;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import lombok.Value;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Data
@ToString
public abstract class ParkourGame implements Comparable<ParkourGame> {
	private final List<DirectionalRegion> checkpoints = new ArrayList<>();
	private final List<BasicRegion> walls = new ArrayList<>();
	private final Set<String> authors = new TreeSet<>();
	private final List<ParkourPlayer> players = new ArrayList<>();
	private final Map<PotionEffectType, Integer> effects = new HashMap<>();
	private final Map<ParkourMedal, ParkourMedalData> medals = new HashMap<>();

	private String name;
	private World world;
	private BasicRegion region;
	private Location recordsBlock;
	private Location teleportBlock;
	private DirectionalRegion spawn;
	private boolean running;
	private String displayName;

	private Options options = Options.builder().build();

	public enum Type {
		SERVER,
		TRAINING,
		PLAYERS
	}

	@Value
	public static class Result {
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
		private int difficulty = 1;
		@Builder.Default
		private DyeColor color = DyeColor.WHITE;
		@Builder.Default
		private Type type = Type.SERVER;
	}

	protected ParkourGame(final String name, final BasicRegion region, final World world) {
		this.name = displayName = name;
		this.region = region;
		this.world = world;
	}

	public List<BasicRegion> getAllRegions() {
		final List<BasicRegion> arr = new ArrayList<>();
		arr.add(region);
		arr.add(spawn);
		arr.addAll(walls);
		arr.addAll(checkpoints);
		return arr.stream().filter(Objects::nonNull).collect(Collectors.toList());
	}

	public boolean addPlayer(final ParkourPlayer parkourPlayer) {
		if (players.contains(parkourPlayer)) {
			return false;
		}
		players.add(parkourPlayer);
		return true;
	}

	public boolean removePlayer(final ParkourPlayer parkourPlayer) {
		if (!players.contains(parkourPlayer)) {
			return false;
		}
		players.remove(parkourPlayer);
		return true;
	}

	public boolean inSpawn(final ParkourPlayer player) {
		if (player == null) {
			return false;
		}
		if (spawn == null) {
			return false;
		}
		return spawn.contains(player);
	}

	public boolean inCheckpoint(final ParkourPlayer player) {
		if (player == null) {
			return false;
		}
		return checkpoints.stream().anyMatch(checkpoint -> checkpoint.contains(player));
	}

	@NotNull
	public Result getResult(final double time) {
		double smallest = Double.POSITIVE_INFINITY;
		ParkourMedal medal = null;
		double reward = 0;
		for (final Map.Entry<ParkourMedal, ParkourMedalData> entry : medals.entrySet()) {
			final ParkourMedalData parkourMedalData = entry.getValue();
			if (parkourMedalData.getTime() < time) {
				continue;
			}
			reward += parkourMedalData.getReward();
			if (parkourMedalData.getTime() < smallest) {
				smallest = parkourMedalData.getTime();
				medal = entry.getKey();
			}
		}
		return new Result(medal, reward);
	}

	@Override
	public int compareTo(@Nonnull final ParkourGame parkourGame) {
		if (running && !parkourGame.running) {
			return 1;
		}
		if (!running && parkourGame.running) {
			return -1;
		}
		if (options.difficulty != parkourGame.options.difficulty) {
			return options.difficulty - parkourGame.options.difficulty;
		}
		return name.compareToIgnoreCase(parkourGame.name);
	}
}
