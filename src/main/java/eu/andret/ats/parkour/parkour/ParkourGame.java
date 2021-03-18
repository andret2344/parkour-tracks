/*
 * Copyright Andret (c) 2019. Copying and modifying allowed only keeping git link reference.
 */
package eu.andret.ats.parkour.parkour;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import eu.andret.ats.parkour.JSONSerializable;
import eu.andret.ats.parkour.event.game.GameStartEvent;
import eu.andret.ats.parkour.event.game.GameStopEvent;
import eu.andret.ats.parkour.event.player.PlayerJoinGameEvent;
import eu.andret.ats.parkour.event.player.PlayerQuitGameEvent;
import eu.andret.ats.parkour.player.ParkourPlayer;
import eu.andret.ats.parkour.player.PlayerManager;
import eu.andret.ats.parkour.region.AbstractRegion;
import eu.andret.ats.parkour.region.Checkpoint;
import eu.andret.ats.parkour.region.EffectRegion;
import eu.andret.ats.parkour.region.GameRegion;
import eu.andret.ats.parkour.region.Wall;
import eu.andret.ats.parkour.util.Medal;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

@Data
public abstract class ParkourGame implements Comparable<ParkourGame>, JSONSerializable {
	private boolean running = false;
	private String name;
	private World world;
	private GameRegion gameRegion;
	private Location bestRecord;
	private Location teleportBlock;
	private Options options = new Options();

	@Getter(AccessLevel.NONE)
	private final List<Checkpoint> checkpoints = new ArrayList<>();
	@Getter(AccessLevel.NONE)
	private final List<Wall> walls = new ArrayList<>();
	@Getter(AccessLevel.NONE)
	private final List<EffectRegion> effectRegions = new ArrayList<>();
	private final Set<String> authors = new TreeSet<>();
	@Getter(AccessLevel.NONE)
	private final List<ParkourPlayer> players = new ArrayList<>();

	public enum ParkourType {
		SERVER,
		TRAINING,
		PLAYERS
	}

	@Data
	@NoArgsConstructor
	public static class Options implements JSONSerializable {
		private boolean enabled = true;
		private boolean sprintForced = false;
		private boolean alwaysSpawn = false;
		private boolean recordsCounting = true;
		private boolean damageAllowed = false;
		private boolean boat = false;
		private boolean modifyInventory = true;
		private boolean available = false;
		private boolean vipOnly = false;
		private int difficulty = 1;
		private double bronze = 0;
		private double silver = 0;
		private double gold = 0;
		private double platinum = 0;
		private double fair = 0;
		@Getter(AccessLevel.NONE)
		private final Map<PotionEffectType, Integer> effects = new HashMap<>();
		private DyeColor color = DyeColor.WHITE;
		private ParkourType type = ParkourType.SERVER;
		private String displayName;

		public Options(final Options options) {
			enabled = options.enabled;
			sprintForced = options.sprintForced;
			alwaysSpawn = options.alwaysSpawn;
			recordsCounting = options.recordsCounting;
			damageAllowed = options.damageAllowed;
			boat = options.boat;
			modifyInventory = options.modifyInventory;
			available = options.available;
			vipOnly = options.vipOnly;
			bronze = options.bronze;
			silver = options.silver;
			gold = options.gold;
			platinum = options.platinum;
			difficulty = options.difficulty;
			fair = options.fair;
			effects.putAll(options.effects);
			color = options.color;
			type = options.type;
			displayName = options.displayName;
		}

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

		@Override
		public JSONObject toJSON() {
			final JSONObject result = new JSONObject();
			result.put("alwaysSpawn", alwaysSpawn);
			result.put("boat", boat);
			result.put("recordCounting", recordsCounting);
			result.put("damageAllowed", damageAllowed);
			result.put("sprintForced", sprintForced);
			result.put("modifyInventory", modifyInventory);
			result.put("bronze", bronze);
			result.put("silver", silver);
			result.put("gold", gold);
			result.put("platinum", platinum);
			result.put("available", available);
			result.put("color", color.name());
			result.put("difficulty", difficulty);
			result.put("displayName", displayName);
			result.put("vipOnly", vipOnly);
			result.put("enabled", enabled);
			result.put("fair", fair);
			result.put("type", type.toString());
			final JSONArray localEffects = new JSONArray();
			for (final Entry<PotionEffectType, Integer> entry : effects.entrySet()) {
				final JSONObject object = new JSONObject();
				object.put("name", entry.getKey().getName());
				object.put("amplifier", entry.getValue());
				localEffects.put(object);
			}
			result.put("effects", localEffects);
			return result;
		}

		@Override
		public void fromJSON(final JSONObject options) {
			alwaysSpawn = options.getBoolean("alwaysSpawn");
			boat = options.getBoolean("boat");
			recordsCounting = options.getBoolean("recordCounting");
			damageAllowed = options.getBoolean("damageAllowed");
			sprintForced = options.getBoolean("sprintForced");
			modifyInventory = options.getBoolean("modifyInventory");
			bronze = options.getDouble("bronze");
			silver = options.getDouble("silver");
			gold = options.getDouble("gold");
			platinum = options.getDouble("platinum");
			color = DyeColor.valueOf(options.getString("color"));
			difficulty = options.getInt("difficulty");
			final JSONArray localEffects = options.getJSONArray("effects");
			for (int i = 0; i < localEffects.length(); i++) {
				final JSONObject jsonObject = localEffects.getJSONObject(i);
				final String name = jsonObject.getString("name");
				final int amplifier = jsonObject.getInt("apmplifier");
				effects.put(PotionEffectType.getByName(name), amplifier);
			}
			available = options.getBoolean("available");
			type = ParkourType.valueOf(options.getString("type"));
			displayName = options.getString("displayName");
			vipOnly = options.getBoolean("vipOnly");
			enabled = options.getBoolean("enabled");
			fair = options.getDouble("fair");
		}
	}

	protected ParkourGame(final String name, final GameRegion gameRegion, final World world) {
		this.name = name;
		this.gameRegion = gameRegion;
		this.world = world;
		options.displayName = name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public void addCheckpoint(final Checkpoint checkpoint) {
		checkpoints.add(checkpoint);
	}

	public void setCheckpoint(final int id, final CuboidRegion checkpoint) {
		if (id == 0) {
			throw new ArrayIndexOutOfBoundsException("Index must be positive, " + id + " provided");
		}
		checkpoints.get(id).setCuboidRegion(checkpoint);
	}

	public void setCheckpoint(final int id, final Checkpoint checkpoint) {
		if (id == 0) {
			throw new ArrayIndexOutOfBoundsException("Index must be positive, " + id + " provided");
		}
		checkpoints.set(id, checkpoint);
	}

	public void addWall(final Wall wall) {
		walls.add(wall);
	}

	public void addEffectRegion(final EffectRegion effectRegion) {
		effectRegions.add(effectRegion);
	}

	public void setWall(final int id, final CuboidRegion wall) {
		walls.get(id).setCuboidRegion(wall);
	}

	public void setWall(final int id, final Wall wall) {
		walls.set(id, wall);
	}

	public void setEffectRegion(final int id, final EffectRegion effectRegion) {
		effectRegions.set(id, effectRegion);
	}

	public void setSpawn(final Checkpoint newSpawnLocation) {
		if (checkpoints.isEmpty()) {
			checkpoints.add(newSpawnLocation);
		} else {
			checkpoints.set(0, newSpawnLocation);
		}
	}

	public abstract void addPlayer(Player player);

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

	public void start() {
		running = true;
		Bukkit.getServer().getPluginManager().callEvent(new GameStartEvent(this));
	}

	public void stop() {
		running = false;
		Bukkit.getServer().getPluginManager().callEvent(new GameStopEvent(this));
	}

	public boolean inAnyRegion(final Location location) {
		if (world == null || !world.equals(location.getWorld())) {
			return false;
		}
		return getAllRegions().stream().anyMatch(region -> region.contains(location));
	}

	public List<AbstractRegion> getAllRegions() {
		final List<AbstractRegion> arr = new ArrayList<>();
		arr.add(gameRegion);
		arr.addAll(walls);
		arr.addAll(checkpoints);
		arr.addAll(effectRegions);
		return arr;
	}

	public Checkpoint getFinish() {
		return checkpoints.get(checkpoints.size() - 1);
	}

	public List<ParkourPlayer> getPlayers() {
		return players;
	}

	public int getLastCheckpointId() {
		return checkpoints.size() - 1;
	}

	public Checkpoint getCheckpoint(final int id) {
		return checkpoints.get(id);
	}

	public List<Checkpoint> getCheckpointList() {
		return checkpoints;
	}

	public Wall getWall(final int id) {
		return walls.get(id);
	}

	public List<Wall> getWallList() {
		return walls;
	}

	public EffectRegion getEffectRegion(final int id) {
		return effectRegions.get(id);
	}

	public List<EffectRegion> getEffectRegionList() {
		return effectRegions;
	}

	public Checkpoint getSpawn() {
		if (checkpoints.isEmpty()) {
			return null;
		}
		return checkpoints.get(0);
	}

	public List<String> getAuthors() {
		return new ArrayList<>(authors);
	}

	// TODO: Move json (de)serialize to dedicated class
	@Override
	public JSONObject toJSON() {
		if (gameRegion == null) {
			return new JSONObject();
		}
		final JSONObject object = new JSONObject();
		object.put("region", gameRegion.toJSON());
		object.put("checkpoints", collectionToJSON(checkpoints));
		object.put("walls", collectionToJSON(walls));
		object.put("effectRegions", collectionToJSON(effectRegions));
		object.put("recordsSign", bestRecord == null ? null : locationToJSON(bestRecord));
		object.put("teleportBlock", teleportBlock == null ? null : locationToJSON(teleportBlock));
		object.put("options", options.toJSON());
		return object;
	}

	private JSONArray collectionToJSON(final List<? extends AbstractRegion> regions) {
		final JSONArray jsonArray = new JSONArray();
		regions.stream().map(AbstractRegion::toJSON).forEach(jsonArray::put);
		return jsonArray;
	}

	private JSONObject locationToJSON(final Location location) {
		final JSONObject jsonObject = new JSONObject();
		jsonObject.put("x", location.getX());
		jsonObject.put("y", location.getY());
		jsonObject.put("z", location.getZ());
		jsonObject.put("world", location.getWorld().getName());
		return jsonObject;
	}

	private Location locationFromJSON(final JSONObject jsonObject) {
		if (jsonObject == null) {
			return null;
		}
		return new Location(Bukkit.getWorld(jsonObject.getString("world")),
				jsonObject.getInt(("x")), jsonObject.getInt(("y")), jsonObject.getInt("z"));
	}

	private <E extends AbstractRegion> List<E> collectionFromJSON(final JSONArray jsonArray, final Class<E> clazz) {
		final List<E> list = new ArrayList<>();
		try {
			final int length = jsonArray.length();
			for (int i = 0; i < length; i++) {
				final JSONObject jsonObject = jsonArray.getJSONObject(i);
				final E e = clazz.getConstructor(CuboidRegion.class).newInstance(new CuboidRegion(BlockVector3.ZERO, BlockVector3.ZERO));
				e.fromJSON(jsonObject);
				list.add(e);
			}
		} catch (final ReflectiveOperationException ex) {
			Bukkit.getLogger().throwing(getClass().getName(), "collectionFromJSON", ex);
		}
		return list;
	}

	@Override
	public void fromJSON(final JSONObject object) {
		gameRegion = new GameRegion(world);
		gameRegion.fromJSON(object.getJSONObject("region"));
		checkpoints.clear();
		checkpoints.addAll(collectionFromJSON(object.getJSONArray("checkpoints"), Checkpoint.class));
		walls.clear();
		walls.addAll(collectionFromJSON(object.getJSONArray("walls"), Wall.class));
		effectRegions.clear();
		effectRegions.addAll(collectionFromJSON(object.getJSONArray("effectRegions"), EffectRegion.class));
		bestRecord = object.has("recordsSign") ? locationFromJSON(object.getJSONObject("recordsSign")) : null;
		teleportBlock = object.has("teleportBlock") ? locationFromJSON(object.getJSONObject("teleportBlock")) : null;
		options.fromJSON(object.getJSONObject("options"));
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

	public void setAuthors(final Collection<String> authors) {
		this.authors.clear();
		this.authors.addAll(authors);
	}
}
