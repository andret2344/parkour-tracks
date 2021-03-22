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
import eu.andret.ats.parkour.tasks.KeepConnection;
import eu.andret.ats.parkour.util.Data;
import eu.andret.ats.parkour.util.JSONSerializer;
import lombok.Getter;
import lombok.Setter;
import org.bstats.bukkit.Metrics;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ParkourPlugin extends JavaPlugin {
	private Connection conn;
	private ItemStack exit;
	private ParkourListeners listeners;
	@Setter
	private FinancialProvider financialProvider;
	@Setter
	private RankProvider rankProvider;
	@Getter
	private final Map<String, String> messages = new LinkedHashMap<>();
	private final YamlConfiguration yamlConfiguration = new YamlConfiguration();
	private final String url = getConfig().getString("connection.url");
	private final String user = getConfig().getString("connection.user");
	private final String pass = getConfig().getString("connection.pass");
	private final String database = getConfig().getString("connection.dbname");

	private final JSONSerializer jsonSerializer = new JSONSerializer(this);

	@Override
	public void onEnable() {
		if (getServer().getPluginManager().getPlugin("WorldEdit") == null) {
			getServer().getLogger().log(Level.SEVERE, "Could not find WorldEdit plugin. Disabling.");
			setEnabled(false);
			return;
		}
		listeners = new ParkourListeners(this);
		getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
		if (!new File(getDataFolder(), "config.yml").exists()) {
			saveDefaultConfig();
		}
		saveResource("scoreboard.yml", false);
		saveResource("messages.yml", false);
		createDoors();
		getServer().getPluginManager().registerEvents(listeners, this);
		final AnnotatedCommand command = CommandManager.registerCommand(ParkourCommand.class, this);
		command.setOnInsufficientPermissionsListener(sender -> sender.sendMessage(msg("noPerms", true)));
		command.setOnUnknownSubCommandExecutionListener(sender -> sender.sendMessage(msg("wrongArg", true)));
		command.addArgumentMapper("parkourGame", ParkourGame.class, ParkourManager::getParkour, Fallback.ON_NULL);
		command.addArgumentMapper("potion", PotionEffectType.class, PotionEffectType::getByName, Fallback.ON_NULL);
		command.addTypeCompleter(ParkourGame.class, () -> ParkourManager.getAllGames().stream()
				.map(ParkourGame::getName)
				.collect(Collectors.toList()));
		command.addTypeCompleter(boolean.class, Arrays.asList("false", "true"));
		command.addTypeCompleter(PotionEffectType.class, () -> Data.ALLOWED_EFFECTS.stream()
				.map(PotionEffectType::getName)
				.collect(Collectors.toList()));
		try {
			yamlConfiguration.load(new File(getDataFolder().getAbsolutePath(), "messages.yml"));
			load();
			connect();
		} catch (final Exception ex) {
			ex.printStackTrace();
			getServer().getLogger().throwing(getClass().getName(), "onEnable", ex);
		}
		generate();
		ParkourManager.getAllGames().forEach(p -> getServer().getOnlinePlayers().stream()
				.filter(pl -> p.getAllRegions().stream().filter(Objects::nonNull).anyMatch(r -> r.contains(pl.getLocation())))
				.forEach(p::addPlayer));
		getServer().getScheduler().scheduleSyncRepeatingTask(this, new KeepConnection(this), 36_000, 36_000);
		new Metrics(this, 10700);
	}

	@Override
	public void onDisable() {
		try {
			if (!save()) {
				getLogger().log(Level.WARNING, "Problem with saving");
			}
		} catch (final IOException e) {
			e.printStackTrace();
		}
		getServer().getScheduler().cancelTasks(this);
	}

	private void createDoors() {
		exit = new ItemStack(Material.IRON_DOOR);
		Optional.ofNullable(exit.getItemMeta())
				.stream()
				.peek(im -> im.setDisplayName("§r" + ChatColor.translateAlternateColorCodes('&', getConfig().getString("door.name"))))
				.findAny()
				.ifPresent(exit::setItemMeta);
	}

	private void connect() throws SQLException {
		conn = DriverManager.getConnection("jdbc:mysql://" + url + "?autoReconnect=true&useSSL=false", user, pass);
		try (final Statement stat = conn.createStatement()) {
			stat.execute("CREATE DATABASE IF NOT EXISTS `" + database + "`;");
			stat.execute("USE " + database + ";");
			stat.execute("CREATE TABLE IF NOT EXISTS ats_parkour_records(id INT PRIMARY KEY AUTO_INCREMENT, date DATETIME, nick VARCHAR(64), parkour VARCHAR(64), time FLOAT, count INT);");
		}
	}

	public String msg(final String path, final boolean err) {
		final String here;
		if (yamlConfiguration.getString("player." + path) != null) {
			here = "player.";
		} else if (yamlConfiguration.getString("admin." + path) != null) {
			here = "admin.";
		} else {
			throw new NullPointerException("Invalid message: " + path + " (should be error: " + err + ")");
		}
		String result = "";
		if (err) {
			result += yamlConfiguration.getString(here + "errorMsg");
		}
		return (result + yamlConfiguration.getString(here + path)).replace('&', '\u00A7');
	}

	private boolean save() throws IOException {
		for (final ParkourGame parkourGame : ParkourManager.getAllGames()) {
			final File path = new File(getDataFolder(), "games");
			if (!path.exists() && !path.mkdirs()) {
				return false;
			}
			final File file = new File(path.getAbsolutePath(), parkourGame.getName() + ".json");
			if (!file.exists() && !file.createNewFile()) {
				return false;
			}
			final PrintWriter printWriter = new PrintWriter(file);
			printWriter.write(jsonSerializer.writeParkourGame(parkourGame).toString(4));
			printWriter.close();
		}
		return true;
	}

	public void saveLobbyLoc(final Location l) {
		getConfig().set("lobby.world", l.getWorld().getName());
		getConfig().set("lobby.x", l.getX());
		getConfig().set("lobby.y", l.getY());
		getConfig().set("lobby.z", l.getZ());
		getConfig().set("lobby.yaw", l.getYaw());
		getConfig().set("lobby.pitch", l.getPitch());
		saveConfig();
	}

	private void load() {
		final String worldName = getConfig().getString("lobby.world");
		if (!"-1".equals(worldName) && worldName != null) {
			final World world = getServer().getWorld(worldName);
			if (world == null) {
				return;
			}
			final Location location = new Location(world, getConfig().getDouble("lobby.x"), getConfig().getDouble("lobby.y"), getConfig().getDouble("lobby.z"));
			location.setPitch((float) getConfig().getDouble("lobby.pitch"));
			location.setYaw((float) getConfig().getDouble("lobby.yaw"));
			ParkourManager.setLobbyLocation(location);
			System.out.println("[atsParkour] Loaded lobby location successfully!");
		}
		final File folder = new File(getDataFolder(), "games");
		if (!folder.exists() || folder.listFiles() == null) {
			return;
		}
		Stream.of(folder.listFiles())
				.filter(File::isFile)
				.filter(file -> file.getName().endsWith("json"))
				.forEach(file -> {
					final String name = file.getName().substring(0, file.getName().lastIndexOf('.'));
					getServer().getLogger().log(Level.INFO, "[atsParkour] Loading parkour \"{0}\"", name);
					try (final Reader reader = new FileReader(file)) {
						final JSONTokener jsonTokener = new JSONTokener(reader);
						final JSONObject jsonObject = new JSONObject(jsonTokener);
						final ParkourGame parkourGame = jsonSerializer.readParkourGame(name, jsonObject);
						ParkourManager.addParkour(parkourGame);
						getServer().getLogger().log(Level.INFO, "[atsParkour] Loaded parkour \"{0}\" in world \"{1}\"", new String[]{parkourGame.getName(), parkourGame.getWorld().getName()});
					} catch (final IOException e) {
						e.printStackTrace();
					}
				});
		getServer().getLogger().log(Level.INFO, "[atsParkour] Successfully loaded all parkours.");
	}

	public Connection getConnection() {
		return conn;
	}

	public Optional<FinancialProvider> getFinancialProvider() {
		return Optional.ofNullable(financialProvider);
	}

	public Optional<RankProvider> getRankProvider() {
		return Optional.ofNullable(rankProvider);
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
		messages.put("fair", msg("cmdFair", false));
		messages.put("enabled", msg("cmdEnabled", false));
		messages.put("modifyInventory|eq", msg("cmdModifyInventory", false));
		messages.put("bestRecord|br", msg("cmdBestRecord", false));
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

	ItemStack getExit() {
		return exit;
	}

	public ParkourListeners getListeners() {
		return listeners;
	}

	public WorldEditPlugin getWorldEdit() {
		return (WorldEditPlugin) getServer().getPluginManager().getPlugin("WorldEdit");
	}
}
