package eu.andret.ats.parkour.util;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import eu.andret.ats.parkour.ParkourPlugin;
import eu.andret.ats.parkour.event.game.GameStartEvent;
import eu.andret.ats.parkour.parkour.Parkour;
import eu.andret.ats.parkour.parkour.ParkourGame;
import eu.andret.ats.parkour.region.AbstractRegion;
import eu.andret.ats.parkour.region.DirectionalRegion;
import lombok.AllArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.potion.PotionEffectType;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
public class JSONSerializer {
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

	ParkourPlugin plugin;

	// === READING ===

	public ParkourGame readParkourGame(final String name, final JSONObject jsonObject) {
		final String world = jsonObject.getString(WORLD);
		if (world == null) {
			return null;
		}
		if (!jsonObject.has(REGION)) {
			return null;
		}
		final AbstractRegion gameRegion = readAbstractRegion(jsonObject.getJSONObject(REGION));
		final ParkourGame parkourGame = new Parkour(name, gameRegion, plugin.getServer().getWorld(world));
		if (jsonObject.has(CHECKPOINTS)) {
			readDirectionalRegions(jsonObject.getJSONArray(CHECKPOINTS)).forEach(x -> parkourGame.getCheckpoints().add(x));
		}
		if (jsonObject.has(WALLS)) {
			readAbstractRegions(jsonObject.getJSONArray(WALLS)).forEach(x -> parkourGame.getWalls().add(x));
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
		return parkourGame;
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

	private ParkourGame.Options readOptions(final JSONObject jsonObject1) {
		if (jsonObject1 == null) {
			return ParkourGame.Options.builder().build();
		}
		final ParkourGame.Options parkourOptions = ParkourGame.Options.builder()
				.alwaysSpawn(jsonObject1.getBoolean("alwaysSpawn"))
				.boat(jsonObject1.getBoolean("boat"))
				.recordsCounting(jsonObject1.getBoolean("recordsCounting"))
				.damageAllowed(jsonObject1.getBoolean("damageAllowed"))
				.sprintForced(jsonObject1.getBoolean("sprintForced"))
				.modifyInventory(jsonObject1.getBoolean("modifyInventory"))
				.bronze(jsonObject1.getDouble("bronze"))
				.silver(jsonObject1.getDouble("silver"))
				.gold(jsonObject1.getDouble("gold"))
				.platinum(jsonObject1.getDouble("platinum"))
				.color(DyeColor.valueOf(jsonObject1.getString("color")))
				.difficulty(jsonObject1.getInt("difficulty"))
				.available(jsonObject1.getBoolean("available"))
				.type(ParkourGame.ParkourType.valueOf(jsonObject1.getString("type")))
				.vipOnly(jsonObject1.getBoolean("vipOnly"))
				.enabled(jsonObject1.getBoolean("enabled"))
				.fair(jsonObject1.getDouble("fair"))
				.build();
		final JSONArray effects = jsonObject1.getJSONArray(EFFECTS);
		for (int i = 0; i < effects.length(); i++) {
			final JSONObject jsonObject = effects.getJSONObject(i);
			final String name = jsonObject.getString(NAME);
			final int amplifier = jsonObject.getInt(AMPLIFIER);
			if (amplifier <= 0) {
				continue;
			}
			parkourOptions.setEffect(PotionEffectType.getByName(name), amplifier);
		}
		return parkourOptions;
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

	private List<AbstractRegion> readAbstractRegions(final JSONArray jsonArray) {
		final List<AbstractRegion> list = new ArrayList<>();
		for (int i = 0; i < jsonArray.length(); i++) {
			final JSONObject item = jsonArray.getJSONObject(i);
			final AbstractRegion region = readAbstractRegion(item);
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
		return new CuboidRegion(BlockVector3.at(x1, y1, z1), BlockVector3.at(x2, y2, z2));
	}

	private AbstractRegion readAbstractRegion(final JSONObject jsonObject) {
		if (jsonObject == null) {
			return null;
		}
		return new AbstractRegion(readCuboidRegion(jsonObject));
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
		final String world = jsonObject.getString(WORLD);
		final int x = jsonObject.getInt(X);
		final int y = jsonObject.getInt(Y);
		final int z = jsonObject.getInt(Z);
		return new Location(Bukkit.getWorld(world), x, y, z);
	}

	// ==

	public JSONObject writeParkourGame(final ParkourGame parkourGame) {
		if (parkourGame == null) {
			return null;
		}
		final JSONObject jsonObject = new JSONObject();
		jsonObject.put(WORLD, parkourGame.getWorld().getName());
		jsonObject.put(REGION, writeAbstractRegion(parkourGame.getGameRegion()));
		jsonObject.put(CHECKPOINTS, writeDirectionalRegions(parkourGame.getCheckpoints()));
		jsonObject.put(WALLS, writeAbstractRegions(parkourGame.getWalls()));
		jsonObject.put(OPTIONS, writeOptions(parkourGame.getOptions()));
		jsonObject.put(TELEPORT_BLOCK, writeLocation(parkourGame.getTeleportBlock()));
		jsonObject.put(RECORDS_BLOCK, writeLocation(parkourGame.getRecordsBlock()));
		jsonObject.put(STARTED, parkourGame.isRunning());
		jsonObject.put(AUTHORS, writeStringCollection(parkourGame.getAuthors()));
		jsonObject.put(DISPLAY_NAME, parkourGame.getDisplayName());
		return jsonObject;
	}

	public JSONArray writeStringCollection(final Collection<String> list) {
		final JSONArray jsonArray = new JSONArray();
		if (list != null) {
			list.forEach(jsonArray::put);
		}
		return jsonArray;
	}

	private JSONObject writeCuboidRegion(final CuboidRegion cuboidRegion) {
		if (cuboidRegion == null) {
			return null;
		}
		final JSONObject jsonObject = new JSONObject();
		jsonObject.put("x1", cuboidRegion.getPos1().getX());
		jsonObject.put("y1", cuboidRegion.getPos1().getY());
		jsonObject.put("z1", cuboidRegion.getPos1().getZ());
		jsonObject.put("x2", cuboidRegion.getPos2().getX());
		jsonObject.put("y2", cuboidRegion.getPos2().getY());
		jsonObject.put("z2", cuboidRegion.getPos2().getZ());
		return jsonObject;
	}

	private JSONObject writeAbstractRegion(final AbstractRegion abstractRegion) {
		if (abstractRegion == null) {
			return null;
		}
		return writeCuboidRegion(abstractRegion.getCuboidRegion());
	}

	private JSONObject writeLocation(final Location location) {
		if (location == null || location.getWorld() == null) {
			return null;
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

	private JSONArray writeAbstractRegions(final List<AbstractRegion> list) {
		final JSONArray jsonArray = new JSONArray();
		if (list == null) {
			return jsonArray;
		}
		list.stream().map(this::writeAbstractRegion).forEach(jsonArray::put);
		return jsonArray;
	}

	private JSONObject writeDirectionalRegion(final DirectionalRegion directionalRegion) {
		final JSONObject jsonObject = writeAbstractRegion(directionalRegion);
		if (jsonObject == null) {
			return null;
		}
		jsonObject.put(YAW, directionalRegion.getYaw());
		jsonObject.put(PITCH, directionalRegion.getPitch());
		return jsonObject;
	}

	private JSONObject writeOptions(final ParkourGame.Options options) {
		if (options == null) {
			return null;
		}
		final JSONObject jsonObject1 = new JSONObject();
		jsonObject1.put("alwaysSpawn", options.isAlwaysSpawn());
		jsonObject1.put("boat", options.isBoat());
		jsonObject1.put("recordsCounting", options.isRecordsCounting());
		jsonObject1.put("damageAllowed", options.isDamageAllowed());
		jsonObject1.put("sprintForced", options.isSprintForced());
		jsonObject1.put("modifyInventory", options.isModifyInventory());
		jsonObject1.put("bronze", options.getBronze());
		jsonObject1.put("silver", options.getSilver());
		jsonObject1.put("gold", options.getGold());
		jsonObject1.put("platinum", options.getPlatinum());
		jsonObject1.put("available", options.isAvailable());
		jsonObject1.put("color", options.getColor().name());
		jsonObject1.put("difficulty", options.getDifficulty());
		jsonObject1.put("vipOnly", options.isVipOnly());
		jsonObject1.put("enabled", options.isEnabled());
		jsonObject1.put("fair", options.getFair());
		jsonObject1.put("type", options.getType().name());
		final JSONArray effects = new JSONArray();
		for (final Map.Entry<PotionEffectType, Integer> entry : options.getEffects().entrySet()) {
			final JSONObject jsonObject = new JSONObject();
			jsonObject.put(NAME, entry.getKey().getName());
			jsonObject.put(AMPLIFIER, entry.getValue());
			effects.put(jsonObject);
		}
		jsonObject1.put(EFFECTS, effects);
		return jsonObject1;
	}
}
