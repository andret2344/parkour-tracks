package eu.andret.atsparkour;

import static eu.andret.atsparkour.atsParkour.msg;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Damageable;
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

import eu.andret.atsparkour.data.Callback;
import eu.andret.atsparkour.event.PlayerAchieveCheckpointEvent;
import eu.andret.atsparkour.event.PlayerCompleteParkourEvent;
import eu.andret.atsparkour.event.PlayerEnterEffectRegionEvent;
import eu.andret.atsparkour.event.PlayerEnterRegoinEvent;
import eu.andret.atsparkour.event.PlayerHitWallEvent;
import eu.andret.atsparkour.event.PlayerJoinGameEvent;
import eu.andret.atsparkour.event.PlayerLeaveRegoinEvent;
import eu.andret.atsparkour.event.PlayerQuitGameEvent;
import eu.andret.atsparkour.event.PlayerTeleportBackEvent;
import eu.andret.atsparkour.parkour.ParkourGame;
import eu.andret.atsparkour.parkour.ParkourManager;
import eu.andret.atsparkour.player.ParkourPlayer;
import eu.andret.atsparkour.player.PlayerManager;
import eu.andret.atsparkour.region.AbstractRegion;
import eu.andret.atsparkour.region.Checkpoint;
import eu.andret.atsparkour.region.Wall;
import eu.andret.atsparkour.tasks.DataBaseOperations;
import eu.andret.atsparkour.tasks.ParkourDataOperations;
import eu.andret.atsparkour.tasks.TeleportCount;

public class ParkourListeners implements Listener {

	private final Map<UUID, Integer> countTime = new HashMap<UUID, Integer>();

	private final Map<UUID, Integer> teleportCount1 = new HashMap<UUID, Integer>();
	private final Map<UUID, TeleportCount> teleportCount2 = new HashMap<UUID, TeleportCount>();

	private final Map<UUID, BukkitTask> authorTasks = new HashMap<UUID, BukkitTask>();

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
					pl.setHealth(((Damageable) pl).getMaxHealth());
					for (Entry<PotionEffectType, Integer> entry : parkour.getOptions().getEffects().entrySet()) {
						pl.addPotionEffect(new PotionEffect(entry.getKey(), 99999999, entry.getValue()));
					}
					if (!pp.isSpectating() && pl.isFlying() && !parkour.getOptions().isBoats() && !parkour.getSpawn().contains(pl)) {
						pp.teleportToCheckpoint(pp.getLastVisitedCheckpoint());
					}
					if (!pp.isSpectating() && parkour.getOptions().mustSprint() && !pl.isSprinting()) {
						boolean tp = true;
						for (Checkpoint cr : parkour.getCheckpointList()) {
							if (cr.contains(pl)) {
								tp = false;
							}
						}

						if (tp) {
							if (parkour.getOptions().alwaysTpToSpawn()) {
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
								Bukkit.getPluginManager().callEvent(new PlayerLeaveRegoinEvent(pl, p, r));
							} else if (!r.contains(e.getFrom()) && r.contains(e.getTo())) {
								Bukkit.getPluginManager().callEvent(new PlayerEnterRegoinEvent(pl, p, r));
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
					if (p.getOptions().isVip() && !atsParkour.getInstance().isVip(e.getPlayer())) {
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
				pp.setLastVisitedChecpoint(e.getCheckpointId());
				if (e.getCheckpointId() == e.getGame().getCheckpointList().size() - 1) {
					if (atsParkour.getInstance().getConfig().getBoolean("last-checkpoint-info")) {
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
	public void complete(final PlayerCompleteParkourEvent e) {
		final Player pl = e.getPlayer();
		pl.playSound(pl.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5F, 0.5F);
		if (!atsParkour.getInstance().getListeners().getTeleportCounts().containsKey(e.getPlayer().getUniqueId())) {
			if (e.getGame().getOptions().countRecords()) {
				float curr = PlayerManager.getParkourSinglePlayer(e.getPlayer()).getTime();
				if (curr < e.getGame().getOptions().getFair()) {
					Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), "tban " + pl.getName() + " 1");
					return;
				}
				String time = String.valueOf(curr);
				pl.sendMessage(msg("finishTime", false).replace("%TIME%", String.valueOf(Math.abs(time.lastIndexOf(".") - time.length()) == 2 ? (time + "0") : time)));
				Bukkit.getScheduler().runTaskAsynchronously(atsParkour.getInstance(), new DataBaseOperations(pl, e.getGame(), curr));
			}
			if (atsParkour.getInstance().isLobbyCoins() && e.getGame().getOptions().getRewardPrice() > 0) {
				int i = e.getGame().getOptions().getRewardPrice();
				// LobbyCoins.getInstance().addCoins(pl.getName(), i);
				e.getPlayer().sendMessage(msg("rewardPrice", false).replace("%PRICE%", String.valueOf(i)));
			}
			Bukkit.getScheduler().scheduleSyncDelayedTask(atsParkour.getInstance(), new Runnable() {
				@Override
				public void run() {
					ParkourListeners.this.startScheduling(pl);
				}
			}, 5L);
		}
		updateScoreboardFor(e.getPlayer(), e.getGame());
	}

	@EventHandler
	public void back(PlayerTeleportBackEvent e) {
		if (e.getCheckpointId() == 0) {
			PlayerManager.getParkourSinglePlayer(e.getPlayer()).reset();
		}
		if (e.getGame().getOptions().isBoats()) {
			Boat b = (Boat) e.getPlayer().getLocation().getWorld().spawnEntity(e.getPlayer().getLocation(), EntityType.BOAT);
			b.setPassenger(e.getPlayer());
		}
	}

	@EventHandler
	public void leaveBoat(VehicleExitEvent e) {
		if (e.getExited() instanceof Player) {
			Player player = (Player) e.getExited();
			ParkourGame parkour = ParkourManager.getParkour(player);
			if (PlayerManager.playerExists(player) && parkour.getOptions().isBoats() && e.getVehicle() instanceof Boat && parkour.isRunning()) {
				if (player.getWorld().equals(parkour.getWorld())) {
					e.setCancelled(true);
				}
			}
		}
	}

	@EventHandler
	public void wall(PlayerHitWallEvent e) {
		ParkourPlayer pp = PlayerManager.getParkourSinglePlayer(e.getPlayer());
		if (e.getGame().getOptions().alwaysTpToSpawn()) {
			pp.teleportToSpawn();
		} else {
			pp.teleportToCheckpoint(pp.getLastVisitedCheckpoint());
		}
	}

	@EventHandler
	public synchronized void joinGame(PlayerJoinGameEvent e) {
		if (e.getGame().getOptions().countRecords() && !e.getPlayer().getGameMode().equals(GameMode.CREATIVE)) {
			e.getPlayer().setLevel(0);
			e.getPlayer().setExp(0);
			if (!countTime.containsKey(e.getPlayer().getUniqueId()) && !PlayerManager.getParkourSinglePlayer(e.getPlayer()).isSpectating()) {
				int s = Bukkit.getScheduler().scheduleSyncRepeatingTask(atsParkour.getInstance(), PlayerManager.getParkourSinglePlayer(e.getPlayer()), 1, 1);
				countTime.put(e.getPlayer().getUniqueId(), s);
			}
		}
		if (e.getGame().isRunning() && !PlayerManager.getParkourSinglePlayer(e.getPlayer()).isIgnoring() && e.getGame().getOptions().isModifyeq()) {
			e.getPlayer().getInventory().setItem(8, atsParkour.getInstance().getExitdoor());
		}
		String tmp = msg("joinParkour", false).split("%")[0];
		String color = "§" + tmp.charAt(tmp.lastIndexOf('§') + 1);
		e.getPlayer().sendMessage(msg("joinParkour", false).replace("%PARKOUR%", e.getGame().getOptions().getDisplayName().replace('&', '§') + color));
		updateScoreboardFor(e.getPlayer(), e.getGame());
		for (ParkourGame p : ParkourManager.getAllGames()) {
			if (!p.equals(e.getGame())) {
				for (Player pl : p.getPlayers()) {
					pl.hidePlayer(e.getPlayer());
					e.getPlayer().hidePlayer(pl);
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void interact(PlayerInteractEvent e) {
		ParkourPlayer pp = PlayerManager.getParkourSinglePlayer(e.getPlayer());
		for (ParkourGame p : ParkourManager.getAllGames()) {
			if (!e.getAction().equals(Action.PHYSICAL) && p.getTeleportBlockLocation() != null && e.getClickedBlock() != null && p.getTeleportBlockLocation().equals(e.getClickedBlock().getLocation()) && !pp.isIgnoring()) {
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
				if (atsParkour.getInstance().getExitdoor().getItemMeta().getDisplayName().equals(e.getItem().getItemMeta().getDisplayName())) {
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
		if (atsParkour.getInstance().isBetaVersion()) {
			e.getPlayer().sendMessage("§4§l[Parkour]§r §cWersia developerska §lBETA§r§c, prawdopodobne b³êdy.");
		}
		for (ParkourGame p : ParkourManager.getAllGames()) {
			for (AbstractRegion r : p.getAllRegions()) {
				if (r.contains(e.getPlayer()) && p.getWorld().equals(e.getPlayer().getWorld()) && !PlayerManager.getParkourSinglePlayer(e.getPlayer()).isIgnoring()) {
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
			if (p.inAnyRegion(e.getTo()) && !p.getPlayers().contains(e.getPlayer()) && !PlayerManager.getParkourSinglePlayer(e.getPlayer()).isIgnoring()) {
				p.addPlayer(e.getPlayer());
				if (!countTime.containsKey(e.getPlayer().getUniqueId()) && p.getOptions().countRecords()) {
					int s = Bukkit.getScheduler().scheduleSyncRepeatingTask(atsParkour.getInstance(), PlayerManager.getParkourSinglePlayer(e.getPlayer()), 1, 1);
					countTime.put(e.getPlayer().getUniqueId(), s);
				}
				continue;
			}
			if (!p.inAnyRegion(e.getTo()) && p.getPlayers().contains(e.getPlayer())) {
				p.removePlayer(e.getPlayer());
				countTime.remove(e.getPlayer().getUniqueId());
				continue;
			}
		}
	}

	@EventHandler
	public void dmg(EntityDamageEvent e) {
		if (e.getEntity() instanceof Player) {
			Player pl = (Player) e.getEntity();
			ParkourGame parkour = ParkourManager.getParkour(pl);
			if (parkour != null) {
				if (!parkour.getOptions().isDamage() && parkour.getWorld().equals(pl.getWorld())) {
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
		for (PotionEffectType p : e.getAdditableEffects()) {
			e.getPlayer().addPotionEffect(new PotionEffect(p, 72000, 10));
		}

		for (PotionEffectType p : e.getRemovableEffects()) {
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
		int s = Bukkit.getScheduler().scheduleSyncRepeatingTask(atsParkour.getInstance(), t, 0, 20);
		teleportCount1.put(pl.getUniqueId(), s);
		teleportCount2.put(pl.getUniqueId(), t);
	}

	public Map<UUID, TeleportCount> getTeleportCounts() {
		return teleportCount2;
	}

	@SuppressWarnings("unchecked")
	public void updateScoreboardFor(final Player pl, final ParkourGame pk) {
		// final ScoreboardSystem ss =
		// PlayerManager.getParkourSinglePlayer(pl).getScoreboard();
		try {
			YamlConfiguration yml = new YamlConfiguration();
			yml.load(new File(atsParkour.getInstance().getDataFolder().getAbsolutePath() + File.separator + "scoreboard.yml"));
			// int i = 0;
			if (pk == null) {
				if (authorTasks.containsKey(pl.getUniqueId())) {
					authorTasks.get(pl.getUniqueId()).cancel();
				}
				for (String s : (List<String>) yml.getList("type.none")) {
					if (s != null && !s.equals("")) {
						String result = new String(s);
						// result = result.replace("%MONEY%", "" +
						// LobbyCoins.getInstance().getCoins(pl.getName()));
						result = result.replace("%LEVEL%", "Not implemented yet");
						// ss.setFullLine(i++, result);
					}
				}
			} else {
				for (final String s : (List<String>) yml.getList("type." + pk.getOptions().getType().toString().toLowerCase())) {
					if (s != null && !s.equals("")) {
						// final int j = i++;
						Bukkit.getScheduler().runTaskAsynchronously(atsParkour.getInstance(), new ParkourDataOperations(pk, pl, new Callback<ParkourDataOperations>() {
							@Override
							public void run(ParkourDataOperations r) {
								String result = new String(s);
								result = result.replace("%PARKOUR%", "§f" + pk.getOptions().getDisplayName());
								result = result.replace("%AUTHOR%", pk.getAuthors().size() > 0 ? pk.getAuthors().get(0) : "none");
								result = result.replace("%PRICE%", "" + pk.getOptions().getRewardPrice());
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
							}
						}));
						if (s.contains("%AUTHOR%") && pk.getAuthors().size() > 0) {
							authorTasks.put(pl.getUniqueId(), Bukkit.getScheduler().runTaskTimer(atsParkour.getInstance(), new Runnable() {
								// int currentAuthor;
								@Override
								public void run() {
									// ss.setFullLine(j, s.replace("%AUTHOR%",
									// pk.getAuthors().get(currentAuthor==pk.getAuthors().size()-1?(currentAuthor=0):++currentAuthor)));
								}
							}, 20, 20));
						}
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
