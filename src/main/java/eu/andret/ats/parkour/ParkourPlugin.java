/*
 * Copyright Andret (c) 2018-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import eu.andret.arguments.AnnotatedCommand;
import eu.andret.arguments.CommandManager;
import eu.andret.arguments.api.annotation.Fallback;
import eu.andret.ats.parkour.api.FinancialProvider;
import eu.andret.ats.parkour.api.RankProvider;
import eu.andret.ats.parkour.entity.EventSound;
import eu.andret.ats.parkour.entity.MedalSetupOption;
import eu.andret.ats.parkour.entity.SimpleLever;
import eu.andret.ats.parkour.item.ParkourInteractiveItem;
import eu.andret.ats.parkour.item.ParkourItem;
import eu.andret.ats.parkour.item.ParkourItemMap;
import eu.andret.ats.parkour.parkour.ParkourGame;
import eu.andret.ats.parkour.parkour.ParkourGameCreator;
import eu.andret.ats.parkour.parkour.ParkourManager;
import eu.andret.ats.parkour.parkour.ParkourMedal;
import eu.andret.ats.parkour.parkour.ParkourRecord;
import eu.andret.ats.parkour.player.PlayerManager;
import eu.andret.ats.parkour.tasks.database.KeepAliveTask;
import eu.andret.ats.parkour.tutorial.TutorialManager;
import eu.andret.ats.parkour.util.Constants;
import eu.andret.ats.parkour.util.Data;
import eu.andret.ats.parkour.util.M;
import eu.andret.ats.parkour.util.adapter.LocationAdapter;
import eu.andret.ats.parkour.util.adapter.MedalAdapter;
import eu.andret.ats.parkour.util.adapter.PotionEffectTypeAdapter;
import eu.andret.ats.parkour.util.adapter.WorldAdapter;
import lombok.Getter;
import lombok.Setter;
import org.bstats.bukkit.Metrics;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
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
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class ParkourPlugin extends JavaPlugin {
	@NotNull
	private static final String MEDAL = "medal";
	@Getter
	@NotNull
	private final Map<String, String> helpDescription = new LinkedHashMap<>();
	@NotNull
	private final YamlConfiguration messages = new YamlConfiguration();
	@NotNull
	private final YamlConfiguration commands = new YamlConfiguration();
	@NotNull
	private final YamlConfiguration inventory = new YamlConfiguration();
	@Getter
	@NotNull
	private final Map<UUID, Integer> teleportCountdown = new HashMap<>();
	@Getter
	@NotNull
	private final Map<UUID, Integer> timeCounter = new HashMap<>();
	@Nullable
	private Connection connection;
	private ItemStack exitItem;
	private ItemStack hidingItem;
	@Getter
	@NotNull
	private final ParkourManager parkourManager = new ParkourManager();
	@Getter
	@NotNull
	private final PlayerManager playerManager = new PlayerManager();
	@NotNull
	@Getter
	private final TutorialManager tutorialManager = new TutorialManager(this);
	@Setter
	@Nullable
	private FinancialProvider financialProvider;
	@Setter
	@Nullable
	private RankProvider rankProvider;
	@Getter
	@NotNull
	private final ParkourItemMap gameItemMap = new ParkourItemMap();
	@Getter
	@NotNull
	private final ParkourItemMap worldItemMap = new ParkourItemMap();
	@Getter
	@NotNull
	private final List<ParkourMedal> medals = new ArrayList<>();
	@Getter
	private int teleportationTimeout;
	private DecimalFormat decimalFormat;
	@NotNull
	private final Gson gson = new GsonBuilder()
			.registerTypeHierarchyAdapter(PotionEffectType.class, new PotionEffectTypeAdapter())
			.registerTypeHierarchyAdapter(World.class, new WorldAdapter(this))
			.registerTypeHierarchyAdapter(Location.class, new LocationAdapter())
			.registerTypeHierarchyAdapter(ParkourMedal.class, new MedalAdapter(this))
			.registerTypeAdapter(ParkourGame.class, new ParkourGameCreator())
			.setPrettyPrinting()
			.create();

	@Override
	public void onEnable() {
		setupConfigFiles();
		medals.addAll(loadMedals());
		exitItem = createItem("exit");
		hidingItem = createItem("hiding");
		teleportationTimeout = getConfig().getInt("teleportation-timeout");
		decimalFormat = Optional.of(getConfig())
				.map(config -> config.getConfigurationSection("economy"))
				.map(this::setupDecimalFormat)
				.orElse(new DecimalFormat());
		getServer().getPluginManager().registerEvents(new ParkourListeners(this), this);
		setupCommand();
		setupDatabase();
		loadGames();
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
					printWriter.write(gson.toJson(parkourManager.getSetting()));
					printWriter.close();
					getLogger().info("Successfully created \"backups/" + name + "\" file!");
				} catch (final FileNotFoundException ex) {
					getLogger().severe("An error occurred when trying to backup parkours!");
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

	@NotNull
	public String msg(@NotNull final String path) {
		return Optional.of(path)
				.map(messages::getString)
				.map(text -> ChatColor.translateAlternateColorCodes('&', text))
				.orElse("");
	}

	@NotNull
	public String msg(@NotNull final M.Message message) {
		final StringBuilder result = new StringBuilder();
		if (message.isError()) {
			result.append(commands.getString("misc.prefix-error"));
		}
		return ChatColor.translateAlternateColorCodes('&', result.append(commands.getString(message.toString())).toString());
	}

	@NotNull
	public String misc(@NotNull final String name) {
		return Optional.of(name)
				.map(text -> "misc." + text)
				.map(commands::getString)
				.map(text -> ChatColor.translateAlternateColorCodes('&', text))
				.orElse("");
	}

	@NotNull
	public Optional<Connection> getConnection() {
		return Optional.ofNullable(connection);
	}

	@NotNull
	public WorldEditPlugin getWorldEdit() {
		return getPlugin(WorldEditPlugin.class);
	}

	@NotNull
	public Optional<Sound> getSound(@NotNull final EventSound eventSound) {
		return Optional.of(getConfig())
				.map(configuration -> configuration.getString("sound." + eventSound.name().toLowerCase(), "NONE"))
				.filter(sound -> !sound.equals("NONE"))
				.map(Sound::valueOf);
	}

	public void updateSyncSign(@NotNull final ParkourRecord parkourRecord) {
		getServer().getScheduler().scheduleSyncDelayedTask(this, () -> updateSign(parkourRecord));
	}

	public void updateSign(@NotNull final ParkourRecord parkourRecord) {
		updateSign(parkourRecord.getGame(), line -> replace(String.valueOf(line), parkourRecord));
	}

	public void updateSyncSign(@NotNull final ParkourGame parkourGame) {
		getServer().getScheduler().scheduleSyncDelayedTask(this, () -> updateSign(parkourGame));
	}

	public void updateSign(@NotNull final ParkourGame parkourGame) {
		updateSign(parkourGame, this::replace);
	}

	public ItemStack getExitItem() {
		return exitItem;
	}

	public ItemStack getHidingItem() {
		return hidingItem;
	}

	@NotNull
	public String formatTime(final double time) {
		final int minutes = (int) time / 60;
		final int seconds = (int) time % 60;
		final int milliseconds = (int) Math.round((time % 1) * 100);
		return String.format("%02d:%02d.%02d", minutes, seconds, milliseconds);
	}

	@NotNull
	public String formatMoney(final double money) {
		return decimalFormat.format(money);
	}

	@NotNull
	public String formatCoord(final double coord) {
		return String.format("%.2f", coord);
	}

	@NotNull
	public String getFormattedMedals(@NotNull final String separator) {
		return getMedals().stream()
				.map(ParkourMedal::getDisplayName)
				.collect(Collectors.joining(separator));
	}

	public boolean isEditLockActive() {
		return getConfig().getBoolean("edit-lock", true);
	}

	@NotNull
	public Optional<FinancialProvider> getFinancialProvider() {
		return Optional.ofNullable(financialProvider);
	}

	@NotNull
	public Optional<RankProvider> getRankProvider() {
		return Optional.ofNullable(rankProvider);
	}

	// =============== PRIVATE =============== //

	@NotNull
	private List<ParkourMedal> loadMedals() {
		final ConfigurationSection medalsSection = getConfig().getConfigurationSection(MEDAL);
		if (medalsSection == null) {
			getLogger().info("No medals loaded!");
			return Collections.emptyList();
		}
		return medalsSection.getKeys(false).stream()
				.map(key -> Optional.of(key)
						.map(medalsSection::getConfigurationSection)
						.map(configurationSection -> {
							final String display = ChatColor.translateAlternateColorCodes('&', configurationSection.getString("display", key));
							final int importance = configurationSection.getInt("importance");
							return new ParkourMedal(key, display, importance);
						})
						.orElse(null))
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
			getLogger().info("An error occurred when loading messages");
			ex.printStackTrace();
		}
		generate();
	}

	@NotNull
	private ItemStack createItem(@NotNull final String path) {
		final ConfigurationSection section = inventory.getConfigurationSection(String.join(".", "game", path));
		if (section == null) {
			throw new NullPointerException("Section " + path + " doesn't exist in config file!");
		}
		final Material material = Material.valueOf(section.getString("material"));
		final String name = ChatColor.translateAlternateColorCodes('&', "&r" + section.getString("name"));
		final List<String> lore = section.getStringList("lore").stream()
				.map(line -> ChatColor.translateAlternateColorCodes('&', "&r" + line))
				.collect(Collectors.toList());
		final ParkourItem parkourItem = new ParkourInteractiveItem(material, name, lore);
		gameItemMap.setItem(section.getInt("position"), parkourItem);
		return parkourItem.toItemStack();
	}

	private void setupCommand() {
		final AnnotatedCommand<ParkourPlugin> command = CommandManager.registerCommand(ParkourCommand.class, this);
		command.setOnInsufficientPermissionsListener(sender -> sender.sendMessage(msg(M.Error.DEFAULT.insufficientPermissions)));
		command.setOnUnknownSubCommandExecutionListener(sender -> sender.sendMessage(msg(M.Error.DEFAULT.invalidArgument)));
		command.setOnMainCommandExecutionListener(sender -> sender.sendMessage(msg("main-command")));
		command.getOptions().setAutoTranslateColors(true);
		command.getOptions().setCaseSensitive(false);

		command.addTypeMapper(ParkourGame.class, parkourManager::getParkour, Fallback.ON_NULL);
		command.addTypeMapper(PotionEffectType.class, PotionEffectType::getByName, Fallback.ON_NULL);
		command.addEnumMapper(DyeColor.class, Fallback.ON_NULL);
		command.addTypeMapper(ParkourMedal.class, name -> medals.stream()
						.filter(medal -> medal.getName().equals(name))
						.findAny()
						.orElse(null),
				Fallback.ON_NULL);
		command.addEnumMapper(MedalSetupOption.class);
		command.addEnumMapper(SimpleLever.class);

		command.addTypeCompleter(ParkourGame.class, () -> parkourManager.getAllGames().stream()
				.map(ParkourGame::getName)
				.collect(Collectors.toList()));

		command.addEnumCompleter(SimpleLever.class);
		command.addTypeCompleter(PotionEffectType.class, Data.ALLOWED_EFFECTS.stream()
				.map(PotionEffectType::getName)
				.collect(Collectors.toList()));
		command.addTypeCompleter(ParkourMedal.class, medals.stream()
				.map(ParkourMedal::getName)
				.collect(Collectors.toList()));
		command.addTypeCompleter(boolean.class, Arrays.asList(Boolean.FALSE.toString(), Boolean.TRUE.toString()));
		command.addEnumCompleter(MedalSetupOption.class);
		command.addEnumCompleter(DyeColor.class);
	}

	private void setupDatabase() {
		final boolean databaseEnabled = getConfig().getBoolean("database.enabled", false);
		if (!databaseEnabled) {
			getLogger().warning("Database is disabled. In order to save records, enable it in config.");
			return;
		}
		try {
			connection = createConnection();
			connect();
			getLogger().info("Database connection established.");
		} catch (final SQLException ex) {
			getLogger().severe("An error occurred when trying to connect to database.");
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

	private void connect() {
		final String database = getConfig().getString("database.dbname", "ats_parkour");
		getConnection().ifPresent(conn -> {
			try (final Statement stat = conn.createStatement()) {
				stat.execute("CREATE DATABASE IF NOT EXISTS `" + database + "`;");
				stat.execute("USE " + database + ";");
				stat.execute("CREATE TABLE IF NOT EXISTS ats_parkour_records(id INT PRIMARY KEY AUTO_INCREMENT, date DATETIME, uuid VARCHAR(64), parkour VARCHAR(64), duration DECIMAL(8, 2));");
			} catch (final SQLException ex) {
				ex.printStackTrace();
			}
		});
	}

	private void updateSign(@NotNull final ParkourGame parkourGame, @NotNull final UnaryOperator<String> replaceFunction) {
		Optional.of(parkourGame)
				.map(ParkourGame::getRecordsBlock)
				.map(Location::getBlock)
				.map(Block::getState)
				.filter(Sign.class::isInstance)
				.map(Sign.class::cast)
				.ifPresent(sign -> {
					IntStream.of(0, 1, 2, 3).forEach(i -> {
						final String lineText = getConfig().getString("recordSign.line" + (i + 1));
						sign.setLine(i, ChatColor.translateAlternateColorCodes('&', replaceFunction.apply(lineText)));
					});
					sign.update();
				});
	}

	private void save() {
		try {
			final File target = new File(getDataFolder(), "setting.json");
			if (!target.exists() && !target.createNewFile()) {
				getLogger().severe("An error occurred when trying to create parkour setting file");
				return;
			}
			final PrintWriter printWriter = new PrintWriter(target);
			printWriter.write(gson.toJson(parkourManager.getSetting()));
			printWriter.close();
			getLogger().info("Successfully saved parkour setting");
		} catch (final IOException ex) {
			getLogger().severe("An error occurred when trying to save parkour setting");
			ex.printStackTrace();
		}
	}

	private void loadGames() {
		final File lobby = new File(getDataFolder(), "setting.json");
		if (!lobby.exists()) {
			return;
		}
		try (final Reader reader = new FileReader(lobby)) {
			parkourManager.setSetting(gson.fromJson(reader, ParkourManager.ParkourSetting.class));
			getLogger().info("Successfully loaded parkour setting");
		} catch (final IOException ex) {
			getLogger().severe("An error occurred when trying to load parkour setting");
			ex.printStackTrace();
		}
	}

	private void generate() {
		helpDescription.put("help|?", msg(M.List.HELP.helpMessage));
		helpDescription.put("lobby", msg(M.General.LOBBY.helpMessage));
		helpDescription.put("create", msg(M.Executive.CREATE.helpMessage));
		helpDescription.put("remove", msg(M.Executive.REMOVE.helpMessage));
		helpDescription.put("rename", msg(M.Executive.RENAME.helpMessage));
		helpDescription.put("info", msg(M.Executive.INFO.helpMessage));
		helpDescription.put("setSpawn", msg(M.Executive.SPAWN.helpMessage));
		helpDescription.put("recreate", msg(M.Executive.RECREATE.helpMessage));
		helpDescription.put("start", msg(M.Executive.START.helpMessage));
		helpDescription.put("stop", msg(M.Executive.STOP.helpMessage));
		helpDescription.put("addCheckpoint", msg(M.Region.Checkpoint.ADD.helpMessage));
		helpDescription.put("setCheckpoint", msg(M.Region.Checkpoint.SET.helpMessage));
		helpDescription.put("addWall", msg(M.Region.Wall.ADD.helpMessage));
		helpDescription.put("setWall", msg(M.Region.Wall.SET.helpMessage));
		helpDescription.put("list|ls", msg(M.List.GAMES.helpMessage));
		helpDescription.put("ignore|i", msg(M.General.IGNORE.helpMessage));
		helpDescription.put("sprintForced", msg(M.Option.SPRINT_FORCED.helpMessage));
		helpDescription.put("alwaysSpawn", msg(M.Option.ALWAYS_SPAWN.helpMessage));
		helpDescription.put("savingResults", msg(M.Option.SAVING_RESULTS.helpMessage));
		helpDescription.put("damageAllowed", msg(M.Option.DAMAGE_ALLOWED.helpMessage));
		helpDescription.put("effects", msg(M.List.EFFECT.helpMessage));
		helpDescription.put("setEffect", msg(M.Amplifier.EFFECT.helpMessage));
		helpDescription.put("boat", msg(M.Option.BOAT.helpMessage));
		helpDescription.put("enabled", msg(M.Option.ENABLED.helpMessage));
		helpDescription.put("modifyInventory", msg(M.Option.MODIFY_INVENTORY.helpMessage));
		helpDescription.put("recordsBlock", msg(M.Executive.RECORDS_BLOCK.helpMessage));
		helpDescription.put("teleportBlock", msg(M.Executive.TELEPORT_BLOCK.helpMessage));
		helpDescription.put("teleport|tp", msg(M.Executive.TELEPORT.helpMessage));
		helpDescription.put("fix", msg(M.General.FIX.helpMessage));
		helpDescription.put("color", msg(M.Option.COLOR.helpMessage));
		helpDescription.put("difficulty", msg(M.Option.DIFFICULTY.helpMessage));
		helpDescription.put("type", msg(M.Option.TYPE.helpMessage));
		helpDescription.put("displayName", msg(M.Parkour.DISPLAY_NAME.helpMessage));
		helpDescription.put("authors", msg(M.Parkour.AUTHORS.helpMessage));
		helpDescription.put("vipOnly", msg(M.Option.VIP_ONLY.helpMessage));
		helpDescription.put(MEDAL, msg(M.Option.MEDAL.helpMessage));
	}

	@NotNull
	private String replace(@NotNull final String source, @NotNull final ParkourRecord parkourRecord) {
		final String name = Optional.of(parkourRecord)
				.map(ParkourRecord::getUuid)
				.map(uuid -> getServer().getOfflinePlayer(uuid))
				.map(OfflinePlayer::getName)
				.orElse(Constants.PLACEHOLDER_NO_RECORD);
		return source.replace(Constants.NICK, name)
				.replace(Constants.PERSONAL_TIME, formatTime(parkourRecord.getTime()));
	}

	@NotNull
	private String replace(@NotNull final String source) {
		return source.replace(Constants.NICK, Constants.PLACEHOLDER_NO_RECORD).replace(Constants.PERSONAL_TIME, formatTime(0));
	}

	@NotNull
	private DecimalFormat setupDecimalFormat(@NotNull final ConfigurationSection economySection) {
		final DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols();
		decimalFormatSymbols.setCurrencySymbol(economySection.getString("currency-symbol", "{@}"));
		decimalFormatSymbols.setDecimalSeparator(economySection.getString("decimal-separator", ".").charAt(0));
		decimalFormatSymbols.setGroupingSeparator(economySection.getString("group-separator", " ").charAt(0));
		final DecimalFormat format = new DecimalFormat(economySection.getString("pattern", "+###,##0.00\u00A4;-###,##0.00\u00A4"), decimalFormatSymbols);
		format.setGroupingSize(economySection.getInt("group-size", 3));
		return format;
	}
}
