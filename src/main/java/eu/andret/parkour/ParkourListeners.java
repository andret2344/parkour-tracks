package eu.andret.parkour;

import eu.andret.parkour.event.player.PlayerAchieveCheckpointEvent;
import eu.andret.parkour.event.player.PlayerCompleteParkourEvent;
import eu.andret.parkour.event.player.PlayerEnterEffectRegionEvent;
import eu.andret.parkour.event.player.PlayerEnterRegionEvent;
import eu.andret.parkour.event.player.PlayerHitWallEvent;
import eu.andret.parkour.event.player.PlayerJoinGameEvent;
import eu.andret.parkour.event.player.PlayerLeaveRegionEvent;
import eu.andret.parkour.event.player.PlayerQuitGameEvent;
import eu.andret.parkour.event.player.PlayerTeleportBackEvent;
import eu.andret.parkour.parkour.ParkourGame;
import eu.andret.parkour.parkour.ParkourManager;
import eu.andret.parkour.player.ParkourPlayer;
import eu.andret.parkour.player.PlayerManager;
import eu.andret.parkour.region.AbstractRegion;
import eu.andret.parkour.region.Checkpoint;
import eu.andret.parkour.region.Wall;
import eu.andret.parkour.tasks.DataBaseOperations;
import eu.andret.parkour.tasks.ParkourDataOperations;
import eu.andret.parkour.tasks.TeleportCount;
import lombok.AllArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Boat;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

@AllArgsConstructor
public class ParkourListeners implements Listener {

    private final Map<UUID, Integer> countTime = new HashMap<>();

    private final Map<UUID, Integer> teleportCount1 = new HashMap<>();
    private final Map<UUID, TeleportCount> teleportCount2 = new HashMap<>();

    private final Map<UUID, BukkitTask> authorTasks = new HashMap<>();

    private final ParkourPlugin plugin;

    @EventHandler(priority = EventPriority.HIGHEST)
    public synchronized void move(PlayerMoveEvent e) {
        Player pl = e.getPlayer();
        ParkourPlayer pp = PlayerManager.getParkourSinglePlayer(e.getPlayer());
        if (e.getTo().getY() < -10) {
            pp.teleportToLobby();
        }
        ParkourGame parkour = ParkourManager.getParkour(pl);
        if (parkour != null) {
            if (!pp.isIgnoring()) {
                if (pl.getWorld().equals(parkour.getWorld()) && parkour.isRunning()) {
                    pl.setFoodLevel(20);
                    pl.setHealth(pl.getMaxHealth());
                    for (Entry<PotionEffectType, Integer> entry : parkour.getOptions().getEffects().entrySet()) {
                        pl.addPotionEffect(new PotionEffect(entry.getKey(), 99999999, entry.getValue()));
                    }
                    if (!pp.isSpectating() && pl.isFlying() && !parkour.getOptions().isBoat() && !parkour.getSpawn().contains(pl.getLocation())) {
                        pp.teleportToCheckpoint(pp.getLastVisitedCheckpoint());
                    }
                    if (!pp.isSpectating() && parkour.getOptions().isForcingSprint() && !pl.isSprinting()) {
                        boolean tp = true;
                        for (Checkpoint cr : parkour.getCheckpointList()) {
                            if (cr.contains(pl.getLocation())) {
                                tp = false;
                            }
                        }

                        if (tp) {
                            if (parkour.getOptions().isAlwaysSpawn()) {
                                pp.teleportToSpawn();
                            } else {
                                pp.teleportToCheckpoint(pp.getLastVisitedCheckpoint());
                            }
                        }
                    }
                }

                for (ParkourGame p : ParkourManager.getAllGames()) {
                    for (AbstractRegion r : p.getAllRegions()) {
                        if (!pp.isSpectating() && p.isRunning() && !pp.isIgnoring() && p.getWorld().equals(parkour.getWorld())) {
                            if (r.contains(e.getFrom()) && !r.contains(e.getTo())) {
                                Bukkit.getPluginManager().callEvent(new PlayerLeaveRegionEvent(pl, p, r));
                            } else if (!r.contains(e.getFrom()) && r.contains(e.getTo())) {
                                Bukkit.getPluginManager().callEvent(new PlayerEnterRegionEvent(pl, p, r));
                                Event evt = null;
                                if (r instanceof Checkpoint) {
                                    evt = new PlayerAchieveCheckpointEvent(pl, p, p.getCheckpointList().indexOf(r));
                                } else if (r instanceof Wall) {
                                    evt = new PlayerHitWallEvent(pl, p, p.getWallList().indexOf(r));
                                    // } else if (r instanceof EffectRegion) {
                                    // evt = new
                                    // PlayerEnterEffectRegionEvent(pl, p,
                                    // p.getEffectRegionList().indexOf((EffectRegion)r));
                                }
                                if (evt != null) {
                                    Bukkit.getPluginManager().callEvent(evt);
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!e.getPlayer().getGameMode().equals(GameMode.CREATIVE)) {
            boolean fly = false;
            for (ParkourGame p : ParkourManager.getAllGames()) {
                if (PlayerManager.getParkourSinglePlayer(pl).isSpectating()) {
                    if (p.inAnyRegion(pl.getLocation())) {
                        fly = true;
                        for (Entry<PotionEffectType, Integer> entry : p.getOptions().getEffects().entrySet()) {
                            e.getPlayer().addPotionEffect(new PotionEffect(entry.getKey(), 99999999, entry.getValue()));
                        }
                        pl.setFoodLevel(20);
                        pl.setHealth(20D);
                    } else {
                        for (Entry<PotionEffectType, Integer> entry : p.getOptions().getEffects().entrySet()) {
                            e.getPlayer().removePotionEffect(entry.getKey());
                        }
                    }
                }
            }
            pl.setAllowFlight(fly);
        }
        for (ParkourGame p : ParkourManager.getAllGames()) {
            if (!pp.isSpectating() && pl.getWorld().equals(p.getWorld()) && !pp.isIgnoring()) {
                if (!p.inAnyRegion(e.getPlayer().getLocation()) && p.getPlayers().contains(e.getPlayer())) {
                    p.removePlayer(pl);
                } else if (p.inAnyRegion(e.getPlayer().getLocation()) && !p.getPlayers().contains(e.getPlayer())) {
                    if (p.getOptions().isVip() && !plugin.isVip(e.getPlayer())) {
                        pl.sendMessage(msg("noVip", true));
                        PlayerManager.getParkourSinglePlayer(e.getPlayer()).teleportToLobby();
                    } else {
                        p.addPlayer(pl);
                    }
                }
            }
        }
    }

    @EventHandler
    public void checkpoint(PlayerAchieveCheckpointEvent e) {
        ParkourGame pk = e.getGame();
        ParkourPlayer pp = PlayerManager.getParkourSinglePlayer(e.getPlayer());
        if (e.getPlayer().getWorld().equals(pk.getWorld())) {
            if (e.getCheckpointId() > pp.getLastVisitedCheckpoint() // &&
                // !e.getPlayer().getLocation().clone().add(0, -1,
                // 0).getBlock().getType().equals(Material.AIR)
            ) {
                pp.setLastVisitedCheckpoint(e.getCheckpointId());
                if (e.getCheckpointId() == e.getGame().getCheckpointList().size() - 1) {
                    if (plugin.getConfig().getBoolean("last-checkpoint-info")) {
                        e.getPlayer().sendMessage(msg("achieveCheckpoint", false));
                    }
                } else {
                    e.getPlayer().sendMessage(msg("achieveCheckpoint", false));
                }
            }
            if (e.getCheckpointId() == pk.getLastCheckpointId()) {
                Bukkit.getPluginManager().callEvent(new PlayerCompleteParkourEvent(e.getPlayer(), e.getGame()));
            }
            if (e.getCheckpointId() == 0) {
                PlayerManager.getParkourSinglePlayer(e.getPlayer()).reset();
            }
        }
    }

    @EventHandler
    public void complete(PlayerCompleteParkourEvent e) {
        Player pl = e.getPlayer();
        pl.playSound(pl.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5F, 0.5F);
        if (!teleportCount2.containsKey(e.getPlayer().getUniqueId())) {
            if (e.getGame().getOptions().isCountingRecords()) {
                float curr = PlayerManager.getParkourSinglePlayer(e.getPlayer()).getTime();
                if (curr < e.getGame().getOptions().getFair()) {
                    Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), "tban " + pl.getName() + " 1");
                    return;
                }
                String time = String.valueOf(curr);
                pl.sendMessage(msg("finishTime", false).replace("%TIME%", String.valueOf(Math.abs(time.lastIndexOf(".") - time.length()) == 2 ? (time + "0") : time)));
                Bukkit.getScheduler().runTaskAsynchronously(plugin, new DataBaseOperations(pl, e.getGame(), curr));
            }
            if (plugin.isLobbyCoins() && e.getGame().getOptions().getPrice() > 0) {
                int i = e.getGame().getOptions().getPrice();
                // LobbyCoins.getInstance().addCoins(pl.getName(), i);
                e.getPlayer().sendMessage(msg("rewardPrice", false).replace("%PRICE%", String.valueOf(i)));
            }
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> startScheduling(pl), 5L);
        }
        updateScoreboardFor(e.getPlayer(), e.getGame());
    }

    @EventHandler
    public void back(PlayerTeleportBackEvent e) {
        if (e.getCheckpointId() == 0) {
            PlayerManager.getParkourSinglePlayer(e.getPlayer()).reset();
        }
        if (e.getGame().getOptions().isBoat()) {
            Boat b = (Boat) e.getPlayer().getLocation().getWorld().spawnEntity(e.getPlayer().getLocation(), EntityType.BOAT);
            b.setPassenger(e.getPlayer());
        }
    }

    @EventHandler
    public void leaveBoat(VehicleExitEvent e) {
        if (e.getExited() instanceof Player) {
            Player player = (Player) e.getExited();
            ParkourGame parkour = ParkourManager.getParkour(player);
            if (PlayerManager.playerExists(player) && parkour.getOptions().isBoat() && e.getVehicle() instanceof Boat && parkour.isRunning()) {
                if (player.getWorld().equals(parkour.getWorld())) {
                    e.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void wall(PlayerHitWallEvent e) {
        ParkourPlayer pp = PlayerManager.getParkourSinglePlayer(e.getPlayer());
        if (e.getGame().getOptions().isAlwaysSpawn()) {
            pp.teleportToSpawn();
        } else {
            pp.teleportToCheckpoint(pp.getLastVisitedCheckpoint());
        }
    }

    @EventHandler
    public synchronized void joinGame(PlayerJoinGameEvent e) {
        if (e.getGame().getOptions().isCountingRecords() && !e.getPlayer().getGameMode().equals(GameMode.CREATIVE)) {
            e.getPlayer().setLevel(0);
            e.getPlayer().setExp(0);
            if (!countTime.containsKey(e.getPlayer().getUniqueId()) && !PlayerManager.getParkourSinglePlayer(e.getPlayer()).isSpectating()) {
                int s = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, PlayerManager.getParkourSinglePlayer(e.getPlayer()), 1, 1);
                countTime.put(e.getPlayer().getUniqueId(), s);
            }
        }
        if (e.getGame().isRunning() && !PlayerManager.getParkourSinglePlayer(e.getPlayer()).isIgnoring() && e.getGame().getOptions().isModifyInventory()) {
            e.getPlayer().getInventory().setItem(8, plugin.getExit());
        }
        String tmp = msg("joinParkour", false).split("%")[0];
        String color = "�" + tmp.charAt(tmp.lastIndexOf('�') + 1);
        e.getPlayer().sendMessage(msg("joinParkour", false).replace("%PARKOUR%", e.getGame().getOptions().getDisplayName().replace('&', '�') + color));
        updateScoreboardFor(e.getPlayer(), e.getGame());
        for (ParkourGame p : ParkourManager.getAllGames()) {
            if (!p.equals(e.getGame())) {
                for (ParkourPlayer pl : p.getPlayers()) {
                    pl.getPlayer().hidePlayer(e.getPlayer());
                    e.getPlayer().hidePlayer(pl.getPlayer());
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void interact(PlayerInteractEvent e) {
        ParkourPlayer pp = PlayerManager.getParkourSinglePlayer(e.getPlayer());
        for (ParkourGame p : ParkourManager.getAllGames()) {
            if (!e.getAction().equals(Action.PHYSICAL) && p.getTeleportBlock() != null && e.getClickedBlock() != null && p.getTeleportBlock().equals(e.getClickedBlock().getLocation()) && !pp.isIgnoring()) {
                p.addPlayer(e.getPlayer());
                pp.teleportToSpawn();
                e.setCancelled(true);
                pp.reset();
                break;
            }
        }
        if (ParkourManager.getPlayersInGames().contains(e.getPlayer())) {
            if (e.getClickedBlock() != null && (e.getClickedBlock().getType().equals(Material.LEVER) || e.getClickedBlock().getType().equals(Material.WOOD_DOOR) || e.getClickedBlock().getType().equals(Material.WOOD_BUTTON) || e.getClickedBlock().getType().equals(Material.STONE_BUTTON) || e.getClickedBlock().getType().equals(Material.TRAP_DOOR) || e.getClickedBlock().getType().equals(Material.CHEST) || e.getClickedBlock().getType().equals(Material.FENCE_GATE))) {
                e.setCancelled(true);
            }

            if (e.getItem() != null && (e.getAction().equals(Action.RIGHT_CLICK_AIR) || e.getAction().equals(Action.RIGHT_CLICK_BLOCK) || e.getAction().equals(Action.LEFT_CLICK_AIR) || e.getAction().equals(Action.LEFT_CLICK_BLOCK))) {
                if (plugin.getExit().getItemMeta().getDisplayName().equals(e.getItem().getItemMeta().getDisplayName())) {
                    e.setCancelled(true);
                    PlayerManager.getParkourSinglePlayer(e.getPlayer()).teleportToLobby();
                }
            }
        }
    }

    @EventHandler
    public void quitGame(PlayerQuitGameEvent e) {
        if (countTime.containsKey(e.getPlayer().getUniqueId())) {
            Bukkit.getScheduler().cancelTask(countTime.get(e.getPlayer().getUniqueId()));
            PlayerManager.getParkourSinglePlayer(e.getPlayer()).reset();
            countTime.remove(e.getPlayer().getUniqueId());
        }
        if (e.getGame().isRunning() && !PlayerManager.getParkourSinglePlayer(e.getPlayer()).isIgnoring()) {
            e.getPlayer().getInventory().setItem(8, new ItemStack(Material.AIR));
            for (Entry<PotionEffectType, Integer> entry : e.getGame().getOptions().getEffects().entrySet()) {
                e.getPlayer().removePotionEffect(entry.getKey());
            }
        }
        if (teleportCount1.containsKey(e.getPlayer().getUniqueId())) {
            if (teleportCount2.get(e.getPlayer().getUniqueId()).getLeastTime() > 0) {
                e.getPlayer().sendMessage(msg("teleportationCanceled", false));
            }
            Bukkit.getScheduler().cancelTask(teleportCount1.get(e.getPlayer().getUniqueId()));
            teleportCount1.remove(e.getPlayer().getUniqueId());
            teleportCount2.remove(e.getPlayer().getUniqueId());
        }
        PlayerManager.remove(e.getPlayer());
        for (Entry<PotionEffectType, Integer> entry : e.getGame().getOptions().getEffects().entrySet()) {
            e.getPlayer().removePotionEffect(entry.getKey());
        }
        updateScoreboardFor(e.getPlayer(), null);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void join(PlayerJoinEvent e) {
        if (plugin.isBetaVersion()) {
            e.getPlayer().sendMessage("§4§l[ParkourPlugin]§r §cWersia developerska §lBETA§r§c, prawdopodobne błędy.");
        }
        for (ParkourGame p : ParkourManager.getAllGames()) {
            for (AbstractRegion r : p.getAllRegions()) {
                if (r.contains(e.getPlayer().getLocation()) && p.getWorld().equals(e.getPlayer().getWorld()) && !PlayerManager.getParkourSinglePlayer(e.getPlayer()).isIgnoring()) {
                    p.addPlayer(e.getPlayer());
                    return;
                }
            }
        }
        updateScoreboardFor(e.getPlayer(), null);
    }

    @EventHandler
    public void tp(PlayerTeleportEvent e) {
        for (ParkourGame p : ParkourManager.getAllGames()) {
            if (p.inAnyRegion(e.getTo()) && !p.getPlayers().contains(PlayerManager.getParkourSinglePlayer(e.getPlayer())) && !PlayerManager.getParkourSinglePlayer(e.getPlayer()).isIgnoring()) {
                p.addPlayer(e.getPlayer());
                if (!countTime.containsKey(e.getPlayer().getUniqueId()) && p.getOptions().isCountingRecords()) {
                    int s = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, PlayerManager.getParkourSinglePlayer(e.getPlayer()), 1, 1);
                    countTime.put(e.getPlayer().getUniqueId(), s);
                }
                continue;
            }
            if (!p.inAnyRegion(e.getTo()) && p.getPlayers().contains(e.getPlayer())) {
                p.removePlayer(e.getPlayer());
                countTime.remove(e.getPlayer().getUniqueId());
            }
        }
    }

    @EventHandler
    public void dmg(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player) {
            Player pl = (Player) e.getEntity();
            ParkourGame parkour = ParkourManager.getParkour(pl);
            if (parkour != null) {
                if (!parkour.getOptions().isAllowingDamage() && parkour.getWorld().equals(pl.getWorld())) {
                    e.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void destroy(BlockBreakEvent e) {
        if (e.getPlayer().hasPermission("ats.parkour.modify")) {
            return;
        }
        for (ParkourGame p : ParkourManager.getAllGames()) {
            for (AbstractRegion r : p.getAllRegions()) {
                if (r.contains(e.getBlock().getLocation()) && p.getWorld().equals(e.getPlayer().getWorld())) {
                    e.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler
    public void place(BlockPlaceEvent e) {
        if (e.getPlayer().hasPermission("ats.parkour.modify")) {
            return;
        }
        for (ParkourGame p : ParkourManager.getAllGames()) {
            for (AbstractRegion r : p.getAllRegions()) {
                if (r.contains(e.getBlock().getLocation()) && p.getWorld().equals(e.getPlayer().getWorld())) {
                    e.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler
    public void drop(PlayerDropItemEvent e) {
        if (!PlayerManager.getParkourSinglePlayer(e.getPlayer()).isIgnoring()) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void enterEffect(PlayerEnterEffectRegionEvent e) {
        List<PotionEffectType> effectsToAdd = e.getGame().getEffectRegionList().get(e.getRegionId()).getEffectsToAdd();
        List<PotionEffectType> effectsToRemove = e.getGame().getEffectRegionList().get(e.getRegionId()).getEffectsToDel();

        for (PotionEffectType p : effectsToAdd) {
            e.getPlayer().addPotionEffect(new PotionEffect(p, 72000, 10));
        }

        for (PotionEffectType p : effectsToRemove) {
            e.getPlayer().removePotionEffect(p);
        }
    }

    @EventHandler
    public void leave(PlayerQuitEvent e) {
        ParkourGame parkour = ParkourManager.getParkour(e.getPlayer());
        if (parkour != null) {
            parkour.removePlayer(e.getPlayer());
        }
        PlayerManager.remove(e.getPlayer());
    }

    public void stopScheduling(Player pl) {
        if (teleportCount1.containsKey(pl.getUniqueId())) {
            Bukkit.getScheduler().cancelTask(teleportCount1.get(pl.getUniqueId()));
            PlayerManager.getParkourSinglePlayer(pl).teleportToLobby();
        }
    }

    public void startScheduling(Player pl) {
        TeleportCount t = new TeleportCount(pl);
        int s = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, t, 0, 20);
        teleportCount1.put(pl.getUniqueId(), s);
        teleportCount2.put(pl.getUniqueId(), t);
    }

    public Map<UUID, TeleportCount> getTeleportCounts() {
        return teleportCount2;
    }

    public void updateScoreboardFor(Player pl, ParkourGame pk) {
        // final ScoreboardSystem ss =
        // PlayerManager.getParkourSinglePlayer(pl).getScoreboard();
        try {
            YamlConfiguration yml = new YamlConfiguration();
            yml.load(new File(plugin.getDataFolder().getAbsolutePath() + File.separator + "scoreboard.yml"));
            // int i = 0;
            if (pk == null) {
                if (authorTasks.containsKey(pl.getUniqueId())) {
                    authorTasks.get(pl.getUniqueId()).cancel();
                }
                for (String s : (List<String>) yml.getList("type.none")) {
                    if (s != null && !s.equals("")) {
                        String result = s;
                        // result = result.replace("%MONEY%", "" +
                        // LobbyCoins.getInstance().getCoins(pl.getName()));
                        result = result.replace("%LEVEL%", "Not implemented yet");
                        // ss.setFullLine(i++, result);
                    }
                }
            } else {
                for (String s : (List<String>) yml.getList("type." + pk.getOptions().getType().toString().toLowerCase())) {
                    if (s != null && !s.equals("")) {
                        // final int j = i++;
                        Bukkit.getScheduler().runTaskAsynchronously(plugin, new ParkourDataOperations(pk, pl, r -> {
                            String result = s;
                            result = result.replace("%PARKOUR%", "�f" + pk.getOptions().getDisplayName());
                            result = result.replace("%AUTHOR%", pk.getAuthors().size() > 0 ? pk.getAuthors().get(0) : "none");
                            result = result.replace("%PRICE%", "" + pk.getOptions().getPrice());
                            result = result.replace("%BESTTIME%", "" + r.getBestTime());
                            result = result.replace("%PLAYERTIME%", "" + r.getTime());
                            result = result.replace("%COUNT%", "" + r.getCount());
                            result = result.replace("%EARNED%", "" + r.getEarned());
                            result = result.replace("%MEDAL%", "" + pk.getOptions().getMedalByTime(r.getTime()));
                            // result = result.replace("%MONEY%", "" +
                            // LobbyCoins.getInstance().getCoins(pl.getName()));
                            result = result.replace("%TYPE%", "" + pk.getOptions().getType());
                            result = result.replace("%LEVEL%", "Not implemented yet");
                            // ss.setFullLine(j, result);
                        }));
                        if (s.contains("%AUTHOR%") && pk.getAuthors().size() > 0) {
                            // int currentAuthor;
                            authorTasks.put(pl.getUniqueId(), Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                                // ss.setFullLine(j, s.replace("%AUTHOR%",
                                // pk.getAuthors().get(currentAuthor==pk.getAuthors().size()-1?(currentAuthor=0):++currentAuthor)));
                            }, 20, 20));
                        }
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private String msg(String arg, boolean error) {
        return plugin.msg(arg, error);
    }
}
