/*
 * Copyright Andret (c) 2019. Copying and modifying allowed only keeping git link reference.
 */
package eu.andret.parkour;

import eu.andret.parkour.parkour.ParkourGame;
import eu.andret.parkour.parkour.ParkourManager;
import eu.andret.parkour.region.AbstractRegion;
import eu.andret.parkour.tasks.KeepConnection;
import eu.andret.parkour.util.Data;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;

public class ParkourPlugin extends JavaPlugin {
	private Connection conn;
	private ItemStack exit;
	private ParkourListeners listeners;
	@Getter
	private final Map<String, String> messages = new LinkedHashMap<>();
	private final YamlConfiguration yamlConfiguration = new YamlConfiguration();
	private final String url = getConfig().getString("connection.url");
	private final String user = getConfig().getString("connection.user");
	private final String pass = getConfig().getString("connection.pass");
	private final String database = getConfig().getString("connection.database");

	@Override
	public void onEnable() {
		if (getServer().getPluginManager().getPlugin("WorldEdit") == null) {
			getServer().getLogger().log(Level.SEVERE, "Could not find WorldEdit plugin. Disabling.");
			setEnabled(false);
			return;
		}
		listeners = new ParkourListeners(this);
		getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
		if (!new File(getDataFolder().getAbsolutePath() + File.separator + "config.yml").exists()) {
			saveDefaultConfig();
		}
		saveResource("scoreboard.yml", false);
		saveResource("messages.yml", false);
		createDoors();
		getServer().getPluginManager().registerEvents(listeners, this);
		getCommand("parkour").setExecutor(new ParkourCommand(this));
		try {
			yamlConfiguration.load(new File(getDataFolder().getAbsolutePath() + File.separator + "messages.yml"));
			load();
			connect();
		} catch (Exception ex) {
			getServer().getLogger().throwing(getClass().getName(), "onEnable", ex);
		}
		generate();
		for (ParkourGame p : ParkourManager.getAllGames()) {
			for (Player pl : Bukkit.getServer().getOnlinePlayers()) {
				for (AbstractRegion r : p.getAllRegions()) {
					if (r.contains(pl.getLocation())) {
						p.addPlayer(pl);
					}
				}
			}
		}
		getServer().getScheduler().scheduleSyncRepeatingTask(this, new KeepConnection(this), 36_000, 36_000);
	}

	@Override
	public void onDisable() {
		try {
			if (!save()) {
				getLogger().log(Level.WARNING, "Problem with saving");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		Bukkit.getScheduler().cancelTasks(this);
	}

	private void createDoors() {
		exit = new ItemStack(Material.IRON_DOOR);
		Optional.ofNullable(exit.getItemMeta())
				.stream()
				.peek(im -> im.setDisplayName("§r" + ChatColor.translateAlternateColorCodes('&',
						getConfig().getString("door.name"))))
				.findAny()
				.ifPresent(exit::setItemMeta);
	}

	private void connect() throws SQLException {
		conn = DriverManager.getConnection("jdbc:mysql://" + url, user, pass);
		try (Statement stat = conn.createStatement()) {
			stat.execute("CREATE DATABASE IF NOT EXISTS `" + database + "`;");
			stat.execute("USE " + database + ";");
			stat.execute(String.format("CREATE TABLE IF NOT EXISTS %s(id INT PRIMARY KEY AUTO_INCREMENT, date DATETIME, nick VARCHAR(64), parkour VARCHAR(64), time FLOAT, count INT, earned INT, xp INT);", Data.TABLE_RECORDS));
		}
	}

	public String msg(String path, boolean err) {
		String here;
		if (yamlConfiguration.getString("player." + path) != null) {
			here = "player.";
		} else if (yamlConfiguration.getString("admin." + path) != null) {
			here = "admin.";
		} else {
			throw new NullPointerException("Invalid message");
		}
		String result = "";
		if (err) {
			result += yamlConfiguration.getString(here + "errorMsg");
		}
		return (result + yamlConfiguration.getString(here + path)).replace('&', '\u00A7');
	}

	private boolean save() throws IOException {
		System.out.println(ParkourManager.getAllGames());
		for (ParkourGame pk : ParkourManager.getAllGames()) {
			System.out.println(pk);
			File path = new File(getDataFolder().getAbsolutePath() + "/games");
			if (!path.exists() && !path.mkdirs()) {
				return false;
			}
			File file = new File(path.getAbsolutePath(), pk.getName() + ".json");
			if (!file.exists() && !file.createNewFile()) {
				return false;
			}
			PrintWriter pw = new PrintWriter(file);
			System.out.println("File exists");
			JSONObject game = new JSONObject();
			game.put("world", pk.getWorld().getName());
			game.put("parkour", pk.toJSON());
			game.put("started", pk.isRunning());
			game.put("authors", pk.getAuthors());
			pw.write(game.toString(2));
			System.out.println("Saved");
			pw.close();
		}
		return true;
	}

	public void saveLobbyLoc(Location l) {
		getConfig().set("lobby.world", l.getWorld().getName());
		getConfig().set("lobby.x", l.getX());
		getConfig().set("lobby.y", l.getY());
		getConfig().set("lobby.z", l.getZ());
		getConfig().set("lobby.yaw", l.getYaw());
		getConfig().set("lobby.pitch", l.getPitch());
		saveConfig();
	}

	private void load() {
//		String w = getConfig().getString("lobby.world");
//		if (!w.equals("-1")) {
//			World world = Bukkit.getWorld(w);
//			if (world == null) {
//				new WorldCreator(w).createWorld();
//			}
//			Location l = new Location(world, getConfig().getDouble("lobby.x"), getConfig().getDouble("lobby.y"), getConfig().getDouble("lobby.z"));
//			l.setPitch((float) getConfig().getDouble("lobby.pitch"));
//			l.setYaw((float) getConfig().getDouble("lobby.yaw"));
//			ParkourManager.setLobbyLocation(l);
//		}
//		File folder = new File(getDataFolder().getAbsolutePath() + "/games");
//		if (!folder.exists() || folder.listFiles().length == 0) {
//			return;
//		}
//		for (File file : folder.listFiles()) {
//			Map<String, Object> map = new TreeMap<>();
//			if (file.isFile() && file.getName().endsWith("yml")) {
//				games = new YamlConfiguration();
//				games.load(file);
//				World world = new WorldCreator(games.getString("world")).createWorld();
//				Parkour pk = new Parkour(file.getName().substring(0, file.getName().lastIndexOf('.')), null, world);
//				getServer().getLogger().log(Level.INFO, "[ParkourPlugin] Trying to load parkour \"{0}\"", pk.getName());
//				ConfigurationSection cs = games.getConfigurationSection("regions");
//				if (cs != null) {
//					for (String key : cs.getKeys(false)) {
//						Map<String, Object> tmp = new TreeMap<>();
//						for (String k : cs.getConfigurationSection(key).getKeys(false)) {
//							tmp.put(k, cs.getConfigurationSection(key).get(k));
//						}
//						map.put(key, tmp);
//					}
//					try {
//						pk.fromJSON(map);
//					} catch (Exception ex) {
//						getServer().getLogger().log(Level.SEVERE, "Unable to load \\\"\" + pk.getName() + \"\\\" parkour.");
//						getServer().getLogger().throwing(getClass().getName(), "load", ex);
//					}
//				}
//				if (games.getBoolean("started")) {
//					pk.start();
//				}
//				cs = games.getConfigurationSection("options");
//				if (cs != null) {
//					Map<String, Object> options = new HashMap<>();
//					for (String s : cs.getKeys(false)) {
//						options.put(s, cs.get(s));
//					}
//					pk.getOptions().fromJSON(options);
//				}
//				getServer().getLogger().log(Level.INFO, "Loaded parkour \"{0}\" in world \"{1}\"", new String[]{pk.getName(), world.getName()});
//			}
//		}
//		getServer().getLogger().log(Level.INFO, "Successfully loaded all parkours.");
	}

	public Connection getConnection() {
		return conn;
	}

	private void generate() {
		messages.put("help|?", msg("cmdHelp", false));
		messages.put("lobby", msg("cmdLobby", false));
		messages.put("create|c", msg("cmdCreate", false));
		messages.put("remove|r", msg("cmdRemove", false));
		messages.put("info", msg("cmdInfo", false));
		messages.put("setspawn|ss", msg("cmdSetspawn", false));
		messages.put("recreate|rc", msg("cmdRecreate", false));
		messages.put("start|s", msg("cmdStart", false));
		messages.put("stop", msg("cmdStop", false));
		messages.put("addcheckpoint|ac", msg("cmdAddcheckpoint", false));
		messages.put("setcheckpoint|sc", msg("cmdSetcheckpoint", false));
		messages.put("addwall|aw", msg("cmdAddwall", false));
		messages.put("setwall|sw", msg("cmdSetwall", false));
		messages.put("list|ls", msg("cmdList", false));
		messages.put("ignore|i", msg("cmdIgnore", false));
		messages.put("reload|rl", msg("cmdReload", false));
		messages.put("sprint|sp", msg("cmdSprint", false));
		messages.put("alwaysSpawn|as", msg("cmdAlwaysspawn", false));
		messages.put("price|p", msg("cmdPrice", false));
		messages.put("countingRecords|cr", msg("cmdPrice", false));
		messages.put("allowingDamage|dmg", msg("cmdDamage", false));
		messages.put("effect|e", msg("cmdEffect", false));
		messages.put("boat|b", msg("cmdBoats", false));
		messages.put("fair", msg("cmdFair", false));
		messages.put("enabled", msg("cmdProceedable", false));
		messages.put("modifyInventory|eq", msg("cmdModifyeq", false));
		messages.put("bestrecord|br", msg("cmdBestrecord", false));
		messages.put("teleportblock|tb", msg("cmdTeleportblock", false));
		messages.put("teleport|tp", msg("cmdTeleport", false));
		messages.put("fix", msg("cmdFix", false));
		messages.put("color", msg("cmdColor", false));
		messages.put("difficulty|d", msg("cmdDifficulty", false));
		messages.put("available|a", msg("cmdAvailable", false));
		messages.put("type", msg("cmdType", false));
		messages.put("displayname|dn", msg("cmdDisplayname", false));
		messages.put("authors", msg("cmdAuthors", false));
		messages.put("vip", msg("cmdVip", false));
		messages.put("brozone", msg("cmdBronze", false));
		messages.put("silver", msg("cmdSilver", false));
		messages.put("gold", msg("cmdGold", false));
		messages.put("platinium", msg("cmdPlatinium", false));
	}

	ItemStack getExit() {
		return exit;
	}

	public ParkourListeners getListeners() {
		return listeners;
	}
}
