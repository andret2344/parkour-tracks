/*
 * Copyright Andret (c) 2019-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import eu.andret.arguments.AnnotatedCommand;
import eu.andret.arguments.CommandManager;
import eu.andret.arguments.api.annotation.Fallback;
import eu.andret.ats.parkour.parkour.ParkourGame;
import eu.andret.ats.parkour.parkour.ParkourManager;
import eu.andret.ats.parkour.parkour.ParkourRecord;
import eu.andret.ats.parkour.tasks.database.KeepAliveTask;
import eu.andret.ats.parkour.util.Data;
import eu.andret.ats.parkour.util.JSONSerializer;
import lombok.Getter;
import org.bstats.bukkit.Metrics;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ParkourPlugin extends JavaPlugin {
	private Connection connection;
	@Getter
	private ItemStack exit;
	@Getter
	private final ParkourManager parkourManager = new ParkourManager();
	@Getter
	private final Map<String, String> messages = new LinkedHashMap<>();
	private final YamlConfiguration yamlConfiguration = new YamlConfiguration();
	private final Map<UUID, Integer> teleportCount = new HashMap<>();
	private final Map<UUID, Integer> playerTimeCounters = new HashMap<>();

	private final JSONSerializer jsonSerializer = new JSONSerializer(this);

	@Override
	public void onEnable() {
		if (getWorldEdit() == null) {
			System.out.println("[atsParkour] CRITICAL! Cannot find WorldEdit plugin! Disabling...");
			setEnabled(false);
			return;
		}
		setupConfigFiles();
		createDoors();
		getServer().getPluginManager().registerEvents(new ParkourListeners(this), this);
		setupCommand();
		setupDatabase();
		loadParkourLobby();
		loadAllParkourGames();
		parkourManager.getAllGames().stream()
				.filter(Objects::nonNull)
				.forEach(p -> getServer().getOnlinePlayers().stream()
						.filter(Objects::nonNull)
						.filter(pl -> p.getAllRegions().stream().filter(Objects::nonNull).anyMatch(r -> r.contains(pl.getLocation())))
						.forEach(p::addPlayer));
		getConnection()
				.map(KeepAliveTask::new)
				.ifPresent(keepAliveTask -> getServer().getScheduler().scheduleSyncRepeatingTask(this, keepAliveTask, 20_000, 20_000));

		new Metrics(this, 10700);
	}

	@Override
	public void onDisable() {
		saveParkourLobby();
		saveAllGames();
		getServer().getScheduler().cancelTasks(this);
	}

	public String msg(final String path, final boolean isError) {
		final String here;
		if (yamlConfiguration.getString("player." + path) != null) {
			here = "player.";
		} else if (yamlConfiguration.getString("admin." + path) != null) {
			here = "admin.";
		} else {
			throw new NullPointerException("Invalid message: " + path + " (should be error: " + isError + ")");
		}
		String result = "";
		if (isError) {
			result += yamlConfiguration.getString(here + "errorMsg");
		}
		return (result + yamlConfiguration.getString(here + path)).replace('&', '\u00A7');
	}

	public Optional<Connection> getConnection() {
		return Optional.ofNullable(connection);
	}

	public WorldEditPlugin getWorldEdit() {
		return (WorldEditPlugin) getServer().getPluginManager().getPlugin("WorldEdit");
	}

	public void updateSign(final ParkourRecord parkourRecord) {
		Optional.of(parkourRecord.getParkourGame())
				.map(ParkourGame::getRecordsBlock)
				.map(Location::getBlock)
				.map(Block::getState)
				.filter(x -> x instanceof Sign)
				.map(Sign.class::cast)
				.ifPresent(sign -> {
					IntStream.of(0, 1, 2, 3).forEach(x -> {
						final String lineText = getConfig().getString("recordSign.line" + (x + 1));
						sign.setLine(x, replace(String.valueOf(lineText), parkourRecord).replace('&', '\u00A7'));
					});
					sign.update();
				});
	}

	public Map<UUID, Integer> getTeleportCount() {
		return teleportCount;
	}

	public Map<UUID, Integer> getPlayerTimeCounters() {
		return playerTimeCounters;
	}

	private void setupConfigFiles() {
		saveDefaultConfig();
		saveResource("scoreboard.yml", false);
		saveResource("messages.yml", false);
		try {
			yamlConfiguration.load(new File(getDataFolder(), "messages.yml"));
		} catch (final IOException | InvalidConfigurationException ex) {
			System.out.println("[atsParkour] An error occurred when loading messages");
			ex.printStackTrace();
		}
		generate();
	}

	private void createDoors() {
		exit = new ItemStack(Material.IRON_DOOR);
		Optional.ofNullable(exit.getItemMeta())
				.ifPresent(meta -> {
					meta.setDisplayName("§r" + ChatColor.translateAlternateColorCodes('&', String.valueOf(getConfig().getString("door.name"))));
					exit.setItemMeta(meta);
				});
	}

	private void setupCommand() {
		final AnnotatedCommand command = CommandManager.registerCommand(ParkourCommand.class, this);
		command.setOnInsufficientPermissionsListener(sender -> sender.sendMessage(msg("noPerms", true)));
		command.setOnUnknownSubCommandExecutionListener(sender -> sender.sendMessage(msg("wrongArg", true)));
		command.addArgumentMapper("parkourGame", ParkourGame.class, parkourManager::getParkour, Fallback.ON_NULL);
		command.addArgumentMapper("potion", PotionEffectType.class, PotionEffectType::getByName, Fallback.ON_NULL);
		command.addTypeCompleter(ParkourGame.class, () -> parkourManager.getAllGames().stream()
				.map(ParkourGame::getName)
				.collect(Collectors.toList()));
		command.addTypeCompleter(boolean.class, Arrays.asList("false", "true"));
		command.addTypeCompleter(PotionEffectType.class, () -> Data.ALLOWED_EFFECTS.stream()
				.map(PotionEffectType::getName)
				.collect(Collectors.toList()));
	}

	private void setupDatabase() {
		final boolean databaseEnabled = getConfig().getBoolean("database.enabled", false);
		if (!databaseEnabled) {
			System.out.println("[atsParkour] Database is disabled. In order to save records, enable it in config.");
			return;
		}
		try {
			connection = createConnection();
			connect();
			System.out.println("[atsParkour] Database connection established.");
		} catch (final SQLException ex) {
			System.out.println("[atsParkour] An error occurred when trying to connect to database.");
			connection = null;
			ex.printStackTrace();
		}
	}

	private Connection createConnection() throws SQLException {
		final String url = getConfig().getString("database.url");
		final String user = getConfig().getString("database.user");
		final String pass = getConfig().getString("database.pass");
		return DriverManager.getConnection("jdbc:mysql://" + url + "?autoReconnect=true&useSSL=false", user, pass);
	}

	private void connect() throws SQLException {
		final String database = getConfig().getString("database.dbname");
		try (final Statement stat = connection.createStatement()) {
			stat.execute("CREATE DATABASE IF NOT EXISTS `" + database + "`;");
			stat.execute("USE " + database + ";");
			stat.execute("CREATE TABLE IF NOT EXISTS ats_parkour_records(id INT PRIMARY KEY AUTO_INCREMENT, date DATETIME, nick VARCHAR(64), parkour VARCHAR(64), time FLOAT);");
		}
	}

	private void saveParkourLobby() {
		try {
			final File lobby = new File(getDataFolder(), "lobby.json");
			if (!lobby.exists() && !lobby.createNewFile()) {
				System.out.println("[atsParkour] An error occurred when trying to create lobby file");
				return;
			}
			final PrintWriter printWriter = new PrintWriter(lobby);
			printWriter.write(jsonSerializer.writeLocation(parkourManager.getLobbyLocation()).toString(4));
			printWriter.close();
			System.out.println("[atsParkour] Successfully saved lobby");
		} catch (final IOException ex) {
			System.out.println("[atsParkour] An error occurred when trying to save lobby");
			ex.printStackTrace();
		}
	}

	private void saveAllGames() {
		try {
			final File games = new File(getDataFolder(), "games.json");
			if (!games.exists() && !games.createNewFile()) {
				System.out.println("[atsParkour] An error occurred when trying to create games file");
			}
			final PrintWriter printWriter = new PrintWriter(games);
			printWriter.write(jsonSerializer.writeParkourGames(parkourManager.getAllGames()).toString(4));
			printWriter.close();
			System.out.println("[atsParkour] Successfully saved " + parkourManager.getAllGames() + " games");
		} catch (final IOException ex) {
			System.out.println("[atsParkour] An error occurred when trying to save games");
			ex.printStackTrace();
		}
	}

	private void loadParkourLobby() {
		final File lobby = new File(getDataFolder(), "lobby.json");
		if (!lobby.exists()) {
			return;
		}
		try (final Reader reader = new FileReader(lobby)) {
			final JSONTokener jsonTokener = new JSONTokener(reader);
			final JSONObject jsonObject = new JSONObject(jsonTokener);
			parkourManager.setLobbyLocation(jsonSerializer.readLocation(jsonObject));
			System.out.println("[atsParkour] Successfully loaded lobby");
		} catch (final IOException ex) {
			System.out.println("[atsParkour] An error occurred when trying to load lobby");
			ex.printStackTrace();
		}
	}

	private void loadAllParkourGames() {
		final File games = new File(getDataFolder(), "games.json");
		if (!games.exists()) {
			System.out.println("[atsParkour] No games.json file found");
			return;
		}
		try (final Reader reader = new FileReader(games)) {
			final JSONTokener jsonTokener = new JSONTokener(reader);
			final JSONArray jsonArray = new JSONArray(jsonTokener);
			jsonSerializer.readParkourGames(jsonArray).forEach(parkourManager::addParkour);
			System.out.printf("[atsParkour] Successfully loaded %d parkour games", parkourManager.getAllGames().size());
		} catch (final IOException ex) {
			System.out.println("[atsParkour] An error occurred when trying to load parkour");
			ex.printStackTrace();
		}
	}

	private void generate() {
		messages.put("help|?", msg("cmdHelp", false));
		messages.put("lobby", msg("cmdLobby", false));
		messages.put("create|c", msg("cmdCreate", false));
		messages.put("remove|r", msg("cmdRemove", false));
		messages.put("info", msg("cmdInfo", false));
		messages.put("setSpawn|ss", msg("cmdSetSpawn", false));
		messages.put("recreate|rc", msg("cmdRecreate", false));
		messages.put("start|s", msg("cmdStart", false));
		messages.put("stop", msg("cmdStop", false));
		messages.put("addCheckpoint|ac", msg("cmdAddCheckpoint", false));
		messages.put("setCheckpoint|sc", msg("cmdSetCheckpoint", false));
		messages.put("addWall|aw", msg("cmdAddWall", false));
		messages.put("setWall|sw", msg("cmdSetWall", false));
		messages.put("list|ls", msg("cmdList", false));
		messages.put("ignore|i", msg("cmdIgnore", false));
		messages.put("reload|rl", msg("cmdReload", false));
		messages.put("sprintForced|sp", msg("cmdSprintForced", false));
		messages.put("alwaysSpawn|as", msg("cmdAlwaysSpawn", false));
		messages.put("recordCounting|cr", msg("cmdRecordCounting", false));
		messages.put("damageAllowed|dmg", msg("cmdDamage", false));
		messages.put("effect|e", msg("cmdEffect", false));
		messages.put("boat|b", msg("cmdBoats", false));
		messages.put("enabled", msg("cmdEnabled", false));
		messages.put("modifyInventory|eq", msg("cmdModifyInventory", false));
		messages.put("recordsBlock|rb", msg("cmdRecordsBlock", false));
		messages.put("teleportBlock|tb", msg("cmdTeleportBlock", false));
		messages.put("teleport|tp", msg("cmdTeleport", false));
		messages.put("fix", msg("cmdFix", false));
		messages.put("color", msg("cmdColor", false));
		messages.put("difficulty|d", msg("cmdDifficulty", false));
		messages.put("available|a", msg("cmdAvailable", false));
		messages.put("type", msg("cmdType", false));
		messages.put("displayName|dn", msg("cmdDisplayName", false));
		messages.put("authors", msg("cmdAuthors", false));
		messages.put("vip", msg("cmdVip", false));
		messages.put("bronze", msg("cmdBronze", false));
		messages.put("silver", msg("cmdSilver", false));
		messages.put("gold", msg("cmdGold", false));
		messages.put("platinum", msg("cmdPlatinum", false));
	}

	private String replace(final String source, final ParkourRecord parkourRecord) {
		final float time = parkourRecord.getTime();
		final int minutes = (int) time / 60;
		final int seconds = (int) time % 60;
		final int milliseconds = Math.round((time % 1) * 100);
		return source.replace("%NICK%", parkourRecord.getNick())
				.replace("%MINUTES%", twoDigits(minutes))
				.replace("%SECONDS%", twoDigits(seconds))
				.replace("%MILLISECONDS%", twoDigits(milliseconds));
	}

	private String twoDigits(final int number) {
		if (number < 10) {
			return "0" + number;
		}
		return String.valueOf(number);
	}
}
