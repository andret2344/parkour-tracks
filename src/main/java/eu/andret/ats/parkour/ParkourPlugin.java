/*
 * Copyright Andret (c) 2019-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import eu.andret.arguments.AnnotatedCommand;
import eu.andret.arguments.CommandManager;
import eu.andret.arguments.api.annotation.Fallback;
import eu.andret.ats.parkour.api.FinancialProvider;
import eu.andret.ats.parkour.api.RankProvider;
import eu.andret.ats.parkour.parkour.ParkourGame;
import eu.andret.ats.parkour.parkour.ParkourManager;
import eu.andret.ats.parkour.parkour.ParkourRecord;
import eu.andret.ats.parkour.tasks.database.KeepAliveTask;
import eu.andret.ats.parkour.util.Data;
import eu.andret.ats.parkour.util.JSONSerializer;
import eu.andret.ats.parkour.util.M;
import lombok.Getter;
import lombok.Setter;
import org.bstats.bukkit.Metrics;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
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

public final class ParkourPlugin extends JavaPlugin {
	@Getter
	private final Map<String, String> helpDescription = new LinkedHashMap<>();
	private final YamlConfiguration messages = new YamlConfiguration();
	private final YamlConfiguration commands = new YamlConfiguration();
	@Getter
	private final Map<UUID, Integer> teleportCountdown = new HashMap<>();
	@Getter
	private final Map<UUID, Integer> timeCounter = new HashMap<>();
	private Connection connection;
	private ItemStack exitItem;
	@Getter
	private final ParkourManager parkourManager = new ParkourManager();
	@Setter
	private FinancialProvider financialProvider;
	@Setter
	private RankProvider rankProvider;

	private final JSONSerializer jsonSerializer = new JSONSerializer(this);

	@Override
	public void onEnable() {
		if (getWorldEdit() == null) {
			System.out.println("[atsParkour] CRITICAL! Cannot find WorldEdit plugin! Disabling...");
			setEnabled(false);
			return;
		}
		setupConfigFiles();
		exitItem = createDoors();
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

	public String msg(final String path) {
		return Optional.ofNullable(path)
				.map(messages::getString)
				.map(text -> ChatColor.translateAlternateColorCodes('&', text))
				.orElse(null);
	}

	public String msg(final M.Message message) {
		final StringBuilder result = new StringBuilder();
		if (message.isError()) {
			result.append(commands.getString("misc.error-prefix"));
		}
		return ChatColor.translateAlternateColorCodes('&', result.append(commands.getString(message.toString())).toString());
	}

	public Optional<Connection> getConnection() {
		return Optional.ofNullable(connection);
	}

	public WorldEditPlugin getWorldEdit() {
		return (WorldEditPlugin) getServer().getPluginManager().getPlugin("WorldEdit");
	}

	public void updateSyncSign(final ParkourRecord parkourRecord) {
		getServer().getScheduler().scheduleSyncDelayedTask(this, () -> updateSign(parkourRecord));
	}

	public void updateSign(final ParkourRecord parkourRecord) {
		Optional.of(parkourRecord.getGame())
				.map(ParkourGame::getRecordsBlock)
				.map(Location::getBlock)
				.map(Block::getState)
				.filter(x -> x instanceof Sign)
				.map(Sign.class::cast)
				.ifPresent(sign -> {
					IntStream.of(0, 1, 2, 3).forEach(x -> {
						final String lineText = getConfig().getString("recordSign.line" + (x + 1));
						sign.setLine(x, ChatColor.translateAlternateColorCodes('&', replace(String.valueOf(lineText), parkourRecord)));
					});
					sign.update();
				});
	}

	public Optional<ItemStack> getExitItem() {
		return Optional.of(exitItem);
	}

	private void setupConfigFiles() {
		saveDefaultConfig();
		saveResource("commands.yml", false);
		saveResource("scoreboard.yml", false);
		saveResource("messages.yml", false);
		try {
			commands.load(new File(getDataFolder(), "commands.yml"));
			messages.load(new File(getDataFolder(), "messages.yml"));
		} catch (final IOException | InvalidConfigurationException ex) {
			System.out.println("[atsParkour] An error occurred when loading messages");
			ex.printStackTrace();
		}
		generate();
	}

	private ItemStack createDoors() {
		final ItemStack result = new ItemStack(Material.IRON_DOOR);
		Optional.ofNullable(result.getItemMeta())
				.ifPresent(meta -> {
					meta.setDisplayName("§r" + ChatColor.translateAlternateColorCodes('&', String.valueOf(getConfig().getString("door.name"))));
					result.setItemMeta(meta);
				});
		return result;
	}

	private void setupCommand() {
		final AnnotatedCommand command = CommandManager.registerCommand(ParkourCommand.class, this);
		command.setOnInsufficientPermissionsListener(sender -> sender.sendMessage(msg(M.Error.DEFAULT.insufficientPermissions)));
		command.setOnUnknownSubCommandExecutionListener(sender -> sender.sendMessage(msg(M.Error.DEFAULT.invalidArgument)));
		command.getOptions().setAutoTranslateColors(true);
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

	public boolean isEditLocked() {
		return getConfig().getBoolean("edit-lock", true);
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
			System.out.printf("[atsParkour] Successfully saved %d parkour games", parkourManager.getAllGames().size());
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

	public Optional<FinancialProvider> getFinancialProvider() {
		return Optional.ofNullable(financialProvider);
	}

	public Optional<RankProvider> getRankProvider() {
		return Optional.ofNullable(rankProvider);
	}

	private void generate() {
		helpDescription.put("help|?", msg(M.List.HELP.help));
		helpDescription.put("lobby", msg(M.General.LOBBY.help));
		helpDescription.put("create|c", msg(M.Executive.CREATE.help));
		helpDescription.put("remove|r", msg(M.Executive.REMOVE.help));
		helpDescription.put("rename|rn", msg(M.Executive.RENAME.help));
		helpDescription.put("info", msg(M.Executive.INFO.help));
		helpDescription.put("setSpawn|ss", msg(M.Executive.SPAWN.help));
		helpDescription.put("recreate", msg(M.Executive.RECREATE.help));
		helpDescription.put("start", msg(M.Executive.START.help));
		helpDescription.put("stop", msg(M.Executive.STOP.help));
		helpDescription.put("addCheckpoint|ac", msg(M.Region.Checkpoint.ADD.help));
		helpDescription.put("setCheckpoint|sc", msg(M.Region.Checkpoint.SET.help));
		helpDescription.put("addWall|aw", msg(M.Region.Wall.ADD.help));
		helpDescription.put("setWall|sw", msg(M.Region.Wall.SET.help));
		helpDescription.put("list|ls", msg(M.List.GAMES.help));
		helpDescription.put("ignore|i", msg(M.General.IGNORE.help));
		helpDescription.put("sprintForced", msg(M.Option.SPRINT_FORCED.help));
		helpDescription.put("alwaysSpawn", msg(M.Option.ALWAYS_SPAWN.help));
		helpDescription.put("savingResults", msg(M.Option.SAVING_RESULTS.help));
		helpDescription.put("damageAllowed", msg(M.Option.DAMAGE_ALLOWED.help));
		helpDescription.put("effects", msg(M.List.EFFECT.help));
		helpDescription.put("setEffect", msg(M.Amplifier.EFFECT.help));
		helpDescription.put("boat", msg(M.Option.BOAT.help));
		helpDescription.put("enabled", msg(M.Option.ENABLED.help));
		helpDescription.put("modifyInventory", msg(M.Option.MODIFY_INVENTORY.help));
		helpDescription.put("recordsBlock", msg(M.Executive.RECORDS_BLOCK.help));
		helpDescription.put("teleportBlock", msg(M.Executive.TELEPORT_BLOCK.help));
		helpDescription.put("teleport|tp", msg(M.Executive.TELEPORT.help));
		helpDescription.put("fix", msg(M.General.FIX.help));
		helpDescription.put("color", msg(M.Option.COLOR.help));
		helpDescription.put("difficulty", msg(M.Option.DIFFICULTY.help));
		helpDescription.put("type", msg(M.Option.TYPE.help));
		helpDescription.put("displayName", msg(M.Parkour.DISPLAY_NAME.help));
		helpDescription.put("authors", msg(M.Parkour.AUTHORS.help));
		helpDescription.put("vip", msg(M.Option.VIP_ONLY.help));
		helpDescription.put("bronze", msg(M.Medal.BRONZE.help));
		helpDescription.put("silver", msg(M.Medal.SILVER.help));
		helpDescription.put("gold", msg(M.Medal.GOLD.help));
		helpDescription.put("platinum", msg(M.Medal.PLATINUM.help));
	}

	private String replace(final String source, final ParkourRecord parkourRecord) {
		final String name = Optional.of(parkourRecord)
				.map(ParkourRecord::getUuid)
				.map(x -> getServer().getOfflinePlayer(x))
				.map(OfflinePlayer::getName)
				.orElse("========");
		return source.replace("%NICK%", name)
				.replace("%PERSONAL_TIME%", formatTime(parkourRecord.getTime()));
	}

	public String formatTime(final double time) {
		final int minutes = (int) time / 60;
		final int seconds = (int) time % 60;
		final int milliseconds = (int) Math.round((time % 1) * 100);
		return String.format("%02d:%02d.%02d", minutes, seconds, milliseconds);
	}
}
