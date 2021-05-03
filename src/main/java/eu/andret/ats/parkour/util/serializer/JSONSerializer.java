/*
 * Copyright Andret (c) 2019-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.util.serializer;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import eu.andret.ats.parkour.ParkourPlugin;
import eu.andret.ats.parkour.event.game.GameStartEvent;
import eu.andret.ats.parkour.parkour.ParkourGame;
import eu.andret.ats.parkour.parkour.ParkourManager;
import eu.andret.ats.parkour.parkour.ParkourMedal;
import eu.andret.ats.parkour.parkour.ParkourMedalData;
import eu.andret.ats.parkour.region.BasicRegion;
import eu.andret.ats.parkour.region.DirectionalRegion;
import lombok.AllArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
public class JSONSerializer implements Serializer<JSONObject> {
	private static final String ALWAYS_SPAWN = "alwaysSpawn";
	private static final String AMPLIFIER = "amplifier";
	private static final String AUTHORS = "authors";
	private static final String BOAT = "boat";
	private static final String CHECKPOINTS = "checkpoints";
	private static final String COLOR = "color";
	private static final String DAMAGE_ALLOWED = "damageAllowed";
	private static final String DIFFICULTY = "difficulty";
	private static final String DISPLAY_NAME = "displayName";
	private static final String EFFECTS = "effects";
	private static final String ENABLED = "enabled";
	private static final String FEE = "fee";
	private static final String GAMES = "games";
	private static final String LOBBY = "lobby";
	private static final String MEDALS = "medals";
	private static final String MODIFY_INVENTORY = "modifyInventory";
	private static final String NAME = "name";
	private static final String OPTIONS = "options";
	private static final String PITCH = "pitch";
	private static final String RECORDS_BLOCK = "recordsBlock";
	private static final String REGION = "region";
	private static final String REWARD = "reward";
	private static final String SAVING_RESULTS = "savingResults";
	private static final String SPAWN = "spawn";
	private static final String SPRINT_FORCED = "sprintForced";
	private static final String STARTED = "started";
	private static final String TELEPORT_BLOCK = "teleportBlock";
	private static final String TIME = "time";
	private static final String TYPE = "type";
	private static final String VIP_ONLY = "vipOnly";
	private static final String WALLS = "walls";
	private static final String WORLD = "world";
	private static final String X = "x";
	private static final String Y = "y";
	private static final String YAW = "yaw";
	private static final String Z = "z";

	@NotNull
	ParkourPlugin plugin;

	// === READING ===

	@NotNull
	@Override
	public ParkourManager.ParkourSetting readParkourSetting(@NotNull final JSONObject jsonObject) {
		final ParkourManager.ParkourSetting setting = new ParkourManager.ParkourSetting();
		if (jsonObject.has(LOBBY)) {
			setting.setLobbyLocation(readLocation(jsonObject.getJSONObject(LOBBY)));
		}
		if (jsonObject.has(GAMES)) {
			setting.getParkourGames().addAll(readParkourGames(jsonObject.getJSONArray(GAMES)));
		}
		return setting;
	}

	@NotNull
	private List<ParkourGame> readParkourGames(@NotNull final JSONArray jsonArray) {
		final List<ParkourGame> list = new ArrayList<>();
		for (int i = 0; i < jsonArray.length(); i++) {
			list.add(readParkourGame(jsonArray.getJSONObject(i)));
		}
		return list;
	}

	@Nullable
	private ParkourGame readParkourGame(@NotNull final JSONObject jsonObject) {
		if (!jsonObject.has(WORLD)) {
			return null;
		}
		if (!jsonObject.has(REGION)) {
			return null;
		}
		if (!jsonObject.has(NAME)) {
			return null;
		}
		final String world = jsonObject.getString(WORLD);
		final BasicRegion gameRegion = readBasicRegion(jsonObject.getJSONObject(REGION));
		final ParkourGame parkourGame = plugin.getParkourManager().createParkour(jsonObject.getString(NAME), gameRegion, plugin.getServer().getWorld(world));
		if (jsonObject.has(SPAWN)) {
			parkourGame.setSpawn(readDirectionalRegion(jsonObject.getJSONObject(SPAWN)));
		}
		if (jsonObject.has(CHECKPOINTS)) {
			readDirectionalRegions(jsonObject.getJSONArray(CHECKPOINTS)).forEach(parkourGame.getCheckpoints()::add);
		}
		if (jsonObject.has(WALLS)) {
			readBasicRegions(jsonObject.getJSONArray(WALLS)).forEach(parkourGame.getWalls()::add);
		}
		if (jsonObject.has(OPTIONS)) {
			parkourGame.setOptions(readOptions(jsonObject.getJSONObject(OPTIONS)));
		}
		if (jsonObject.has(TELEPORT_BLOCK)) {
			parkourGame.setTeleportBlock(readLocation(jsonObject.getJSONObject(TELEPORT_BLOCK)));
		}
		if (jsonObject.has(RECORDS_BLOCK)) {
			parkourGame.setRecordsBlock(readLocation(jsonObject.getJSONObject(RECORDS_BLOCK)));
		}
		if (jsonObject.has(STARTED) && jsonObject.getBoolean(STARTED)) {
			parkourGame.setRunning(true);
			plugin.getServer().getPluginManager().callEvent(new GameStartEvent(parkourGame));
		}
		if (jsonObject.has(AUTHORS)) {
			parkourGame.getAuthors().addAll(readStringList(jsonObject.getJSONArray(AUTHORS)));
		}
		if (jsonObject.has(DISPLAY_NAME)) {
			parkourGame.setDisplayName(jsonObject.getString(DISPLAY_NAME));
		}
		if (jsonObject.has(EFFECTS)) {
			readEffects(jsonObject.getJSONArray(EFFECTS), parkourGame);
		}
		if (jsonObject.has(MEDALS)) {
			parkourGame.getMedals().putAll(readMedals(jsonObject.getJSONObject(MEDALS)));
		}
		return parkourGame;
	}

	private void readEffects(@NotNull final JSONArray effects, @NotNull final ParkourGame parkourGame) {
		for (int i = 0; i < effects.length(); i++) {
			final JSONObject effect = effects.getJSONObject(i);
			final String name = effect.getString(NAME);
			final int amplifier = effect.getInt(AMPLIFIER);
			if (amplifier <= 0) {
				continue;
			}
			parkourGame.getEffects().put(PotionEffectType.getByName(name), amplifier);
		}
	}

	@NotNull
	private Map<ParkourMedal, ParkourMedalData> readMedals(@NotNull final JSONObject object) {
		final Map<ParkourMedal, ParkourMedalData> result = new HashMap<>();
		plugin.getMedals()
				.stream()
				.filter(medal -> object.has(medal.getName()))
				.forEach(medal -> {
					final JSONObject data = object.getJSONObject(medal.getName());
					result.put(medal, new ParkourMedalData(data.getDouble(TIME), data.getDouble(REWARD)));
				});
		return result;
	}

	@NotNull
	private List<String> readStringList(@NotNull final JSONArray jsonArray) {
		final List<String> list = new ArrayList<>();
		for (int i = 0; i < jsonArray.length(); i++) {
			list.add(jsonArray.getString(i));
		}
		return list;
	}

	@NotNull
	private ParkourGame.Options readOptions(@NotNull final JSONObject jsonObject) {
		return ParkourGame.Options.builder()
				.alwaysSpawn(jsonObject.getBoolean(ALWAYS_SPAWN))
				.boat(jsonObject.getBoolean(BOAT))
				.savingResults(jsonObject.getBoolean(SAVING_RESULTS))
				.damageAllowed(jsonObject.getBoolean(DAMAGE_ALLOWED))
				.sprintForced(jsonObject.getBoolean(SPRINT_FORCED))
				.modifyInventory(jsonObject.getBoolean(MODIFY_INVENTORY))
				.color(DyeColor.valueOf(jsonObject.getString(COLOR)))
				.difficulty(jsonObject.getInt(DIFFICULTY))
				.type(ParkourGame.Type.valueOf(jsonObject.getString(TYPE)))
				.reward(jsonObject.getDouble(REWARD))
				.vipOnly(jsonObject.getBoolean(VIP_ONLY))
				.enabled(jsonObject.getBoolean(ENABLED))
				.fee(jsonObject.getDouble(FEE))
				.build();
	}

	@NotNull
	private List<DirectionalRegion> readDirectionalRegions(@NotNull final JSONArray jsonArray) {
		final List<DirectionalRegion> list = new ArrayList<>();
		for (int i = 0; i < jsonArray.length(); i++) {
			list.add(readDirectionalRegion(jsonArray.getJSONObject(i)));
		}
		return list;
	}

	@NotNull
	private List<BasicRegion> readBasicRegions(@NotNull final JSONArray jsonArray) {
		final List<BasicRegion> list = new ArrayList<>();
		for (int i = 0; i < jsonArray.length(); i++) {
			list.add(readBasicRegion(jsonArray.getJSONObject(i)));
		}
		return list;
	}

	@NotNull
	private CuboidRegion readCuboidRegion(@NotNull final JSONObject jsonObject) {
		final double x1 = jsonObject.getDouble("x1");
		final double y1 = jsonObject.getDouble("y1");
		final double z1 = jsonObject.getDouble("z1");
		final double x2 = jsonObject.getDouble("x2");
		final double y2 = jsonObject.getDouble("y2");
		final double z2 = jsonObject.getDouble("z2");
		final String world = jsonObject.getString(WORLD);
		final World serverWorld = plugin.getServer().getWorld(world);
		if (serverWorld == null) {
			throw new UnsupportedOperationException("Tried to load world \"" + world + "\", but it doesn't exist!");
		}
		return new CuboidRegion(BukkitAdapter.adapt(serverWorld), BlockVector3.at(x1, y1, z1), BlockVector3.at(x2, y2, z2));
	}

	@NotNull
	private BasicRegion readBasicRegion(@NotNull final JSONObject jsonObject) {
		return new BasicRegion(readCuboidRegion(jsonObject));
	}

	@NotNull
	private DirectionalRegion readDirectionalRegion(@NotNull final JSONObject jsonObject) {
		final CuboidRegion cuboidRegion = readCuboidRegion(jsonObject);
		final double yaw = jsonObject.has(YAW) ? jsonObject.getDouble(YAW) : 0;
		final double pitch = jsonObject.has(PITCH) ? jsonObject.getDouble(PITCH) : 0;
		return new DirectionalRegion(cuboidRegion, yaw, pitch);
	}

	@Nullable
	private Location readLocation(@NotNull final JSONObject jsonObject) {
		if (!jsonObject.has(WORLD) || !jsonObject.has(X) || !jsonObject.has(Y) || !jsonObject.has(Z)) {
			return null;
		}
		final double x = jsonObject.getDouble(X);
		final double y = jsonObject.getDouble(Y);
		final double z = jsonObject.getDouble(Z);
		final double yaw = jsonObject.has(YAW) ? jsonObject.getDouble(YAW) : 0;
		final double pitch = jsonObject.has(PITCH) ? jsonObject.getDouble(PITCH) : 0;
		return new Location(Bukkit.getWorld(jsonObject.getString(WORLD)), x, y, z, (float) yaw, (float) pitch);
	}

	// === WRITING ===

	@NotNull
	@Override
	public JSONObject writeParkourSetting(@NotNull final ParkourManager.ParkourSetting setting) {
		return new JSONObject()
				.put(LOBBY, writeLocation(setting.getLobbyLocation()))
				.put(GAMES, writeParkourGames(setting.getParkourGames()));
	}

	@NotNull
	private JSONArray writeParkourGames(@NotNull final List<ParkourGame> list) {
		return list.stream()
				.map(this::writeParkourGame)
				.collect(JSONArray::new, JSONArray::put, JSONArray::putAll);
	}

	@NotNull
	private JSONObject writeParkourGame(@NotNull final ParkourGame parkourGame) {
		return new JSONObject()
				.put(NAME, parkourGame.getName())
				.put(WORLD, parkourGame.getWorld().getName())
				.put(REGION, writeBasicRegion(parkourGame.getRegion()))
				.put(SPAWN, writeDirectionalRegion(parkourGame.getSpawn()))
				.put(CHECKPOINTS, writeDirectionalRegions(parkourGame.getCheckpoints()))
				.put(WALLS, writeBasicRegions(parkourGame.getWalls()))
				.put(OPTIONS, writeOptions(parkourGame.getOptions()))
				.put(TELEPORT_BLOCK, writeLocation(parkourGame.getTeleportBlock()))
				.put(RECORDS_BLOCK, writeLocation(parkourGame.getRecordsBlock()))
				.put(STARTED, parkourGame.isRunning())
				.put(AUTHORS, writeStringCollection(parkourGame.getAuthors()))
				.put(DISPLAY_NAME, parkourGame.getDisplayName())
				.put(EFFECTS, writeEffects(parkourGame))
				.put(MEDALS, writeMedals(parkourGame.getMedals()));
	}

	@NotNull
	private JSONArray writeEffects(@NotNull final ParkourGame parkourGame) {
		return parkourGame.getEffects()
				.entrySet()
				.stream()
				.map(entry -> new JSONObject()
						.put(NAME, entry.getKey().getName())
						.put(AMPLIFIER, entry.getValue()))
				.collect(JSONArray::new, JSONArray::put, JSONArray::putAll);
	}

	@NotNull
	private JSONObject writeMedals(@NotNull final Map<ParkourMedal, ParkourMedalData> medals) {
		final JSONObject result = new JSONObject();
		medals.forEach((medal, parkourMedalData) -> result.put(medal.getName(), new JSONObject()
				.put(TIME, parkourMedalData.getTime())
				.put(REWARD, parkourMedalData.getReward())));
		return result;
	}

	@NotNull
	private JSONArray writeStringCollection(@NotNull final Collection<String> list) {
		return list.stream().collect(JSONArray::new, JSONArray::put, JSONArray::putAll);
	}

	@NotNull
	private JSONObject writeCuboidRegion(@NotNull final CuboidRegion cuboidRegion) {
		if (cuboidRegion.getWorld() == null) {
			throw new UnsupportedOperationException("Cuboid region has no world! " + cuboidRegion);
		}
		return new JSONObject()
				.put("x1", cuboidRegion.getPos1().getX())
				.put("y1", cuboidRegion.getPos1().getY())
				.put("z1", cuboidRegion.getPos1().getZ())
				.put("x2", cuboidRegion.getPos2().getX())
				.put("y2", cuboidRegion.getPos2().getY())
				.put("z2", cuboidRegion.getPos2().getZ())
				.put(WORLD, cuboidRegion.getWorld().getName());
	}

	@NotNull
	private JSONObject writeBasicRegion(@NotNull final BasicRegion basicRegion) {
		return writeCuboidRegion(basicRegion.getRegion());
	}

	@Nullable
	private JSONObject writeLocation(@Nullable final Location location) {
		if (location == null || location.getWorld() == null) {
			return null;
		}
		return new JSONObject()
				.put(WORLD, location.getWorld().getName())
				.put(X, location.getX())
				.put(Y, location.getY())
				.put(Z, location.getZ())
				.put(YAW, location.getYaw())
				.put(PITCH, location.getPitch());
	}

	@NotNull
	private JSONArray writeDirectionalRegions(@NotNull final List<DirectionalRegion> list) {
		return list.stream()
				.map(this::writeDirectionalRegion)
				.collect(JSONArray::new, JSONArray::put, JSONArray::putAll);
	}

	@NotNull
	private JSONArray writeBasicRegions(@NotNull final List<BasicRegion> list) {
		return list.stream()
				.map(this::writeBasicRegion)
				.collect(JSONArray::new, JSONArray::put, JSONArray::putAll);
	}

	@NotNull
	private JSONObject writeDirectionalRegion(@Nullable final DirectionalRegion directionalRegion) {
		if (directionalRegion == null) {
			return new JSONObject();
		}
		return writeBasicRegion(directionalRegion)
				.put(YAW, directionalRegion.getYaw())
				.put(PITCH, directionalRegion.getPitch());
	}

	@NotNull
	private JSONObject writeOptions(@NotNull final ParkourGame.Options options) {
		return new JSONObject()
				.put(ALWAYS_SPAWN, options.isAlwaysSpawn())
				.put(BOAT, options.isBoat())
				.put(COLOR, options.getColor().name())
				.put(DAMAGE_ALLOWED, options.isDamageAllowed())
				.put(DIFFICULTY, options.getDifficulty())
				.put(ENABLED, options.isEnabled())
				.put(FEE, options.getFee())
				.put(MODIFY_INVENTORY, options.isModifyInventory())
				.put(REWARD, options.getReward())
				.put(SAVING_RESULTS, options.isSavingResults())
				.put(SPRINT_FORCED, options.isSprintForced())
				.put(TYPE, options.getType().name())
				.put(VIP_ONLY, options.isVipOnly());
	}
}
