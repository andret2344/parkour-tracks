package eu.andret.atsparkour.parkour;

import com.sk89q.worldedit.regions.CuboidRegion;
import eu.andret.atsparkour.atsParkour;
import eu.andret.atsparkour.data.Medal;
import eu.andret.atsparkour.event.GameStartEvent;
import eu.andret.atsparkour.event.GameStopEvent;
import eu.andret.atsparkour.event.PlayerJoinGameEvent;
import eu.andret.atsparkour.event.PlayerQuitGameEvent;
import eu.andret.atsparkour.player.ParkourPlayer;
import eu.andret.atsparkour.player.PlayerManager;
import eu.andret.atsparkour.region.*;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.MemorySection;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.util.*;
import java.util.Map.Entry;

public abstract class ParkourGame implements Comparable<ParkourGame> {
    private boolean run = false, hasspawn = false;
    private String name;
    private World world;
    private GameRegion gameregion;
    private ParkourOptions options;
    private Location bestRecord;
    private Location teleportBlock;
    private final List<Checkpoint> checkpoints = new ArrayList<>();
    private final List<Wall> walls = new ArrayList<>();
    private final List<EffectRegion> effects = new ArrayList<>();
    private final List<String> authors = new ArrayList<>();
    private final List<ParkourPlayer> players = new ArrayList<>();


    public enum ParkourType {
        SERVER,
        TRENING,
        PLAYERS
    }

    public class ParkourOptions implements Cloneable {
        private boolean proceedable = true, forceSprint = false, alwaysspawn = false, countrecords = true, damage = false, boats = false, modifyeq = true, available = false, vip = false;
        private int price = 0, bronze = 0, silver = 0, gold = 0, platinum = 0, difficulty = 1, xp = 0;
        private float fair = 0.0F;
        private final Map<PotionEffectType, Integer> effects = new HashMap<>();
        private DyeColor color = DyeColor.WHITE;
        private ParkourType type = ParkourType.SERVER;
        private String displayName;

        public void setVip(boolean vip) {
            this.vip = vip;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public void setType(ParkourType type) {
            this.type = type;
        }

        public void setAvailable(boolean available) {
            this.available = available;
        }

        public void setDifficulty(int difficulty) {
            this.difficulty = difficulty;
            ParkourManager.sortGames();
        }

        public void setColor(DyeColor color) {
            this.color = color;
        }

        public void setCountRecords(boolean countrecords) {
            this.countrecords = countrecords;
        }

        public void setDamage(boolean damage) {
            this.damage = damage;
        }

        public void setAlwaysTpToSpawn(boolean alwaysspawn) {
            this.alwaysspawn = alwaysspawn;
        }

        public void mustSprint(boolean sprint) {
            forceSprint = sprint;
        }

        public void setRewardPrice(int price) {
            this.price = price;
        }

        public boolean isVip() {
            return vip;
        }

        public String getDisplayName() {
            return displayName;
        }

        public ParkourType getType() {
            return type;
        }

        public boolean isAvailable() {
            return available;
        }

        public int getDifficulty() {
            return difficulty;
        }

        public boolean countRecords() {
            return countrecords;
        }

        public boolean alwaysTpToSpawn() {
            return alwaysspawn;
        }

        public boolean mustSprint() {
            return forceSprint;
        }

        public DyeColor getColor() {
            return color;
        }

        public int getRewardPrice() {
            return price;
        }

        public boolean isDamage() {
            return damage;
        }

        public boolean isBoats() {
            return boats;
        }

        public void setBoats(boolean boats) {
            this.boats = boats;
        }

        public int getBronze() {
            return bronze;
        }

        public void setBronze(int bronze) {
            this.bronze = bronze;
        }

        public int getSilver() {
            return silver;
        }

        public void setSilver(int silver) {
            this.silver = silver;
        }

        public int getGold() {
            return gold;
        }

        public void setGold(int gold) {
            this.gold = gold;
        }

        public int getPlatinum() {
            return platinum;
        }

        public boolean isProceedable() {
            return proceedable;
        }

        public float getFair() {
            return fair;
        }

        public int getXp() {
            return xp;
        }

        public void setXp(int xp) {
            this.xp = xp;
        }

        public void setFair(float fair) {
            this.fair = fair;
        }

        public void setProceedable(boolean proceedable) {
            this.proceedable = proceedable;
        }

        public void setPlatnum(int platinum) {
            this.platinum = platinum;
        }

        public int getEffectAmplifier(PotionEffectType effect) {
            return effects.getOrDefault(effect, 0);
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

        public boolean isModifyeq() {
            return modifyeq;
        }

        public void setModifyeq(boolean modifyeq) {
            this.modifyeq = modifyeq;
        }

        public Map<PotionEffectType, Integer> getEffects() {
            return new HashMap<>(effects);
        }

        @Override
        public String toString() {
            StringBuilder result = new StringBuilder(getClass().getSimpleName() +
                    "{displayname=" + displayName +
                    "§r, forceSprint=" + forceSprint +
                    ", alwaysspawn=" + alwaysspawn +
                    ", price=" + price +
                    ", damage=" + damage +
                    ", boats=" + boats +
                    ", modifyeq=" + modifyeq +
                    ", countrecords=" + countrecords +
                    ", difficulty=" + difficulty +
                    ", bronze=" + bronze +
                    ", silver=" + silver +
                    ", gold=" + gold +
                    ", platinum=" + platinum +
                    ", color=" + color +
                    ", available=" + available +
                    ", type=" + type +
                    ", vip=" + vip +
                    ", proceedable=" + proceedable +
                    ", fair=" + fair +
                    ", xp=" + xp +
                    ", effects=[");
            for (Entry<PotionEffectType, Integer> entry : effects.entrySet()) {
                result.append(entry.getKey().getName()).append("=").append(entry.getValue()).append(", ");
            }
            result = new StringBuilder(result.substring(0, result.length() - (effects.size() == 0 ? 0 : 2)));
            result.append("]}");
            return result.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) {
                return false;
            }
            if (!(o instanceof ParkourOptions)) {
                return false;
            }
            if (o == this) {
                return true;
            }
            ParkourOptions po = (ParkourOptions) o;
            return
                    po.alwaysspawn == alwaysspawn &&
                            po.displayName.equals(displayName) &&
                            po.forceSprint == forceSprint &&
                            po.price == price &&
                            po.countrecords == countrecords &&
                            po.damage == damage &&
                            po.boats == boats &&
                            po.modifyeq == modifyeq &&
                            po.bronze == bronze &&
                            po.silver == silver &&
                            po.gold == gold &&
                            po.platinum == platinum &&
                            po.difficulty == difficulty &&
                            po.available == available &&
                            po.vip == vip &&
                            po.proceedable == proceedable &&
                            po.fair == fair &&
                            po.xp == xp &&
                            po.type.equals(type) &&
                            po.color.equals(color) &&
                            po.effects.equals(effects);
        }

        @Override
        public ParkourOptions clone() throws CloneNotSupportedException {
            ParkourOptions o = (ParkourOptions) super.clone();
            o.alwaysspawn = alwaysspawn;
            o.boats = boats;
            o.countrecords = countrecords;
            o.damage = damage;
            o.forceSprint = forceSprint;
            o.price = price;
            o.modifyeq = modifyeq;
            o.bronze = bronze;
            o.silver = silver;
            o.gold = gold;
            o.platinum = platinum;
            o.color = color;
            o.available = available;
            o.difficulty = difficulty;
            o.displayName = displayName;
            o.type = type;
            o.vip = vip;
            o.proceedable = proceedable;
            o.fair = fair;
            o.xp = xp;
            for (Entry<PotionEffectType, Integer> entry : effects.entrySet()) {
                o.effects.put(entry.getKey(), entry.getValue());
            }
            return o;
        }

        public Map<String, Object> toYamlStructure() {
            Map<String, Object> options = new HashMap<>();
            options.put("alwaysspawn", alwaysspawn);
            options.put("boats", boats);
            options.put("countrecords", countrecords);
            options.put("damage", damage);
            options.put("forceSprint", forceSprint);
            options.put("price", price);
            options.put("modifyeq", modifyeq);
            options.put("bronze", bronze);
            options.put("silver", silver);
            options.put("gold", gold);
            options.put("platinum", platinum);
            options.put("available", available);
            options.put("color", color.name());
            options.put("difficulty", difficulty);
            options.put("displayname", displayName);
            options.put("vip", vip);
            options.put("proceedable", proceedable);
            options.put("fair", fair);
            options.put("xp", xp);
            options.put("type", type.toString());
            Map<String, Integer> localEffects = new HashMap<>();
            for (Entry<PotionEffectType, Integer> entry : effects.entrySet()) {
                localEffects.put(entry.getKey().getName(), entry.getValue());
            }
            if (localEffects.size() > 0) {
                options.put("effects", localEffects);
            }
            return options;
        }

        public void fromYamlStructure(Map<String, Object> options) {
            alwaysspawn = (boolean) options.get("alwaysspawn");
            boats = (boolean) options.get("boats");
            countrecords = (boolean) options.get("countrecords");
            damage = (boolean) options.get("damage");
            forceSprint = (boolean) options.get("forceSprint");
            price = (int) options.get("price");
            modifyeq = (boolean) options.get("modifyeq");
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
                proceedable = (boolean) options.get("proceedable");
            } catch (Exception ex) {
                proceedable = true;
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


    public ParkourGame(String name, GameRegion gameregion, World world) {
        this.name = name;
        this.gameregion = gameregion;
        this.world = world;
        options = new ParkourOptions();
        options.displayName = name;
        ParkourManager.addParkour(this);
    }

    public void setOptions(ParkourOptions options) {
        this.options = options;
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
        checkpoints.get(id).updateRegion(checkpoint);
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
        walls.get(id).updateRegion(wall);
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
        if (checkpoints.size() == 0) {
            checkpoints.add(newSpawnLocation);
        } else {
            checkpoints.set(0, newSpawnLocation);
        }
        hasspawn = true;
    }

    void destroy() {
        new File(atsParkour.getInstance().getDataFolder().getAbsolutePath() + "/games/" + name + ".yml").delete();
        run = hasspawn = false;
        name = null;
        world = null;
        gameregion = null;
        options = null;
        checkpoints.clear();
        walls.clear();
        effects.clear();
        authors.clear();
        players.clear();
    }

    public abstract void addPlayer(Player player);

    protected void addPlayer(ParkourPlayer pl) {
        if (!players.contains(pl) && !pl.inAnyParkour()) {
            players.add(pl);
            pl.reset();
            Bukkit.getPluginManager().callEvent(new PlayerJoinGameEvent(pl.getPlayer(), this));
            pl.setLastVisitedChecpoint(0);
        }
    }

    public boolean removePlayer(Player player) {
        ParkourPlayer p = PlayerManager.getParkourSinglePlayer(player);
        Bukkit.getServer().getPluginManager().callEvent(new PlayerQuitGameEvent(player, this));
        p.reset();
        return players.remove(p);
    }

    public void start() {
        run = true;
        Bukkit.getServer().getPluginManager().callEvent(new GameStartEvent(this));
        ParkourManager.sortGames();
    }

    public void stop() {
        run = false;
        Bukkit.getServer().getPluginManager().callEvent(new GameStopEvent(this));
        ParkourManager.sortGames();
    }

    public boolean isRunning() {
        return run;
    }

    public World getWorld() {
        return world;
    }

    public void setWorld(World world) {
        this.world = world;
    }

    public void updateRegion(GameRegion gr) {
        gameregion = gr;
    }

    public void setBestRecordLocation(Location bestRecord) {
        this.bestRecord = bestRecord;
    }

    public void setTeleportBlockLocation(Location teleportBlock) {
        this.teleportBlock = teleportBlock;
    }

    public Location getBestRecordLocation() {
        return bestRecord;
    }

    public boolean inAnyRegion(Location loc) {
        if (world == null || !world.equals(loc.getWorld())) {
            return false;
        }
        for (AbstractRegion r : getAllRegions()) {
            if (r == null) {
                System.out.print("[atsParkour] Debug: Region " + r + " is null!");
            } else if (r.contains(loc)) {
                return true;
            }
        }
        return false;
    }

    public List<AbstractRegion> getAllRegions() {
        List<AbstractRegion> arr = new ArrayList<>();
        arr.add(gameregion);
        arr.addAll(walls);
        arr.addAll(checkpoints);
        arr.addAll(effects);
        return arr;
    }

    @Override
    public String toString() {
        String check = "[", w = "[", e = "[";
        for (Checkpoint checkpoint : checkpoints) {
            check += checkpoint + ", ";
        }
        check = check.substring(0, check.length() - 1) + "]";
        for (Wall wall : walls) {
            w += wall + ", ";
        }
        w = w.substring(0, w.length() - 1) + "]";
        for (EffectRegion effect : effects) {
            e += effect + ", ";
        }
        e = e.substring(0, e.length() - 1) + "]";
        return getClass().getSimpleName() + "{name=" + name + ", world= " + world.getName() + (walls.size() != 0 ? ", walls=" + w : "") +
                (checkpoints.size() != 0 ? ", checkpoints=" + check : "") + (effects.size() != 0 ? ", effectregions=" + e : "") +
                ", options=" + options + "}";
    }

    public Checkpoint getFinish() {
        return checkpoints.get(checkpoints.size() - 1);
    }

    public ParkourOptions getOptions() {
        return options;
    }

    public String getName() {
        return name;
    }

    public List<? extends ParkourPlayer> getPlayers() {
        return players;
    }

    public GameRegion getGameRegion() {
        return gameregion;
    }

    public int getLastCheckpointId() {
        return checkpoints.size() - 1;
    }

    public boolean hasSpawn() {
        return hasspawn;
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
        if (checkpoints.size() == 0) {
            return null;
        }
        return checkpoints.get(0);
    }

    public Location getTeleportBlockLocation() {
        return teleportBlock;
    }

    public List<String> getAuthors() {
        return new ArrayList<>(authors);
    }

    public Map<String, Map<String, Object>> toYamlStructure() {
        Map<String, Map<String, Object>> map = new TreeMap<>();
        if (gameregion != null) {
            map.put("region", gameregion.toYamlStructure());
            if (getSpawn() != null) {
                map.put("spawn", getSpawn().toYamlStructure());
            }
            for (AbstractRegion r : checkpoints) {
                if (checkpoints.indexOf(r) != 0) {
                    map.put("checkpoint_" + checkpoints.indexOf(r), r.toYamlStructure());
                }
            }
            for (AbstractRegion r : walls) {
                map.put("wall_" + walls.indexOf(r), r.toYamlStructure());
            }
            for (AbstractRegion r : effects) {
                map.put("effect_" + effects.indexOf(r), r.toYamlStructure());
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
        }
        return map;
    }

    public void fromYamlStructure(Map<String, Map<String, Object>> map) {
        updateRegion(new GameRegion(world, map.get("region")));
        setSpawn(new Checkpoint(world, map.get("spawn")));
        for (Entry<String, Map<String, Object>> entry : map.entrySet()) {
            switch (entry.getKey().split("_")[0]) {
                case "checkpoint":
                    addCheckpoint(new Checkpoint(world, entry.getValue()));
                    break;
                case "wall":
                    addWall(new Wall(world, entry.getValue()));
                    break;
                case "effect":
                    addEffectRegion(new EffectRegion(world, entry.getValue()));
                    break;
                case "recordssign":
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
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (o == this) {
            return true;
        }
        if (!(o instanceof ParkourGame)) {
            return false;
        }
        return ((ParkourGame) o).name.equals(name);
    }


    @Override
    public int compareTo(ParkourGame parkour) {
        if (run && !parkour.run) {
            return 1;
        }
        if (!run && parkour.run) {
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