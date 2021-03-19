/*
 * Copyright Andret (c) 2019. Copying and modifying allowed only keeping git link reference.
 */
package eu.andret.ats.parkour.parkour;

import eu.andret.ats.parkour.event.player.PlayerJoinGameEvent;
import eu.andret.ats.parkour.event.player.PlayerQuitGameEvent;
import eu.andret.ats.parkour.player.ParkourPlayer;
import eu.andret.ats.parkour.player.PlayerManager;
import eu.andret.ats.parkour.region.AbstractRegion;
import eu.andret.ats.parkour.region.DirectionalRegion;
import eu.andret.ats.parkour.util.Medal;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

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
public abstract class ParkourGame implements Comparable<ParkourGame> {
	private boolean running;
	private String name;
	private World world;
	private AbstractRegion gameRegion;
	private Location recordsBlock;
	private Location teleportBlock;
	private DirectionalRegion spawn;
	private String displayName;

	private Options options = Options.builder().build();

	private final List<DirectionalRegion> checkpoints = new ArrayList<>();
	private final List<AbstractRegion> walls = new ArrayList<>();
	private final Set<String> authors = new TreeSet<>();
	private final List<ParkourPlayer> players = new ArrayList<>();

	public enum ParkourType {
		SERVER,
		TRAINING,
		PLAYERS
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
		private boolean recordsCounting = true;
		@Builder.Default
		private boolean damageAllowed = false;
		@Builder.Default
		private boolean boat = false;
		@Builder.Default
		private boolean modifyInventory = true;
		@Builder.Default
		private boolean available = false;
		@Builder.Default
		private boolean vipOnly = false;
		@Builder.Default
		private int difficulty = 1;
		@Builder.Default
		private double bronze = 0;
		@Builder.Default
		private double silver = 0;
		@Builder.Default
		private double gold = 0;
		@Builder.Default
		private double platinum = 0;
		@Builder.Default
		private double fair = 0;
		@Getter(AccessLevel.NONE)
		private final Map<PotionEffectType, Integer> effects = new HashMap<>();
		@Builder.Default
		private DyeColor color = DyeColor.WHITE;
		@Builder.Default
		private ParkourType type = ParkourType.SERVER;

		public Medal getMedalByTime(final double time) {
			if (platinum >= time) {
				return Medal.PLATINUM;
			}
			if (gold >= time) {
				return Medal.GOLD;
			}
			if (silver >= time) {
				return Medal.SILVER;
			}
			if (bronze >= time) {
				return Medal.BRONZE;
			}
			return Medal.NONE;
		}

		public void setEffect(final PotionEffectType effect, final int amplifier) {
			effects.put(effect, amplifier);
		}

		public int removeEffect(final PotionEffectType effect) {
			return effects.remove(effect);
		}

		public Map<PotionEffectType, Integer> getEffects() {
			return new HashMap<>(effects);
		}
	}

	protected ParkourGame(final String name, final AbstractRegion gameRegion, final World world) {
		this.name = displayName = name;
		this.gameRegion = gameRegion;
		this.world = world;
	}

	public List<AbstractRegion> getAllRegions() {
		final List<AbstractRegion> arr = new ArrayList<>();
		arr.add(gameRegion);
		arr.add(spawn);
		arr.addAll(walls);
		arr.addAll(checkpoints);
		return arr.stream().filter(Objects::nonNull).collect(Collectors.toList());
	}

	public int getLastCheckpointId() {
		return checkpoints.size() - 1;
	}

	protected void addPlayer(final ParkourPlayer parkourPlayer) {
		if (!players.contains(parkourPlayer) && !parkourPlayer.inAnyParkour()) {
			players.add(parkourPlayer);
			parkourPlayer.reset();
			Bukkit.getPluginManager().callEvent(new PlayerJoinGameEvent(this, parkourPlayer));
			parkourPlayer.setLastVisitedCheckpointId(0);
		}
	}

	public boolean removePlayer(final Player player) {
		final ParkourPlayer parkourPlayer = PlayerManager.getParkourSinglePlayer(player);
		Bukkit.getServer().getPluginManager().callEvent(new PlayerQuitGameEvent(this, parkourPlayer));
		parkourPlayer.reset();
		return players.remove(parkourPlayer);
	}

	public abstract void addPlayer(Player player);

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
