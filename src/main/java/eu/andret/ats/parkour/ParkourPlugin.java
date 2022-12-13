/*
 * Copyright Andret (c) 2018-2022. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import eu.andret.arguments.AnnotatedCommand;
import eu.andret.arguments.CommandManager;
import eu.andret.arguments.api.entity.FallbackConstants;
import eu.andret.ats.parkour.api.FinancialProvider;
import eu.andret.ats.parkour.api.RankProvider;
import eu.andret.ats.parkour.entity.EventSound;
import eu.andret.ats.parkour.entity.MedalRequirement;
import eu.andret.ats.parkour.entity.MedalSetupOption;
import eu.andret.ats.parkour.entity.SimpleLever;
import eu.andret.ats.parkour.item.ParkourInteractiveItem;
import eu.andret.ats.parkour.item.ParkourItem;
import eu.andret.ats.parkour.item.ParkourItemMap;
import eu.andret.ats.parkour.parkour.Medal;
import eu.andret.ats.parkour.parkour.ParkourGame;
import eu.andret.ats.parkour.parkour.ParkourGameCreator;
import eu.andret.ats.parkour.parkour.ParkourManager;
import eu.andret.ats.parkour.player.ParkourPlayer;
import eu.andret.ats.parkour.player.PlayerManager;
import eu.andret.ats.parkour.region.Checkpoint;
import eu.andret.ats.parkour.tasks.database.FetchParkourBestScoreTask;
import eu.andret.ats.parkour.tasks.database.FetchParkourPlayerScoreTask;
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
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ParkourPlugin extends JavaPlugin {
	@Getter
	@NotNull
	private final Map<String, String> helpDescription = new LinkedHashMap<>();
	@NotNull
	private final YamlConfiguration messages = new YamlConfiguration();
	@NotNull
	private final YamlConfiguration commands = new YamlConfiguration();
	@NotNull
	private final YamlConfiguration inventory = new YamlConfiguration();
	@NotNull
	private final YamlConfiguration scoreboard = new YamlConfiguration();
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
	private final List<Medal> medals = new ArrayList<>();
	@Getter
	private int teleportationTimeout;
	private DecimalFormat decimalFormat;
	@NotNull
	private final Gson gson = new GsonBuilder()
			.registerTypeHierarchyAdapter(PotionEffectType.class, new PotionEffectTypeAdapter())
			.registerTypeHierarchyAdapter(World.class, new WorldAdapter(this))
			.registerTypeHierarchyAdapter(Location.class, new LocationAdapter(this))
			.registerTypeHierarchyAdapter(Medal.class, new MedalAdapter(this))
			.registerTypeAdapter(ParkourGame.class, new ParkourGameCreator(this))
			.setPrettyPrinting()
			.create();
	@NotNull
	public final List<ArmorStand> indicators = new ArrayList<>();

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
		getServer().getScheduler().scheduleSyncRepeatingTask(this, new KeepAliveTask(this), 20_000, 20_000);

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
					final JsonWriter jsonWriter = gson.newJsonWriter(new PrintWriter(target));
					jsonWriter.setIndent("\t");
					gson.toJson(parkourManager.getSetting(), ParkourManager.ParkourSetting.class, jsonWriter);
					jsonWriter.close();
					getLogger().info("Successfully created \"backups/" + name + "\" file!");
				} catch (final IOException ex) {
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

	/**
	 * Reads the scoreboard config and gets lines to displays including placeholders to be replaced.
	 *
	 * @return The {@link List} of lines with placeholders to display on the scoreboard.
	 */
	public List<String> getScoreboardPattern() {
		return scoreboard.getStringList("scoreboard.content");
	}

	/**
	 * Gets the display name from the scoreboard config file that can include placeholders to be replaced.
	 *
	 * @return The placeholder text to display as the scoreboard name.
	 */
	public String getScoreboardDisplayName() {
		return scoreboard.getString("scoreboard.display-name");
	}

	/**
	 * The Scoreboard can contain dates (i.e., last run date) so this formatter allows formatting all those dates.
	 *
	 * @return The formatter to format all dates on the displayed Scoreboard.
	 */
	public DateTimeFormatter getScoreboardDateTimeFormatter() {
		return DateTimeFormatter.ofPattern(scoreboard.getString("scoreboard.date-time-format", "yyyy-MM-dd'T'HH:mm:ss"));
	}

	/**
	 * @param path The YML path to message from messages.yml file
	 *
	 * @return The colored message or empty string if invalid path provided.
	 */
	@NotNull
	public String msg(@NotNull final String path) {
		return Optional.of(path)
				.map(messages::getString)
				.map(text -> ChatColor.translateAlternateColorCodes('&', text))
				.orElse("");
	}

	/**
	 * @param message The message key to lookup in commands.yml file.
	 *
	 * @return The colored message.
	 */
	@NotNull
	public String msg(@NotNull final M.Message message) {
		final StringBuilder result = new StringBuilder();
		if (message.isError()) {
			result.append(misc("prefix-error"));
		}
		return ChatColor.translateAlternateColorCodes('&', result.append(commands.getString(message.toString())).toString());
	}

	/**
	 * @param name The name of the misc config value to read from the commands.yml file.
	 *
	 * @return The colored text.
	 */
	@NotNull
	public String misc(@NotNull final String name) {
		return Optional.of(name)
				.map(text -> "misc." + text)
				.map(commands::getString)
				.map(text -> ChatColor.translateAlternateColorCodes('&', text))
				.orElse("");
	}

	/**
	 * @return The {@link Optional} wrapper of database connection.
	 */
	@NotNull
	public Optional<Connection> getConnection() {
		return Optional.ofNullable(connection);
	}

	/**
	 * @return The {@link WorldEditPlugin} instance.
	 */
	@NotNull
	public WorldEditPlugin getWorldEdit() {
		return getPlugin(WorldEditPlugin.class);
	}

	/**
	 * @param eventSound The event that may produce sound to its executor player.
	 *
	 * @return The {@link Optional} wrapper of matching {@link Sound} found in config.yml.
	 */
	@NotNull
	public Optional<Sound> getSound(@NotNull final EventSound eventSound) {
		return Optional.of(getConfig())
				.map(configuration -> configuration.getString("sound." + eventSound.name().toLowerCase(), "NONE"))
				.filter(sound -> !sound.equals("NONE"))
				.map(Sound::valueOf);
	}

	/**
	 * Runs the {@link ParkourPlugin#updateSign(FetchParkourBestScoreTask.Result)} in a new, synchronous thread.
	 *
	 * @param result The score to fill the sign with.
	 */
	public void updateSyncSign(@NotNull final FetchParkourBestScoreTask.Result result) {
		getServer().getScheduler().scheduleSyncDelayedTask(this, () -> updateSign(result));
	}

	/**
	 * Updates the sign in the same thread.
	 *
	 * @param result The score to fill the sign with.
	 */
	public void updateSign(@NotNull final FetchParkourBestScoreTask.Result result) {
		updateSign(result.game(), line -> replace(String.valueOf(line), result));
	}

	/**
	 * Updates synchronously the matching record sign block of certain parkour.
	 *
	 * @param parkourGame The game which sign needs to be updated synchronously.
	 */
	public void updateSyncSign(@NotNull final ParkourGame parkourGame) {
		getServer().getScheduler().scheduleSyncDelayedTask(this, () -> updateSign(parkourGame));
	}

	/**
	 * Updates the matching record sign block of certain parkour.
	 *
	 * @param parkourGame The game which sign needs to be updated.
	 */
	public void updateSign(@NotNull final ParkourGame parkourGame) {
		updateSign(parkourGame, this::replace);
	}

	/**
	 * @return The parkour exit item.
	 */
	public ItemStack getExitItem() {
		return exitItem;
	}

	/**
	 * @return The hiding players item.
	 */
	public ItemStack getHidingItem() {
		return hidingItem;
	}

	/**
	 * @param time Time in milliseconds.
	 *
	 * @return The time formatted to 12:34.56 (12 minutes, 34 seconds, 56 millis).
	 */
	@NotNull
	public String formatTime(final double time) {
		final int minutes = (int) time / 60;
		final int seconds = (int) time % 60;
		final int milliseconds = (int) Math.round((time % 1) * 100);
		return String.format("%02d:%02d.%02d", minutes, seconds, milliseconds);
	}

	/**
	 * Formats the given amount to configured format.
	 *
	 * @param money The money to be formatted.
	 *
	 * @return The String representation of formatted amount.
	 */
	@NotNull
	public String formatMoney(final double money) {
		return decimalFormat.format(money);
	}

	/**
	 * Formats the given coord to {@code "%.2f"} format.
	 *
	 * @param coord The coord to be formatted.
	 *
	 * @return The String representation of formatted coord.
	 */
	@NotNull
	public String formatCoord(final double coord) {
		return String.format("%.2f", coord);
	}

	/**
	 * Formats all the medals' display names and joins them with the given separator.
	 *
	 * @param separator The separator to join medals' display names.
	 *
	 * @return The String representation of all the medals' display names joined with the separator.
	 */
	@NotNull
	public String getFormattedMedals(@NotNull final String separator) {
		return getMedals().stream()
				.map(Medal::displayName)
				.collect(Collectors.joining(separator));
	}

	/**
	 * @return The {@link MedalRequirement} read from config.yml. {@link MedalRequirement#ALL} if invalid or no value
	 * 		present.
	 */
	@NotNull
	public MedalRequirement getMedalRequirement() {
		final String medalRequirement = getConfig().getString("medal-requirement", "ALL");
		try {
			return MedalRequirement.valueOf(medalRequirement);
		} catch (final IllegalArgumentException ex) {
			System.out.println("Provided invalid medal requirement: " + medalRequirement);
		}
		return MedalRequirement.ALL;
	}

	/**
	 * Removes all invisible armor stands assigned to the passed {@link ParkourGame}.
	 *
	 * @param parkourGame The owning game of the armor stands.
	 */
	public void hideCheckpoints(@NotNull final ParkourGame parkourGame) {
		indicators.stream()
				.filter(armorStand -> armorStand.getPersistentDataContainer()
						.getOrDefault(new NamespacedKey(this, Constants.PARKOUR), PersistentDataType.STRING, "")
						.equals(parkourGame.getName()))
				.forEach(Entity::remove);
	}

	/**
	 * Spawns and stores all required invisible armor stands that will indicate where the checkpoints of the passed
	 * {@link ParkourGame} are.
	 *
	 * @param parkourGame The game to show the checkpoints indicators of.
	 */
	public void showCheckpoints(@NotNull final ParkourGame parkourGame) {
		final List<Checkpoint> checkpoints = parkourGame.getCheckpoints();
		if (checkpoints.isEmpty()) {
			return;
		}
		createArmorStand(parkourGame, checkpoints.get(0).getLocation(), "SPAWN");
		for (int i = 1; i < checkpoints.size(); i++) {
			createArmorStand(parkourGame, checkpoints.get(i).getLocation(), String.valueOf(i));
		}
	}

	/**
	 * Creates and stores a single invisible invulnerable armor stand assigned to the passed {@link ParkourGame}.
	 *
	 * @param parkourGame The game which checkpoint will be assigned to.
	 * @param location The target location where the armor stand will appear.
	 * @param text The indicator (armor stand name) text.
	 */
	public void createArmorStand(@NotNull final ParkourGame parkourGame, @NotNull final Location location,
								 @NotNull final String text) {
		final World world = location.getWorld();
		if (world == null) {
			return;
		}
		final ArmorStand armorStand = (ArmorStand) world.spawnEntity(location, EntityType.ARMOR_STAND);
		armorStand.setGravity(false);
		armorStand.setCustomName(text);
		armorStand.setCustomNameVisible(true);
		armorStand.setVisible(false);
		armorStand.addEquipmentLock(EquipmentSlot.CHEST, ArmorStand.LockType.ADDING_OR_CHANGING);
		armorStand.getPersistentDataContainer()
				.set(new NamespacedKey(this, Constants.PARKOUR), PersistentDataType.STRING, parkourGame.getName());
		indicators.add(armorStand);
	}

	/**
	 * Moves the found armor stand to the new location.
	 *
	 * @param parkourGame The owning game which armor stand should be moved.
	 * @param location The new location of the armor stand.
	 * @param text Current armor stand name to precisely select the correct one.
	 */
	public void moveArmorStand(@NotNull final ParkourGame parkourGame, @NotNull final Location location,
							   @NotNull final String text) {
		indicators.stream()
				.filter(armorStand -> armorStand.getPersistentDataContainer()
						.getOrDefault(new NamespacedKey(this, Constants.PARKOUR), PersistentDataType.STRING, "")
						.equals(parkourGame.getName()))
				.filter(armorStand -> text.equals(armorStand.getCustomName()))
				.findAny()
				.ifPresent(armorStand -> armorStand.teleport(location));
	}

	/**
	 * Loads the resource using the built-in class loader from {@link JavaPlugin#getClassLoader()}.
	 *
	 * @param filename The name of the resource file to be opened and read.
	 *
	 * @return The String content of opened resource file.
	 *
	 * @throws IOException If anything with the file went wrong.
	 */
	@NotNull
	public String load(@NotNull final String filename) throws IOException {
		try (final InputStream inputStream = getClassLoader().getResourceAsStream(filename)) {
			if (inputStream == null) {
				throw new IOException("Cannot open resource as stream: " + filename);
			}
			return new String(inputStream.readAllBytes());
		}
	}

	/**
	 * Creates the Scoreboard for the player and the game and adds it to the player.
	 *
	 * @param parkourPlayer The player which data should appear on the scoreboard and who will see the scoreboard.
	 * @param parkourGame The game the player is in.
	 */
	public void generateScoreboard(@NotNull final ParkourPlayer parkourPlayer, @NotNull final ParkourGame parkourGame) {
		final Runnable task = new FetchParkourPlayerScoreTask(this, parkourGame, parkourPlayer, result -> {
			final String medalDisplayName = Optional.of(result)
					.map(FetchParkourPlayerScoreTask.Result::playerTime)
					.map(parkourGame::getResult)
					.map(ParkourGame.Result::medal)
					.map(Medal::displayName)
					.orElse("none");
			final UnaryOperator<String> fillPlaceholders = text -> text
					.replace("%PARKOUR%", parkourGame.getName())
					.replace("%BEST_TIME%", formatTime(result.parkourTIme()))
					.replace("%PLAYER_TIME%", formatTime(result.playerTime()))
					.replace("%MEDAL%", medalDisplayName)
					.replace("%COUNT%", String.valueOf(result.count()))
					.replace("%TYPE%", parkourGame.getOptions().getType().name())
					.replace("%LAST_DATE%", result.lastRun().format(getScoreboardDateTimeFormatter()))
					.replace("%PLAYER%", parkourPlayer.getPlayer().getDisplayName());
			final String name = fillPlaceholders.apply(getScoreboardDisplayName());
			final List<String> scoreboardPattern = getScoreboardPattern()
					.stream()
					.map(fillPlaceholders)
					.toList();
			getServer().getScheduler().runTask(this, () ->
					parkourPlayer.getPlayer().setScoreboard(build(name, scoreboardPattern)));
		});
		getServer().getScheduler().runTaskAsynchronously(this, task);
	}

	/**
	 * Check whether the edit lock is enabled in the config.
	 *
	 * @return The value from config.
	 */
	public boolean isEditLockActive() {
		return getConfig().getBoolean("edit-lock", true);
	}

	/**
	 * @return The {@link Optional} wrapper of {@link FinancialProvider} if present, {@link Optional#empty()} otherwise.
	 */
	@NotNull
	public Optional<FinancialProvider> getFinancialProvider() {
		return Optional.ofNullable(financialProvider);
	}

	/**
	 * @return The {@link Optional} wrapper of {@link RankProvider} if present, {@link Optional#empty()} otherwise.
	 */
	@NotNull
	public Optional<RankProvider> getRankProvider() {
		return Optional.ofNullable(rankProvider);
	}

	// =============== PRIVATE =============== //

	@NotNull
	private List<Medal> loadMedals() {
		final ConfigurationSection medalsSection = getConfig().getConfigurationSection(Constants.KEY_MEDAL);
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
							return new Medal(key, display, importance);
						})
						.orElse(null))
				.filter(Objects::nonNull)
				.toList();
	}

	private void setupConfigFiles() {
		saveDefaultConfig();
		saveResource("commands.yml", false);
		saveResource("messages.yml", false);
		saveResource("inventory.yml", false);
		saveResource("scoreboard.yml", false);
		try {
			commands.load(new File(getDataFolder(), "commands.yml"));
			messages.load(new File(getDataFolder(), "messages.yml"));
			inventory.load(new File(getDataFolder(), "inventory.yml"));
			scoreboard.load(new File(getDataFolder(), "scoreboard.yml"));
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
				.toList();
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

		command.addTypeMapper(ParkourGame.class, parkourManager::getParkour, FallbackConstants.ON_NULL);
		command.addTypeMapper(PotionEffectType.class, PotionEffectType::getByName, FallbackConstants.ON_NULL);
		command.addTypeMapper(DyeColor.class, this::getDyeColor, FallbackConstants.ON_NULL);
		command.addTypeMapper(Medal.class, name -> medals.stream()
						.filter(medal -> medal.name().equals(name))
						.findAny()
						.orElse(null),
				FallbackConstants.ON_NULL);
		command.addEnumMapper(MedalSetupOption.class);
		command.addEnumMapper(SimpleLever.class);

		command.addTypeCompleter(ParkourGame.class, () -> parkourManager.getAllGames().stream()
				.map(ParkourGame::getName)
				.toList());

		command.addEnumCompleter(SimpleLever.class);
		command.addTypeCompleter(PotionEffectType.class, Data.ALLOWED_EFFECTS.stream()
				.map(PotionEffectType::getName)
				.toList());
		command.addTypeCompleter(Medal.class, medals.stream()
				.map(Medal::name)
				.toList());
		command.addTypeCompleter(boolean.class, Arrays.asList(Boolean.FALSE.toString(), Boolean.TRUE.toString()));
		command.addEnumCompleter(MedalSetupOption.class);
		command.addEnumCompleter(DyeColor.class);
	}

	@Nullable
	private DyeColor getDyeColor(final String colorName) {
		try {
			return DyeColor.valueOf(colorName);
		} catch (final IllegalArgumentException ex) {
			return null;
		}
	}

	private void setupDatabase() {
		final boolean databaseEnabled = getConfig().getBoolean("database.enabled", false);
		if (!databaseEnabled) {
			getLogger().warning("Database is disabled. In order to save records, enable it in config.");
			return;
		}
		try {
			connection = createConnection();
			initDatabase();
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
		final String database = getConfig().getString("database.dbname", "ats_parkour");
		return DriverManager.getConnection(String.format("jdbc:mysql://%s/%s?allowPublicKeyRetrieval=true&autoReconnect=true&useSSL=false", url, database), user, pass);
	}

	private void initDatabase() {
		getConnection().ifPresent(conn -> {
			try (final PreparedStatement statement = conn.prepareStatement(load("sql/setup.sql"))) {
				statement.execute();
			} catch (final SQLException | IOException e) {
				getLogger().log(Level.SEVERE, "An error occurred when trying to execute query", e);
			}
			getLogger().info("§2Database setup complete.");
		});
	}

	private void updateSign(@NotNull final ParkourGame parkourGame,
							@NotNull final UnaryOperator<String> replaceFunction) {
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
			final JsonWriter jsonWriter = gson.newJsonWriter(new PrintWriter(target));
			jsonWriter.setIndent("\t");
			gson.toJson(parkourManager.getSetting(), ParkourManager.ParkourSetting.class, jsonWriter);
			jsonWriter.close();
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
		helpDescription.put(Constants.MEDAL, msg(M.Option.MEDAL.helpMessage));
	}

	@NotNull
	private String replace(@NotNull final String source, @NotNull final FetchParkourBestScoreTask.Result result) {
		final String playerName = Optional.of(result)
				.map(FetchParkourBestScoreTask.Result::uuid)
				.map(getServer()::getOfflinePlayer)
				.map(OfflinePlayer::getName)
				.orElse(Constants.PLACEHOLDER_NO_RECORD);
		return source.replace(Constants.NICK, playerName)
				.replace(Constants.PERSONAL_TIME, formatTime(result.time()));
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
		final DecimalFormat format = new DecimalFormat(economySection.getString("pattern", "+###,##0.00¤;-###,##0.00¤"), decimalFormatSymbols);
		format.setGroupingSize(economySection.getInt("group-size", 3));
		return format;
	}

	@NotNull
	private Scoreboard build(final String name, final List<String> text) {
		final ScoreboardManager scoreboardManager = Bukkit.getScoreboardManager();
		if (scoreboardManager == null) {
			throw new IllegalArgumentException("Something went wrong with scoreboard manager");
		}
		final Scoreboard board = scoreboardManager.getNewScoreboard();
		final Objective objective = board.registerNewObjective(Constants.PARKOUR, Criteria.DUMMY, Constants.PARKOUR);
		objective.setDisplaySlot(DisplaySlot.SIDEBAR);
		objective.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
		for (int i = 0; i < text.size(); i++) {
			final Score score = objective.getScore(ChatColor.translateAlternateColorCodes('&', text.get(i)));
			score.setScore(text.size() - i);
		}
		return board;
	}
}
