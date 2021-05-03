/*
 *  Copyright Andret (c) 2018-2021. Copying and modifying allowed only keeping git link reference.
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
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
public class JSONSerializer implements Serializer<JSONObject> {
	private static final String NAME = "name";
	private static final String WORLD = "world";
	private static final String STARTED = "started";
	private static final String AUTHORS = "authors";
	private static final String DISPLAY_NAME = "displayName";
	private static final String REGION = "region";
	private static final String CHECKPOINTS = "checkpoints";
	private static final String WALLS = "walls";
	private static final String RECORDS_BLOCK = "recordsBlock";
	private static final String TELEPORT_BLOCK = "teleportBlock";
	private static final String OPTIONS = "options";
	private static final String EFFECTS = "effects";
	private static final String AMPLIFIER = "amplifier";
	private static final String X = "x";
	private static final String Y = "y";
	private static final String Z = "z";
	private static final String YAW = "yaw";
	private static final String PITCH = "pitch";
	private static final String SPAWN = "spawn";
	private static final String LOBBY = "lobby";
	private static final String GAMES = "games";
	private static final String MEDALS = "medals";
	private static final String TIME = "time";
	private static final String REWARD = "reward";

	ParkourPlugin plugin;

	// === READING ===

	@Override
	public ParkourManager.ParkourSetting readParkourSetting(final JSONObject jsonObject) {
		final ParkourManager.ParkourSetting setting = new ParkourManager.ParkourSetting();
		if (jsonObject.has(LOBBY)) {
			setting.setLobbyLocation(readLocation(jsonObject.getJSONObject(LOBBY)));
		}
		if (jsonObject.has(GAMES)) {
			setting.getParkourGames().addAll(readParkourGames(jsonObject.getJSONArray(GAMES)));
		}
		return setting;
	}

	private List<ParkourGame> readParkourGames(final JSONArray jsonArray) {
		final List<ParkourGame> list = new ArrayList<>();
		for (int i = 0; i < jsonArray.length(); i++) {
			list.add(readParkourGame(jsonArray.getJSONObject(i)));
		}
		return list;
	}

	private ParkourGame readParkourGame(final JSONObject jsonObject) {
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
			final JSONArray effects = jsonObject.getJSONArray(EFFECTS);
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
		if (jsonObject.has(MEDALS)) {
			parkourGame.getMedals().putAll(readMedals(jsonObject.getJSONObject(MEDALS)));
		}
		return parkourGame;
	}

	private Map<ParkourMedal, ParkourMedalData> readMedals(final JSONObject object) {
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

	private List<String> readStringList(final JSONArray jsonArray) {
		if (jsonArray == null) {
			return Collections.emptyList();
		}
		final List<String> list = new ArrayList<>();
		for (int i = 0; i < jsonArray.length(); i++) {
			list.add(jsonArray.getString(i));
		}
		return list;
	}

	private ParkourGame.Options readOptions(final JSONObject jsonObject) {
		if (jsonObject == null) {
			return ParkourGame.Options.builder().build();
		}
		return ParkourGame.Options.builder()
				.alwaysSpawn(jsonObject.getBoolean("alwaysSpawn"))
				.boat(jsonObject.getBoolean("boat"))
				.savingResults(jsonObject.getBoolean("savingResults"))
				.damageAllowed(jsonObject.getBoolean("damageAllowed"))
				.sprintForced(jsonObject.getBoolean("sprintForced"))
				.modifyInventory(jsonObject.getBoolean("modifyInventory"))
				.color(DyeColor.valueOf(jsonObject.getString("color")))
				.difficulty(jsonObject.getInt("difficulty"))
				.type(ParkourGame.Type.valueOf(jsonObject.getString("type")))
				.vipOnly(jsonObject.getBoolean("vipOnly"))
				.enabled(jsonObject.getBoolean("enabled"))
				.fee(jsonObject.getDouble("fee"))
				.build();
	}

	private List<DirectionalRegion> readDirectionalRegions(final JSONArray jsonArray) {
		final List<DirectionalRegion> list = new ArrayList<>();
		for (int i = 0; i < jsonArray.length(); i++) {
			final JSONObject item = jsonArray.getJSONObject(i);
			final DirectionalRegion region = readDirectionalRegion(item);
			list.add(region);
		}
		return list;
	}

	private List<BasicRegion> readBasicRegions(final JSONArray jsonArray) {
		final List<BasicRegion> list = new ArrayList<>();
		for (int i = 0; i < jsonArray.length(); i++) {
			final JSONObject item = jsonArray.getJSONObject(i);
			final BasicRegion region = readBasicRegion(item);
			list.add(region);
		}
		return list;
	}

	private CuboidRegion readCuboidRegion(final JSONObject jsonObject) {
		if (jsonObject == null) {
			return null;
		}
		final double x1 = jsonObject.getDouble("x1");
		final double y1 = jsonObject.getDouble("y1");
		final double z1 = jsonObject.getDouble("z1");
		final double x2 = jsonObject.getDouble("x2");
		final double y2 = jsonObject.getDouble("y2");
		final double z2 = jsonObject.getDouble("z2");
		final String world = jsonObject.getString(WORLD);
		final World serverWorld = plugin.getServer().getWorld(world);
		if (serverWorld == null) {
			throw new UnsupportedOperationException("The world tried to load (" + world + ") doesn't exist!");
		}
		return new CuboidRegion(BukkitAdapter.adapt(serverWorld), BlockVector3.at(x1, y1, z1), BlockVector3.at(x2, y2, z2));
	}

	private BasicRegion readBasicRegion(final JSONObject jsonObject) {
		if (jsonObject == null) {
			return null;
		}
		return new BasicRegion(readCuboidRegion(jsonObject));
	}

	private DirectionalRegion readDirectionalRegion(final JSONObject jsonObject) {
		if (jsonObject == null) {
			return null;
		}
		final CuboidRegion cuboidRegion = readCuboidRegion(jsonObject);
		final double yaw = jsonObject.getDouble(YAW);
		final double pitch = jsonObject.getDouble(PITCH);
		return new DirectionalRegion(cuboidRegion, yaw, pitch);
	}

	private Location readLocation(final JSONObject jsonObject) {
		if (jsonObject == null) {
			return null;
		}
		if (!jsonObject.has(WORLD)) {
			return null;
		}
		final String world = jsonObject.getString(WORLD);
		final int x = jsonObject.getInt(X);
		final int y = jsonObject.getInt(Y);
		final int z = jsonObject.getInt(Z);
		return new Location(Bukkit.getWorld(world), x, y, z);
	}

	// === WRITING ===

	@Override
	public JSONObject writeParkourSetting(final ParkourManager.ParkourSetting setting) {
		final JSONObject result = new JSONObject();
		result.put(LOBBY, writeLocation(setting.getLobbyLocation()));
		result.put(GAMES, writeParkourGames(setting.getParkourGames()));
		return result;
	}

	private JSONArray writeParkourGames(final List<ParkourGame> list) {
		final JSONArray jsonArray = new JSONArray();
		list.stream().map(this::writeParkourGame).forEach(jsonArray::put);
		return jsonArray;
	}

	private JSONObject writeParkourGame(final ParkourGame parkourGame) {
		if (parkourGame == null) {
			return null;
		}
		final JSONObject jsonObject = new JSONObject();
		jsonObject.put(NAME, parkourGame.getName());
		jsonObject.put(WORLD, parkourGame.getWorld().getName());
		jsonObject.put(REGION, writeBasicRegion(parkourGame.getRegion()));
		jsonObject.put(SPAWN, writeDirectionalRegion(parkourGame.getSpawn()));
		jsonObject.put(CHECKPOINTS, writeDirectionalRegions(parkourGame.getCheckpoints()));
		jsonObject.put(WALLS, writeBasicRegions(parkourGame.getWalls()));
		jsonObject.put(OPTIONS, writeOptions(parkourGame.getOptions()));
		jsonObject.put(TELEPORT_BLOCK, writeLocation(parkourGame.getTeleportBlock()));
		jsonObject.put(RECORDS_BLOCK, writeLocation(parkourGame.getRecordsBlock()));
		jsonObject.put(STARTED, parkourGame.isRunning());
		jsonObject.put(AUTHORS, writeStringCollection(parkourGame.getAuthors()));
		jsonObject.put(DISPLAY_NAME, parkourGame.getDisplayName());
		final JSONArray effects = new JSONArray();
		for (final Map.Entry<PotionEffectType, Integer> entry : parkourGame.getEffects().entrySet()) {
			final JSONObject effect = new JSONObject();
			effect.put(NAME, entry.getKey().getName());
			effect.put(AMPLIFIER, entry.getValue());
			effects.put(effect);
		}
		jsonObject.put(EFFECTS, effects);
		jsonObject.put(MEDALS, writeMedals(parkourGame.getMedals()));
		return jsonObject;
	}

	private JSONObject writeMedals(final Map<ParkourMedal, ParkourMedalData> medals) {
		final JSONObject result = new JSONObject();
		medals.forEach((medal, parkourMedalData) -> {
			final JSONObject data = new JSONObject();
			data.put(TIME, parkourMedalData.getTime());
			data.put(REWARD, parkourMedalData.getReward());
			result.put(medal.getName(), data);
		});
		return result;
	}

	private JSONArray writeStringCollection(final Collection<String> list) {
		final JSONArray jsonArray = new JSONArray();
		if (list != null) {
			list.forEach(jsonArray::put);
		}
		return jsonArray;
	}

	private JSONObject writeCuboidRegion(final CuboidRegion cuboidRegion) {
		if (cuboidRegion == null || cuboidRegion.getWorld() == null) {
			return null;
		}
		final JSONObject jsonObject = new JSONObject();
		jsonObject.put("x1", cuboidRegion.getPos1().getX());
		jsonObject.put("y1", cuboidRegion.getPos1().getY());
		jsonObject.put("z1", cuboidRegion.getPos1().getZ());
		jsonObject.put("x2", cuboidRegion.getPos2().getX());
		jsonObject.put("y2", cuboidRegion.getPos2().getY());
		jsonObject.put("z2", cuboidRegion.getPos2().getZ());
		jsonObject.put(WORLD, cuboidRegion.getWorld().getName());
		return jsonObject;
	}

	private JSONObject writeBasicRegion(final BasicRegion basicRegion) {
		if (basicRegion == null) {
			return null;
		}
		return writeCuboidRegion(basicRegion.getRegion());
	}

	private JSONObject writeLocation(final Location location) {
		if (location == null || location.getWorld() == null) {
			return new JSONObject();
		}
		final JSONObject jsonObject = new JSONObject();
		jsonObject.put(WORLD, location.getWorld().getName());
		jsonObject.put(X, location.getX());
		jsonObject.put(Y, location.getY());
		jsonObject.put(Z, location.getZ());
		return jsonObject;
	}

	private JSONArray writeDirectionalRegions(final List<DirectionalRegion> list) {
		final JSONArray jsonArray = new JSONArray();
		if (list == null) {
			return jsonArray;
		}
		list.stream().map(this::writeDirectionalRegion).forEach(jsonArray::put);
		return jsonArray;
	}

	private JSONArray writeBasicRegions(final List<BasicRegion> list) {
		final JSONArray jsonArray = new JSONArray();
		if (list == null) {
			return jsonArray;
		}
		list.stream().map(this::writeBasicRegion).forEach(jsonArray::put);
		return jsonArray;
	}

	private JSONObject writeDirectionalRegion(final DirectionalRegion directionalRegion) {
		final JSONObject jsonObject = writeBasicRegion(directionalRegion);
		if (jsonObject == null) {
			return new JSONObject();
		}
		jsonObject.put(YAW, directionalRegion.getYaw());
		jsonObject.put(PITCH, directionalRegion.getPitch());
		return jsonObject;
	}

	private JSONObject writeOptions(final ParkourGame.Options options) {
		if (options == null) {
			return new JSONObject();
		}
		final JSONObject jsonObject1 = new JSONObject();
		jsonObject1.put("alwaysSpawn", options.isAlwaysSpawn());
		jsonObject1.put("boat", options.isBoat());
		jsonObject1.put("savingResults", options.isSavingResults());
		jsonObject1.put("damageAllowed", options.isDamageAllowed());
		jsonObject1.put("sprintForced", options.isSprintForced());
		jsonObject1.put("modifyInventory", options.isModifyInventory());
		jsonObject1.put("color", options.getColor().name());
		jsonObject1.put("difficulty", options.getDifficulty());
		jsonObject1.put("vipOnly", options.isVipOnly());
		jsonObject1.put("enabled", options.isEnabled());
		jsonObject1.put("type", options.getType().name());
		jsonObject1.put("fee", options.getFee());
		return jsonObject1;
	}
}
