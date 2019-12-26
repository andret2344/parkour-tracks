/*
 * Copyright Andret (c) 2019. Copying and modifying allowed only keeping git link reference.
 */
package eu.andret.parkour.parkour;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import eu.andret.parkour.JSONSerializable;
import eu.andret.parkour.event.game.GameStartEvent;
import eu.andret.parkour.event.game.GameStopEvent;
import eu.andret.parkour.event.player.PlayerJoinGameEvent;
import eu.andret.parkour.event.player.PlayerQuitGameEvent;
import eu.andret.parkour.player.ParkourPlayer;
import eu.andret.parkour.player.PlayerManager;
import eu.andret.parkour.region.AbstractRegion;
import eu.andret.parkour.region.Checkpoint;
import eu.andret.parkour.region.EffectRegion;
import eu.andret.parkour.region.GameRegion;
import eu.andret.parkour.region.Wall;
import eu.andret.parkour.util.Medal;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.MemorySection;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

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
	@Getter(AccessLevel.NONE)
	private final List<String> authors = new ArrayList<>();
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
		private boolean forcingSprint = false;
		private boolean alwaysSpawn = false;
		private boolean countingRecords = true;
		private boolean allowingDamage = false;
		private boolean boat = false;
		private boolean modifyInventory = true;
		private boolean available = false;
		private boolean vip = false;
		private int price = 0;
		private int bronze = 0;
		private int silver = 0;
		private int gold = 0;
		private int platinum = 0;
		private int difficulty = 1;
		private int xp = 0;
		private float fair = 0F;
		@Getter(AccessLevel.NONE)
		private final Map<PotionEffectType, Integer> effects = new HashMap<>();
		private DyeColor color = DyeColor.WHITE;
		private ParkourType type = ParkourType.SERVER;
		private String displayName;

		public Options(Options options) {
			enabled = options.enabled;
			forcingSprint = options.forcingSprint;
			alwaysSpawn = options.alwaysSpawn;
			countingRecords = options.countingRecords;
			allowingDamage = options.allowingDamage;
			boat = options.boat;
			modifyInventory = options.modifyInventory;
			available = options.available;
			vip = options.vip;
			bronze = options.bronze;
			silver = options.silver;
			gold = options.gold;
			platinum = options.platinum;
			difficulty = options.difficulty;
			xp = options.xp;
			fair = options.fair;
			effects.putAll(options.effects);
			color = options.color;
			type = options.type;
			price = options.price;
			displayName = options.displayName;
		}

		public Medal getMedalByTime(float time) {
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

		public void setEffect(PotionEffectType effect, int amplifier) {
			effects.put(effect, amplifier);
		}

		public int removeEffect(PotionEffectType effect) {
			return effects.remove(effect);
		}

		public Map<PotionEffectType, Integer> getEffects() {
			return new HashMap<>(effects);
		}

		@Override
		public JSONObject toJSON() {
			JSONObject result = new JSONObject();
			result.put("alwaysSpawn", alwaysSpawn);
			result.put("boat", boat);
			result.put("countingRecords", countingRecords);
			result.put("allowingDamage", allowingDamage);
			result.put("forcingSprint", forcingSprint);
			result.put("price", price);
			result.put("modifyInventory", modifyInventory);
			result.put("bronze", bronze);
			result.put("silver", silver);
			result.put("gold", gold);
			result.put("platinum", platinum);
			result.put("available", available);
			result.put("color", color.name());
			result.put("difficulty", difficulty);
			result.put("displayname", displayName);
			result.put("vip", vip);
			result.put("enabled", enabled);
			result.put("fair", fair);
			result.put("xp", xp);
			result.put("type", type.toString());
			Map<String, Integer> localEffects = new HashMap<>();
			for (Entry<PotionEffectType, Integer> entry : effects.entrySet()) {
				localEffects.put(entry.getKey().getName(), entry.getValue());
			}
			result.put("effects", localEffects);
			return result;
		}

		@Override
		public void fromJSON(JSONObject options) {
			alwaysSpawn = (boolean) options.get("alwaysSpawn");
			boat = (boolean) options.get("boat");
			countingRecords = (boolean) options.get("countingRecords");
			allowingDamage = (boolean) options.get("allowingDamage");
			forcingSprint = (boolean) options.get("forcingSprint");
			price = (int) options.get("price");
			modifyInventory = (boolean) options.get("modifyInventory");
			bronze = (int) options.get("bronze");
			silver = (int) options.get("silver");
			gold = (int) options.get("gold");
			platinum = (int) options.get("platinum");
			color = DyeColor.valueOf((String) options.get("color"));
			difficulty = (int) options.get("difficulty");
			MemorySection m = (MemorySection) options.get("effects");
			if (m != null) {
				for (String s : m.getKeys(false)) {
					effects.put(PotionEffectType.getByName(s), m.getInt(s));
				}
			}
			available = (boolean) options.get("available");
			type = ParkourType.valueOf((String) options.get("type"));
			displayName = (String) options.get("displayname");
			try {
				vip = (boolean) options.get("vip");
			} catch (Exception ex) {
				vip = false;
			}

			try {
				enabled = (boolean) options.get("enabled");
			} catch (Exception ex) {
				enabled = true;
			}

			try {
				fair = (float) options.get("fair");
			} catch (Exception ex) {
				fair = 0.0F;
			}

			try {
				xp = (int) options.get("xp");
			} catch (Exception ex) {
				xp = 0;
			}
		}
	}

	public ParkourGame(String name, GameRegion gameRegion, World world) {
		this.name = name;
		this.gameRegion = gameRegion;
		this.world = world;
		options.displayName = name;
		ParkourManager.addParkour(this);
	}

	public void setName(String newName) {
		name = newName;
		ParkourManager.sortGames();
	}

	public void addCheckpoint(Checkpoint checkpoint) {
		checkpoints.add(checkpoint);
	}

	public void setCheckpoint(int id, CuboidRegion checkpoint) {
		if (id == 0) {
			throw new ArrayIndexOutOfBoundsException();
		}
		checkpoints.get(id).setRegion(checkpoint);
	}

	public void setCheckpoint(int id, Checkpoint checkpoint) {
		if (id == 0) {
			throw new ArrayIndexOutOfBoundsException();
		}
		checkpoints.set(id, checkpoint);
	}

	public void addWall(Wall wall) {
		walls.add(wall);
	}

	public void addEffectRegion(EffectRegion effectRegion) {
		effectRegions.add(effectRegion);
	}

	public void setWall(int id, CuboidRegion wall) {
		walls.get(id).setRegion(wall);
	}

	public void setWall(int id, Wall wall) {
		walls.set(id, wall);
	}

	public void setEffectRegion(int id, EffectRegion effectRegion) {
		effectRegions.set(id, effectRegion);
	}

	public void addAuthor(String author) {
		authors.add(author);
	}

	public void setSpawn(Checkpoint newSpawnLocation) {
		if (checkpoints.isEmpty()) {
			checkpoints.add(newSpawnLocation);
		} else {
			checkpoints.set(0, newSpawnLocation);
		}
	}


	public abstract void addPlayer(Player player);

	protected void addPlayer(ParkourPlayer pl) {
		if (!players.contains(pl) && !pl.inAnyParkour()) {
			players.add(pl);
			pl.reset();
			Bukkit.getPluginManager().callEvent(new PlayerJoinGameEvent(this, pl));
			pl.setLastVisitedCheckpointId(0);
		}
	}

	public boolean removePlayer(Player player) {
		ParkourPlayer p = PlayerManager.getParkourSinglePlayer(player);
		Bukkit.getServer().getPluginManager().callEvent(new PlayerQuitGameEvent(this, p));
		p.reset();
		return players.remove(p);
	}

	public void start() {
		running = true;
		Bukkit.getServer().getPluginManager().callEvent(new GameStartEvent(this));
		ParkourManager.sortGames();
	}

	public void stop() {
		running = false;
		Bukkit.getServer().getPluginManager().callEvent(new GameStopEvent(this));
		ParkourManager.sortGames();
	}

	public boolean inAnyRegion(Location loc) {
		if (world == null || !world.equals(loc.getWorld())) {
			return false;
		}
		for (AbstractRegion r : getAllRegions()) {
			if (r == null) {
				Bukkit.getServer().getLogger().log(Level.INFO, "[ParkourPlugin] Debug: Region is null!");
			} else if (r.contains(loc)) {
				return true;
			}
		}
		return false;
	}

	public List<AbstractRegion> getAllRegions() {
		List<AbstractRegion> arr = new ArrayList<>();
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

	public Checkpoint getCheckpoint(int id) {
		return checkpoints.get(id);
	}

	public List<Checkpoint> getCheckpointList() {
		return checkpoints;
	}

	public Wall getWall(int id) {
		return walls.get(id);
	}

	public List<Wall> getWallList() {
		return walls;
	}

	public EffectRegion getEffectRegion(int id) {
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

	@Override
	public JSONObject toJSON() {
		if (gameRegion == null) {
			return new JSONObject();
		}
		JSONObject object = new JSONObject();
		object.put("region", gameRegion.toJSON());
		object.put("checkpoints", collectionToJSON(checkpoints));
		object.put("walls", collectionToJSON(walls));
		object.put("effectRegions", collectionToJSON(effectRegions));
		object.put("recordsSign", bestRecord == null ? null : locationToJSON(bestRecord));
		object.put("teleportBlock", teleportBlock == null ? null : locationToJSON(teleportBlock));
		object.put("options", options.toJSON());
		return object;
	}

	private JSONArray collectionToJSON(List<? extends AbstractRegion> regions) {
		JSONArray jsonArray = new JSONArray();
		regions.stream()
				.map(AbstractRegion::toJSON)
				.forEach(jsonArray::put);
		return jsonArray;
	}

	private JSONObject locationToJSON(Location location) {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("x", location.getX());
		jsonObject.put("y", location.getY());
		jsonObject.put("z", location.getZ());
		jsonObject.put("world", location.getWorld().getName());
		return jsonObject;
	}

	private Location locationFromJSON(JSONObject jsonObject) {
		if (jsonObject == null) {
			return null;
		}
		return new Location(Bukkit.getWorld(jsonObject.getString("world")),
				jsonObject.getInt(("x")), jsonObject.getInt(("y")), jsonObject.getInt("z"));
	}

	private <E extends AbstractRegion> List<E> collectionFromJSON(JSONArray jsonArray, Class<E> clazz) {
		List<E> list = new ArrayList<>();
		try {
			int length = jsonArray.length();
			for (int i = 0; i < length; i++) {
				JSONObject jsonObject = jsonArray.getJSONObject(i);
				E e = clazz.getConstructor(CuboidRegion.class).newInstance(new CuboidRegion(BlockVector3.ZERO, BlockVector3.ZERO));
				e.fromJSON(jsonObject);
				list.add(e);
			}
		} catch (ReflectiveOperationException ex) {
			Bukkit.getLogger().throwing(getClass().getName(), "collectionFromJSON", ex);
		}
		return list;
	}

	@Override
	public void fromJSON(JSONObject object) {
		gameRegion = new GameRegion(world);
		gameRegion.fromJSON(object.getJSONObject("region"));
		checkpoints.clear();
		checkpoints.addAll(collectionFromJSON(object.getJSONArray("checkpoints"), Checkpoint.class));
		walls.clear();
		walls.addAll(collectionFromJSON(object.getJSONArray("walls"), Wall.class));
		effectRegions.clear();
		effectRegions.addAll(collectionFromJSON(object.getJSONArray("effectRegions"), EffectRegion.class));
		bestRecord = object.has("recordSign") ? locationFromJSON(object.getJSONObject("recordSign")) : null;
		teleportBlock = object.has("teleportBlock") ? locationFromJSON(object.getJSONObject("teleportBlock")) : null;
		options.fromJSON(object.getJSONObject("options"));
	}

	@Override
	public int compareTo(@Nonnull ParkourGame parkour) {
		if (running && !parkour.running) {
			return 1;
		}
		if (!running && parkour.running) {
			return -1;
		}
		if (options.difficulty != parkour.options.difficulty) {
			return options.difficulty - parkour.options.difficulty;
		}
		return name.compareToIgnoreCase(parkour.name);
	}

	public boolean removeAuthor(String string) {
		return authors.remove(string);
	}
}
