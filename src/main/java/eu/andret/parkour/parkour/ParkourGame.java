package eu.andret.parkour.parkour;

import com.sk89q.worldedit.regions.CuboidRegion;
import eu.andret.parkour.YmlSerializable;
import eu.andret.parkour.event.game.GameStartEvent;
import eu.andret.parkour.event.game.GameStopEvent;
import eu.andret.parkour.event.player.PlayerJoinGameEvent;
import eu.andret.parkour.event.player.PlayerQuitGameEvent;
import eu.andret.parkour.player.ParkourPlayer;
import eu.andret.parkour.player.PlayerManager;
import eu.andret.parkour.region.AbstractRegion;
import eu.andret.parkour.region.Checkpoint;
import eu.andret.parkour.region.EffectRegion;
import eu.andret.parkour.region.GameRegion;
import eu.andret.parkour.region.Wall;
import eu.andret.parkour.util.Medal;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.MemorySection;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.logging.Level;

@Data
public abstract class ParkourGame implements Comparable<ParkourGame>, YmlSerializable {
    private boolean running = false;
    private String name;
    private World world;
    private GameRegion gameRegion;
    private Location bestRecord;
    private Location teleportBlock;
    private ParkourOptions options = new ParkourOptions();

    @Getter(AccessLevel.NONE)
    private final List<Checkpoint> checkpoints = new ArrayList<>();
    @Getter(AccessLevel.NONE)
    private final List<Wall> walls = new ArrayList<>();
    @Getter(AccessLevel.NONE)
    private final List<EffectRegion> effects = new ArrayList<>();
    @Getter(AccessLevel.NONE)
    private final List<String> authors = new ArrayList<>();
    @Getter(AccessLevel.NONE)
    private final List<ParkourPlayer> players = new ArrayList<>();

    public enum ParkourType {
        SERVER,
        TRAINING,
        PLAYERS
    }

    @Data
    @NoArgsConstructor
    public class ParkourOptions implements YmlSerializable {
        private boolean enabled = true;
        private boolean forcingSprint = false;
        private boolean alwaysSpawn = false;
        private boolean countingRecords = true;
        private boolean allowingDamage = false;
        private boolean boat = false;
        private boolean modifyInventory = true;
        private boolean available = false;
        private boolean vip = false;
        private int price = 0;
        private int bronze = 0;
        private int silver = 0;
        private int gold = 0;
        private int platinum = 0;
        private int difficulty = 1;
        private int xp = 0;
        private float fair = 0F;
        @Getter(AccessLevel.NONE)
        private final Map<PotionEffectType, Integer> effects = new HashMap<>();
        private DyeColor color = DyeColor.WHITE;
        private ParkourType type = ParkourType.SERVER;
        private String displayName;

        public ParkourOptions(ParkourOptions options) {
            enabled = options.enabled;
            forcingSprint = options.forcingSprint;
            alwaysSpawn = options.alwaysSpawn;
            countingRecords = options.countingRecords;
            allowingDamage = options.allowingDamage;
            boat = options.boat;
            modifyInventory = options.modifyInventory;
            available = options.available;
            vip = options.vip;
            bronze = options.bronze;
            silver = options.silver;
            gold = options.gold;
            platinum = options.platinum;
            difficulty = options.difficulty;
            xp = options.xp;
            fair = options.fair;
            effects.putAll(options.effects);
            color = options.color;
            type = options.type;
            price = options.price;
            displayName = options.displayName;
        }

        public Medal getMedalByTime(float time) {
            if (platinum >= time) {
                return Medal.PLATINUM;
            }
            if (gold >= time) {
                return Medal.GOLD;
            }
            if (silver >= time) {
                return Medal.SILVER;
            }
            if (bronze >= time) {
                return Medal.BRONZE;
            }
            return Medal.NONE;
        }

        public void setEffect(PotionEffectType effect, int amplifier) {
            effects.put(effect, amplifier);
        }

        public int removeEffect(PotionEffectType effect) {
            return effects.remove(effect);
        }

        public Map<PotionEffectType, Integer> getEffects() {
            return new HashMap<>(effects);
        }

        @Override
        public Map<String, Object> toYmlStructure() {
            Map<String, Object> result = new HashMap<>();
            result.put("alwaysSpawn", alwaysSpawn);
            result.put("boat", boat);
            result.put("countingRecords", countingRecords);
            result.put("allowingDamage", allowingDamage);
            result.put("forcingSprint", forcingSprint);
            result.put("price", price);
            result.put("modifyInventory", modifyInventory);
            result.put("bronze", bronze);
            result.put("silver", silver);
            result.put("gold", gold);
            result.put("platinum", platinum);
            result.put("available", available);
            result.put("color", color.name());
            result.put("difficulty", difficulty);
            result.put("displayname", displayName);
            result.put("vip", vip);
            result.put("enabled", enabled);
            result.put("fair", fair);
            result.put("xp", xp);
            result.put("type", type.toString());
            Map<String, Integer> localEffects = new HashMap<>();
            for (Entry<PotionEffectType, Integer> entry : effects.entrySet()) {
                localEffects.put(entry.getKey().getName(), entry.getValue());
            }
            if (localEffects.size() > 0) {
                result.put("effects", localEffects);
            }
            return result;
        }

        @Override
        public void fromYmlStructure(Map<String, Object> options) {
            alwaysSpawn = (boolean) options.get("alwaysSpawn");
            boat = (boolean) options.get("boat");
            countingRecords = (boolean) options.get("countingRecords");
            allowingDamage = (boolean) options.get("allowingDamage");
            forcingSprint = (boolean) options.get("forcingSprint");
            price = (int) options.get("price");
            modifyInventory = (boolean) options.get("modifyInventory");
            bronze = (int) options.get("bronze");
            silver = (int) options.get("silver");
            gold = (int) options.get("gold");
            platinum = (int) options.get("platinum");
            color = DyeColor.valueOf((String) options.get("color"));
            difficulty = (int) options.get("difficulty");
            MemorySection m = (MemorySection) options.get("effects");
            if (m != null) {
                for (String s : m.getKeys(false)) {
                    effects.put(PotionEffectType.getByName(s), m.getInt(s));
                }
            }
            available = (boolean) options.get("available");
            type = ParkourType.valueOf((String) options.get("type"));
            displayName = (String) options.get("displayname");
            try {
                vip = (boolean) options.get("vip");
            } catch (Exception ex) {
                vip = false;
            }

            try {
                enabled = (boolean) options.get("enabled");
            } catch (Exception ex) {
                enabled = true;
            }

            try {
                fair = (float) options.get("fair");
            } catch (Exception ex) {
                fair = 0.0F;
            }

            try {
                xp = (int) options.get("xp");
            } catch (Exception ex) {
                xp = 0;
            }
        }
    }

    public ParkourGame(String name, GameRegion gameRegion, World world) {
        this.name = name;
        this.gameRegion = gameRegion;
        this.world = world;
        options.displayName = name;
        ParkourManager.addParkour(this);
    }

    public void setName(String newName) {
        name = newName;
        ParkourManager.sortGames();
    }

    public void addCheckpoint(Checkpoint checkpoint) {
        checkpoints.add(checkpoint);
    }

    public void setCheckpoint(int id, CuboidRegion checkpoint) {
        if (id == 0) {
            throw new ArrayIndexOutOfBoundsException();
        }
        checkpoints.get(id).setRegion(checkpoint);
    }

    public void setCheckpoint(int id, Checkpoint checkpoint) {
        if (id == 0) {
            throw new ArrayIndexOutOfBoundsException();
        }
        checkpoints.set(id, checkpoint);
    }

    public void addWall(Wall wall) {
        walls.add(wall);
    }

    public void addEffectRegion(EffectRegion effectRegion) {
        effects.add(effectRegion);
    }

    public void setWall(int id, CuboidRegion wall) {
        walls.get(id).setRegion(wall);
    }

    public void setWall(int id, Wall wall) {
        walls.set(id, wall);
    }

    public void setEffectRegion(int id, EffectRegion effectRegion) {
        effects.set(id, effectRegion);
    }

    public void addAuthor(String author) {
        authors.add(author);
    }

    public void setSpawn(Checkpoint newSpawnLocation) {
        if (checkpoints.isEmpty()) {
            checkpoints.add(newSpawnLocation);
        } else {
            checkpoints.set(0, newSpawnLocation);
        }
    }


    public abstract void addPlayer(Player player);

    protected void addPlayer(ParkourPlayer pl) {
        if (!players.contains(pl) && !pl.inAnyParkour()) {
            players.add(pl);
            pl.reset();
            Bukkit.getPluginManager().callEvent(new PlayerJoinGameEvent(this, pl));
            pl.setLastVisitedCheckpointId(0);
        }
    }

    public boolean removePlayer(Player player) {
        ParkourPlayer p = PlayerManager.getParkourSinglePlayer(player);
        Bukkit.getServer().getPluginManager().callEvent(new PlayerQuitGameEvent(this, p));
        p.reset();
        return players.remove(p);
    }

    public void start() {
        running = true;
        Bukkit.getServer().getPluginManager().callEvent(new GameStartEvent(this));
        ParkourManager.sortGames();
    }

    public void stop() {
        running = false;
        Bukkit.getServer().getPluginManager().callEvent(new GameStopEvent(this));
        ParkourManager.sortGames();
    }

    public boolean inAnyRegion(Location loc) {
        if (world == null || !world.equals(loc.getWorld())) {
            return false;
        }
        for (AbstractRegion r : getAllRegions()) {
            if (r == null) {
                Bukkit.getServer().getLogger().log(Level.INFO, "[ParkourPlugin] Debug: Region is null!");
            } else if (r.contains(loc)) {
                return true;
            }
        }
        return false;
    }

    public List<AbstractRegion> getAllRegions() {
        List<AbstractRegion> arr = new ArrayList<>();
        arr.add(gameRegion);
        arr.addAll(walls);
        arr.addAll(checkpoints);
        arr.addAll(effects);
        return arr;
    }


    public Checkpoint getFinish() {
        return checkpoints.get(checkpoints.size() - 1);
    }

    public List<ParkourPlayer> getPlayers() {
        return players;
    }

    public int getLastCheckpointId() {
        return checkpoints.size() - 1;
    }

    public Checkpoint getCheckpoint(int id) {
        return checkpoints.get(id);
    }

    public List<Checkpoint> getCheckpointList() {
        return checkpoints;
    }

    public Wall getWall(int id) {
        return walls.get(id);
    }

    public List<Wall> getWallList() {
        return walls;
    }

    public EffectRegion getEffectRegion(int id) {
        return effects.get(id);
    }

    public List<EffectRegion> getEffectRegionList() {
        return effects;
    }

    public Checkpoint getSpawn() {
        if (checkpoints.isEmpty()) {
            return null;
        }
        return checkpoints.get(0);
    }

    public List<String> getAuthors() {
        return new ArrayList<>(authors);
    }

    @Override
    public Map<String, Object> toYmlStructure() {
        Map<String, Object> map = new TreeMap<>();
        if (gameRegion == null) {
            return map;
        }
        map.put("region", gameRegion.toYmlStructure());
        if (getSpawn() != null) {
            map.put("spawn", getSpawn().toYmlStructure());
        }
        for (Checkpoint r : checkpoints) {
            if (checkpoints.indexOf(r) != 0) {
                map.put("checkpoint_" + checkpoints.indexOf(r), r.toYmlStructure());
            }
        }
        for (Wall r : walls) {
            map.put("wall_" + walls.indexOf(r), r.toYmlStructure());
        }
        for (EffectRegion r : effects) {
            map.put("effect_" + effects.indexOf(r), r.toYmlStructure());
        }
        if (bestRecord != null) {
            Map<String, Object> tmp = new HashMap<>();
            tmp.put("x", bestRecord.getX());
            tmp.put("y", bestRecord.getY());
            tmp.put("z", bestRecord.getZ());
            tmp.put("world", bestRecord.getWorld().getName());
            map.put("recordssign", tmp);
        }
        if (teleportBlock != null) {
            Map<String, Object> tmp = new HashMap<>();
            tmp.put("x", teleportBlock.getX());
            tmp.put("y", teleportBlock.getY());
            tmp.put("z", teleportBlock.getZ());
            tmp.put("world", teleportBlock.getWorld().getName());
            map.put("teleportBlock", tmp);
        }
        return map;
    }

    @Override
    public void fromYmlStructure(Map<String, Object> structure) {
        gameRegion = new GameRegion(world);
        gameRegion.fromYmlStructure((Map<String, Object>) structure.get("region"));
        setSpawn(new Checkpoint(world));
        getSpawn().fromYmlStructure((Map<String, Object>) structure.get("spawn"));
        for (Entry<String, Object> entry : structure.entrySet()) {
            switch (entry.getKey().split("_")[0]) {
                case "checkpoint":
                    Checkpoint checkpoint = new Checkpoint(world);
                    checkpoint.fromYmlStructure((Map<String, Object>) entry.getValue());
                    addCheckpoint(checkpoint);
                    break;
                case "wall":
                    Wall wall = new Wall(world);
                    wall.fromYmlStructure((Map<String, Object>) entry.getValue());
                    addWall(wall);
                    break;
                case "effect":
                    EffectRegion effectRegion = new EffectRegion(world);
                    effectRegion.fromYmlStructure((Map<String, Object>) entry.getValue());
                    addEffectRegion(effectRegion);
                    break;
                /*case "recordssign":
                    Object o = entry.getValue().get("world");
                    bestRecord = new Location(o == null ? world : Bukkit.getWorld((String) o),
                            (double) entry.getValue().get("x"),
                            (double) entry.getValue().get("y"),
                            (double) entry.getValue().get("z"));
                    break;
                case "teleportBlock":
                    teleportBlock = new Location(entry.getValue().get("world") == null ? world : Bukkit.getWorld((String) entry.getValue().get("world")),
                            (double) entry.getValue().get("x"),
                            (double) entry.getValue().get("y"),
                            (double) entry.getValue().get("z"));
                    break;*/
                default:
                    break;
            }
        }
    }

    @Override
    public int compareTo(@Nonnull ParkourGame parkour) {
        if (running && !parkour.running) {
            return 1;
        }
        if (!running && parkour.running) {
            return -1;
        }
        if (options.difficulty != parkour.options.difficulty) {
            return options.difficulty - parkour.options.difficulty;
        }
        return name.compareToIgnoreCase(parkour.name);
    }

    public boolean removeAuthor(String string) {
        return authors.remove(string);
    }
}
