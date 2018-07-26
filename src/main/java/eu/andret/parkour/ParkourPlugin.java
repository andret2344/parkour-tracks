package eu.andret.parkour;

import eu.andret.parkour.parkour.Parkour;
import eu.andret.parkour.parkour.ParkourGame;
import eu.andret.parkour.parkour.ParkourManager;
import eu.andret.parkour.region.AbstractRegion;
import eu.andret.parkour.tasks.KeepConnection;
import eu.andret.parkour.util.Data;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

public class ParkourPlugin extends JavaPlugin {
    private FileConfiguration games;
    private Connection conn;
    private ItemStack exit;
    private ParkourListeners listeners;
    private boolean LobbyCoins = true;
    private boolean GlobalPermissions = true;
    private final Map<String, String> messages = new LinkedHashMap<>();
    private final YamlConfiguration msgs = new YamlConfiguration();
    public final String URL = getConfig().getString("connection.url");
    public final String USER = getConfig().getString("connection.user");
    public final String PASS = getConfig().getString("connection.pass");
    public final String DATABASE = getConfig().getString("connection.database");

    @Override
    public void onEnable() {
        if (getServer().getPluginManager().getPlugin("WorldEdit") == null) {
            System.out.print("[ParkourPlugin] Could not find WorldEdit plugin. Disabling.");
            setEnabled(false);
            return;
        }
        if (getServer().getPluginManager().getPlugin("LobbyCoins") == null) {
            System.out.print("[ParkourPlugin] Could not find LobbyCoins plugin. I will not administrate payments.");
            LobbyCoins = false;
        }
        if (getServer().getPluginManager().getPlugin("GlobalPermissions") == null) {
            System.out.print("[ParkourPlugin] Could not find GlobalPermissions plugin. Every vip-only parkours are unavailable.");
            for (ParkourGame p : ParkourManager.getAllGames()) {
                if (p.getOptions().isVip()) {
                    p.getOptions().setAvailable(false);
                }
            }
            GlobalPermissions = false;
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
            msgs.load(new File(getDataFolder().getAbsolutePath() + File.separator + "messages.yml"));
            load();
            connect();
        } catch (Exception ex) {
            ex.printStackTrace();
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
        getServer().getScheduler().scheduleSyncRepeatingTask(this, new KeepConnection(), 36_000, 36_000);
    }

    @Override
    public void onDisable() {
        save();
        try {
            Field f = ParkourManager.class.getDeclaredField("games");
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            f.setAccessible(true);
            modifiersField.setAccessible(true);
            modifiersField.setInt(f, f.getModifiers() & ~Modifier.FINAL);
            f.set(eu.andret.parkour.parkour.Parkour.class, new ArrayList<eu.andret.parkour.parkour.Parkour>());
            modifiersField.setInt(f, f.getModifiers() & Modifier.FINAL);
            f.setAccessible(false);
            modifiersField.setAccessible(false);
            if (conn != null) {
                conn.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Bukkit.getScheduler().cancelAllTasks();
    }

    private void createDoors() {
        exit = new ItemStack(Material.IRON_DOOR);
        ItemMeta im = exit.getItemMeta();
        im.setDisplayName("§r" + ChatColor.translateAlternateColorCodes('&', getConfig().getString("door.name")));
        exit.setItemMeta(im);
    }

    private void connect() throws SQLException {
        conn = DriverManager.getConnection("jdbc:mysql://" + URL, USER, PASS);
        Statement stat = conn.createStatement();
        stat.execute("CREATE DATABASE IF NOT EXISTS `" + DATABASE + "`;");
        stat.execute("USE " + DATABASE + ";");
        stat.execute("CREATE TABLE IF NOT EXISTS " + Data.TABLE_RECORDS + "(id INT PRIMARY KEY AUTO_INCREMENT, date DATETIME, nick VARCHAR(64), " + "parkour VARCHAR(64), time FLOAT, count INT, earned INT, xp INT);");
    }

    public String msg(String path, boolean err) {
        String here;
        if (msgs.getString("player." + path) != null) {
            here = "player.";
        } else if (msgs.getString("admin." + path) != null) {
            here = "admin.";
        } else {
            throw new NullPointerException("Invalid message");
        }
        String result = "";
        if (err) {
            result += msgs.getString(here + "errorMsg");
        }
        return (result + msgs.getString(here + path)).replace('&', '§');
    }

    private boolean save() {
        try {
            for (ParkourGame pk : ParkourManager.getAllGames()) {
                File path = new File(getDataFolder().getAbsolutePath() + "/games");
                File file = new File(path.getAbsolutePath() + "/" + pk.getName() + ".yml");
                if (!path.exists()) {
                    if (!path.mkdirs()) {
                        return false;
                    }
                }
                if (!file.exists()) {
                    if (!file.createNewFile()) {
                        return false;
                    }
                }
                games = new YamlConfiguration();
                games.set("world", pk.getWorld().getName());
                for (Entry<String, Object> entry : pk.toYmlStructure().entrySet()) {
                    for (Entry<String, Object> e : ((Map<String, Object>) entry.getValue()).entrySet()) {
                        games.set("regions." + entry.getKey() + "." + e.getKey(), e.getValue());
                    }
                }
                games.set("started", pk.isRunning());
                games.set("authors", pk.getAuthors());
                games.set("options", pk.getOptions().toYmlStructure());
                games.save(file);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
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

    private void load() throws Exception {
        String w = getConfig().getString("lobby.world");
        if (!w.equals("-1")) {
            World world = Bukkit.getWorld(w);
            if (world == null) {
                new WorldCreator(w).createWorld();
            }
            Location l = new Location(world, getConfig().getDouble("lobby.x"), getConfig().getDouble("lobby.y"), getConfig().getDouble("lobby.z"));
            l.setPitch((float) getConfig().getDouble("lobby.pitch"));
            l.setYaw((float) getConfig().getDouble("lobby.yaw"));
            ParkourManager.setLobbyLocation(l);
        }
        File folder = new File(getDataFolder().getAbsolutePath() + "/games");
        if (!folder.exists() || folder.listFiles().length == 0) {
            return;
        }
        for (File file : folder.listFiles()) {
            Map<String, Object> map = new TreeMap<>();
            if (file.isFile() && file.getName().endsWith("yml")) {
                games = new YamlConfiguration();
                games.load(file);
                World world = new WorldCreator(games.getString("world")).createWorld();
                Parkour pk = new Parkour(file.getName().substring(0, file.getName().lastIndexOf('.')), null, world);
                System.out.print("[ParkourPlugin] Trying to load parkour \"" + pk.getName() + "\"");
                ConfigurationSection cs = games.getConfigurationSection("regions");
                if (cs != null) {
                    for (String key : cs.getKeys(false)) {
                        Map<String, Object> tmp = new TreeMap<>();
                        for (String k : cs.getConfigurationSection(key).getKeys(false)) {
                            tmp.put(k, cs.getConfigurationSection(key).get(k));
                        }
                        map.put(key, tmp);
                    }
                    try {
                        pk.fromYmlStructure(map);
                    } catch (Exception ex) {
                        System.out.print("[ParkourPlugin] Unable to load \"" + pk.getName() + "\" parkour.");
                        ex.printStackTrace();
                    }
                }
                if (games.getBoolean("started")) {
                    pk.start();
                }
                cs = games.getConfigurationSection("options");
                if (cs != null) {
                    Map<String, Object> options = new HashMap<>();
                    for (String s : cs.getKeys(false)) {
                        options.put(s, cs.get(s));
                    }
                    pk.getOptions().fromYmlStructure(options);
                }
                // cs = games.getConfigurationSection("authors");
                // //cs.getStringList("authors");
                // if (cs!=null) {
                // for (String s:cs.getKeys(false)) {
                // pk.addAuthor(s);
                // }
                // }
                System.out.print("[ParkourPlugin] Loaded parkour \"" + pk.getName() + "\" in world \"" + world.getName() + "\"");
            }
        }
        System.out.print("[ParkourPlugin] Successfully loaded all parkours.");
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

    public Map<String, String> getMessages() {
        return messages;
    }

    public ItemStack getExit() {
        return exit;
    }

    public ParkourListeners getListeners() {
        return listeners;
    }

    public boolean isLobbyCoins() {
        return LobbyCoins;
    }

    public boolean isVip(OfflinePlayer player) {
        // String rank =
        // pl.mclobby.global.permissions.GlobalPermissions.getInstance().getPlayerGroup(player);
        // return rank!=null && !rank.equalsIgnoreCase("gracz");
        return true;
    }

    public boolean isGlobalPermissions() {
        return GlobalPermissions;
    }
}
