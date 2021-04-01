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
import eu.andret.ats.parkour.parkour.ParkourMedal;
import eu.andret.ats.parkour.parkour.ParkourRecord;
import eu.andret.ats.parkour.player.PlayerManager;
import eu.andret.ats.parkour.tasks.database.KeepAliveTask;
import eu.andret.ats.parkour.util.Data;
import eu.andret.ats.parkour.util.M;
import eu.andret.ats.parkour.util.ParkourItem;
import eu.andret.ats.parkour.util.ParkourItemMap;
import eu.andret.ats.parkour.util.serializer.JSONSerializer;
import lombok.Getter;
import lombok.Setter;
import org.bstats.bukkit.Metrics;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class ParkourPlugin extends JavaPlugin {
	private static final String MEDAL = "medal";
	@Getter
	private final Map<String, String> helpDescription = new LinkedHashMap<>();
	private final YamlConfiguration messages = new YamlConfiguration();
	private final YamlConfiguration commands = new YamlConfiguration();
	private final YamlConfiguration inventory = new YamlConfiguration();
	@Getter
	private final Map<UUID, Integer> teleportCountdown = new HashMap<>();
	@Getter
	private final Map<UUID, Integer> timeCounter = new HashMap<>();
	private Connection connection;
	private ItemStack exitItem;
	private ItemStack hidingItem;
	private final JSONSerializer jsonSerializer = new JSONSerializer(this);
	@Getter
	private final ParkourManager<JSONObject> parkourManager = new ParkourManager<>(jsonSerializer);
	@Getter
	private final PlayerManager playerManager = new PlayerManager();
	@Setter
	private FinancialProvider financialProvider;
	@Setter
	private RankProvider rankProvider;
	@Getter
	private final ParkourItemMap gameItemMap = new ParkourItemMap();
	@Getter
	private final ParkourItemMap worldItemMap = new ParkourItemMap();
	@Getter
	private final List<ParkourMedal> medals = new ArrayList<>();

	@Override
	public void onEnable() {
		setupConfigFiles();
		medals.addAll(loadMedals());
		exitItem = createItem("exit");
		hidingItem = createItem("hiding");
		getServer().getPluginManager().registerEvents(new ParkourListeners(this), this);
		setupCommand();
		setupDatabase();
		load();
		getConnection()
				.map(KeepAliveTask::new)
				.ifPresent(keepAliveTask -> getServer().getScheduler().scheduleSyncRepeatingTask(this, keepAliveTask, 20_000, 20_000));

		final long backupFrequency = getConfig().getLong("backup-frequency", 1440L);
		if (backupFrequency > 0) {
			getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> {
				final File backups = new File(getDataFolder(), "backups");
				if (!backups.exists() && !backups.mkdirs()) {
					throw new UnsupportedOperationException("An error occurred when trying to create backup folder!");
				}
				final LocalDateTime now = LocalDateTime.now();
				final String name = String.format("backup_%02d%02d%02d_%02d%02d%02d.json", now.getYear(), now.getMonth().getValue(), now.getDayOfMonth(), now.getHour(), now.getMinute(), now.getSecond());
				final File target = new File(backups.getPath(), name);
				try {
					final PrintWriter printWriter = new PrintWriter(target);
					printWriter.write(parkourManager.serialize().toString(4));
					printWriter.close();
					System.out.println("Successfully created \"backups/" + name + "\" file!");
				} catch (final FileNotFoundException ex) {
					System.out.println("An error occurred when trying to backup parkours!");
					ex.printStackTrace();
				}
			}, 6000, 1200L * backupFrequency);
		}

		new Metrics(this, 10700);
	}

	@Override
	public void onDisable() {
		save();
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
		return getPlugin(WorldEditPlugin.class);
	}

	public Optional<Sound> getSound(final String name) {
		return Optional.of(getConfig())
				.map(configuration -> configuration.getString("sound." + name))
				.filter(sound -> !sound.equals("NONE"))
				.map(Sound::valueOf);
	}

	public void updateSyncSign(final ParkourRecord parkourRecord) {
		getServer().getScheduler().scheduleSyncDelayedTask(this, () -> updateSign(parkourRecord));
	}

	public void updateSign(final ParkourRecord parkourRecord) {
		Optional.of(parkourRecord.getGame())
				.map(ParkourGame::getRecordsBlock)
				.map(Location::getBlock)
				.map(Block::getState)
				.filter(blockState -> blockState instanceof Sign)
				.map(Sign.class::cast)
				.ifPresent(sign -> {
					IntStream.of(0, 1, 2, 3).forEach(i -> {
						final String lineText = getConfig().getString("recordSign.line" + (i + 1));
						sign.setLine(i, ChatColor.translateAlternateColorCodes('&', replace(String.valueOf(lineText), parkourRecord)));
					});
					sign.update();
				});
	}

	@NotNull
	public ItemStack getExitItem() {
		return exitItem;
	}

	@NotNull
	public ItemStack getHidingItem() {
		return hidingItem;
	}

	@NotNull
	private List<ParkourMedal> loadMedals() {
		final ConfigurationSection medalsSection = getConfig().getConfigurationSection(MEDAL);
		if (medalsSection == null) {
			System.out.println("No medals loaded!");
			return Collections.emptyList();
		}
		return medalsSection.getKeys(false).stream()
				.map(s -> {
					final ConfigurationSection configurationSection = medalsSection.getConfigurationSection(s);
					if (configurationSection == null) {
						return null;
					}
					final String display = ChatColor.translateAlternateColorCodes('&', String.valueOf(configurationSection.getString("display", s)));
					final int importance = configurationSection.getInt("importance");
					return new ParkourMedal(s, display, importance);
				})
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
	}

	private void setupConfigFiles() {
		saveDefaultConfig();
		saveResource("commands.yml", false);
		saveResource("messages.yml", false);
		saveResource("inventory.yml", false);
		try {
			commands.load(new File(getDataFolder(), "commands.yml"));
			messages.load(new File(getDataFolder(), "messages.yml"));
			inventory.load(new File(getDataFolder(), "inventory.yml"));
		} catch (final IOException | InvalidConfigurationException ex) {
			System.out.println("An error occurred when loading messages");
			ex.printStackTrace();
		}
		generate();
	}

	@NotNull
	private ItemStack createItem(final String path) {
		final ConfigurationSection section = inventory.getConfigurationSection(String.join(".", "game", path));
		if (section == null) {
			throw new NullPointerException("Section " + path + " doesn't exist in config file!");
		}
		final Material material = Material.valueOf(section.getString("material"));
		final String name = ChatColor.translateAlternateColorCodes('&', "&r" + section.getString("name"));
		final List<String> lore = section.getStringList("lore").stream()
				.map(line -> ChatColor.translateAlternateColorCodes('&', "&r" + line))
				.collect(Collectors.toList());
		final ParkourItem parkourItem = new ParkourItem(material, name, lore);
		gameItemMap.setItem(section.getInt("position"), parkourItem);
		return parkourItem.toItemStack();
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
		command.addArgumentCompleter("startStop", Arrays.asList("start", "stop"));
		command.addArgumentCompleter(MEDAL, medals.stream()
				.map(ParkourMedal::getName)
				.collect(Collectors.toList()));
		command.addArgumentCompleter("medalOption", Arrays.asList("time", "reward"));
		command.addArgumentMapper(MEDAL, ParkourMedal.class, name -> medals.stream()
						.filter(medal -> medal.getName().equals(name))
						.findAny()
						.orElse(null),
				Fallback.ON_NULL);
	}

	public boolean isEditLocked() {
		return getConfig().getBoolean("edit-lock", true);
	}

	private void setupDatabase() {
		final boolean databaseEnabled = getConfig().getBoolean("database.enabled", false);
		if (!databaseEnabled) {
			System.out.println("Database is disabled. In order to save records, enable it in config.");
			return;
		}
		try {
			connection = createConnection();
			connect();
			System.out.println("Database connection established.");
		} catch (final SQLException ex) {
			System.out.println("An error occurred when trying to connect to database.");
			connection = null;
			ex.printStackTrace();
		}
	}

	@NotNull
	private Connection createConnection() throws SQLException {
		final String url = getConfig().getString("database.url", "localhost");
		final String user = getConfig().getString("database.user", "root");
		final String pass = getConfig().getString("database.pass", "");
		return DriverManager.getConnection("jdbc:mysql://" + url + "?allowPublicKeyRetrieval=true&autoReconnect=true&useSSL=false", user, pass);
	}

	private void connect() throws SQLException {
		final String database = getConfig().getString("database.dbname", "ats_parkour");
		try (final Statement stat = connection.createStatement()) {
			stat.execute("CREATE DATABASE IF NOT EXISTS `" + database + "`;");
			stat.execute("USE " + database + ";");
			stat.execute("CREATE TABLE IF NOT EXISTS ats_parkour_records(id INT PRIMARY KEY AUTO_INCREMENT, date DATETIME, uuid VARCHAR(64), parkour VARCHAR(64), time FLOAT);");
		}
	}

	private void save() {
		try {
			final File lobby = new File(getDataFolder(), "setting.json");
			if (!lobby.exists() && !lobby.createNewFile()) {
				System.out.println("An error occurred when trying to create parkour setting file");
				return;
			}
			final PrintWriter printWriter = new PrintWriter(lobby);
			printWriter.write(parkourManager.serialize().toString(4));
			printWriter.close();
			System.out.println("Successfully saved parkour setting");
		} catch (final IOException ex) {
			System.out.println("An error occurred when trying to save parkour setting");
			ex.printStackTrace();
		}
	}

	private void load() {
		final File lobby = new File(getDataFolder(), "setting.json");
		if (!lobby.exists()) {
			return;
		}
		try (final Reader reader = new FileReader(lobby)) {
			final JSONTokener jsonTokener = new JSONTokener(reader);
			final JSONObject jsonObject = new JSONObject(jsonTokener);
			parkourManager.deserialize(jsonObject);
			System.out.println("Successfully loaded parkour setting");
		} catch (final IOException ex) {
			System.out.println("An error occurred when trying to load parkour setting");
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
		helpDescription.put(MEDAL, msg(M.Option.MEDAL.help));
	}

	@NotNull
	private String replace(final String source, final ParkourRecord parkourRecord) {
		final String name = Optional.of(parkourRecord)
				.map(ParkourRecord::getUuid)
				.map(uuid -> getServer().getOfflinePlayer(uuid))
				.map(OfflinePlayer::getName)
				.orElse("========");
		return source.replace("%NICK%", name)
				.replace("%PERSONAL_TIME%", formatTime(parkourRecord.getTime()));
	}

	@NotNull
	public String formatTime(final double time) {
		final int minutes = (int) time / 60;
		final int seconds = (int) time % 60;
		final int milliseconds = (int) Math.round((time % 1) * 100);
		return String.format("%02d:%02d.%02d", minutes, seconds, milliseconds);
	}
}
