/*
 * Copyright Andret (c) 2019. Copying and modifying allowed only keeping git link reference.
 */
package eu.andret.ats.parkour;

import com.sk89q.worldedit.bukkit.BukkitWorld;
import eu.andret.ats.parkour.event.player.PlayerAchieveCheckpointEvent;
import eu.andret.ats.parkour.event.player.PlayerCompleteParkourEvent;
import eu.andret.ats.parkour.event.player.PlayerEnterRegionEvent;
import eu.andret.ats.parkour.event.player.PlayerHitWallEvent;
import eu.andret.ats.parkour.event.player.PlayerJoinGameEvent;
import eu.andret.ats.parkour.event.player.PlayerLeaveRegionEvent;
import eu.andret.ats.parkour.event.player.PlayerQuitGameEvent;
import eu.andret.ats.parkour.event.player.PlayerTeleportBackEvent;
import eu.andret.ats.parkour.parkour.ParkourGame;
import eu.andret.ats.parkour.parkour.ParkourManager;
import eu.andret.ats.parkour.player.ParkourPlayer;
import eu.andret.ats.parkour.player.PlayerManager;
import eu.andret.ats.parkour.region.AbstractRegion;
import eu.andret.ats.parkour.region.Checkpoint;
import eu.andret.ats.parkour.region.EffectRegion;
import eu.andret.ats.parkour.region.Wall;
import eu.andret.ats.parkour.tasks.DataBaseOperations;
import eu.andret.ats.parkour.tasks.TeleportCount;
import eu.andret.ats.parkour.util.Data;
import eu.andret.ats.parkour.util.SchedulerManager;
import lombok.Value;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Boat;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
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

import java.util.List;
import java.util.Map.Entry;

@Value
public class ParkourListeners implements Listener {
	ParkourPlugin plugin;

	@EventHandler(priority = EventPriority.HIGHEST)
	public synchronized void move(final PlayerMoveEvent e) {
		final Player pl = e.getPlayer();
		final ParkourPlayer pp = PlayerManager.getParkourSinglePlayer(e.getPlayer());
		if (e.getTo().getY() < -10) {
			pp.teleportToLobby();
		}
		final ParkourGame parkour = ParkourManager.getParkour(pl);
		if (parkour != null && !pp.isIgnoring()) {
			if (pl.getWorld().equals(parkour.getWorld()) && parkour.isRunning()) {
				pl.setFoodLevel(20);
				pl.setHealth(pl.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
				for (final Entry<PotionEffectType, Integer> entry : parkour.getOptions().getEffects().entrySet()) {
					pl.addPotionEffect(new PotionEffect(entry.getKey(), 99999999, entry.getValue()));
				}
				if (!pp.isSpectating() && pl.isFlying() && !parkour.getOptions().isBoat() && !parkour.getSpawn().contains(pl.getLocation())) {
					pp.teleportToCheckpoint(pp.getLastVisitedCheckpointId());
				}
				if (!pp.isSpectating() && parkour.getOptions().isForcingSprint() && !pl.isSprinting()) {
					boolean tp = true;
					for (final Checkpoint cr : parkour.getCheckpointList()) {
						if (cr.contains(pl.getLocation())) {
							tp = false;
						}
					}

					if (tp) {
						if (parkour.getOptions().isAlwaysSpawn()) {
							pp.teleportToSpawn();
						} else {
							pp.teleportToCheckpoint(pp.getLastVisitedCheckpointId());
						}
					}
				}
			}

			for (final ParkourGame p : ParkourManager.getAllGames()) {
				for (final AbstractRegion r : p.getAllRegions()) {
					if (!pp.isSpectating() && p.isRunning() && !pp.isIgnoring() && p.getWorld().equals(parkour.getWorld())) {
						if (r.contains(e.getFrom()) && !r.contains(e.getTo())) {
							Bukkit.getPluginManager().callEvent(new PlayerLeaveRegionEvent(p, pl, r));
						} else if (!r.contains(e.getFrom()) && r.contains(e.getTo())) {
							Bukkit.getPluginManager().callEvent(new PlayerEnterRegionEvent(p, pl, r));
							if (r instanceof Checkpoint) {
								Bukkit.getPluginManager().callEvent(new PlayerAchieveCheckpointEvent(p, PlayerManager.getParkourPlayer(pl), (Checkpoint) r));
							} else if (r instanceof Wall) {
								Bukkit.getPluginManager().callEvent(new PlayerHitWallEvent(p, PlayerManager.getParkourPlayer(pl), (Wall) r));
							}
						}
					}
				}
			}
		}

		if (!e.getPlayer().getGameMode().equals(GameMode.CREATIVE)) {
			boolean fly = false;
			for (final ParkourGame p : ParkourManager.getAllGames()) {
				if (PlayerManager.getParkourSinglePlayer(pl).isSpectating()) {
					if (p.inAnyRegion(pl.getLocation())) {
						fly = true;
						for (final Entry<PotionEffectType, Integer> entry : p.getOptions().getEffects().entrySet()) {
							e.getPlayer().addPotionEffect(new PotionEffect(entry.getKey(), 99999999, entry.getValue()));
						}
						pl.setFoodLevel(20);
						pl.setHealth(20D);
					} else {
						for (final Entry<PotionEffectType, Integer> entry : p.getOptions().getEffects().entrySet()) {
							e.getPlayer().removePotionEffect(entry.getKey());
						}
					}
				}
			}
			pl.setAllowFlight(fly);
		}
		for (final ParkourGame p : ParkourManager.getAllGames()) {
			if (!pp.isSpectating() && pl.getWorld().equals(p.getWorld()) && !pp.isIgnoring()) {
				if (!p.inAnyRegion(e.getPlayer().getLocation()) && p.getPlayers().contains(PlayerManager.getParkourPlayer(e.getPlayer()))) {
					p.removePlayer(pl);
				} else if (p.inAnyRegion(e.getPlayer().getLocation()) && !p.getPlayers().contains(PlayerManager.getParkourPlayer(e.getPlayer()))) {
					p.addPlayer(pl);
				}
			}
		}
	}

	@EventHandler
	public void checkpoint(final PlayerAchieveCheckpointEvent e) {
		final ParkourGame pk = e.getParkour();
		final ParkourPlayer pp = PlayerManager.getParkourSinglePlayer(e.getPlayer().getPlayer());
		if (e.getPlayer().getPlayer().getWorld().equals(pk.getWorld())) {
			final int checkpointId = e.getParkour().getCheckpointList().indexOf(e.getCheckpoint());
			if (checkpointId > pp.getLastVisitedCheckpointId() // &&
				// !e.getPlayer().getLocation().clone().add(0, -1,
				// 0).getBlock().getType().equals(Material.AIR)
			) {
				pp.setLastVisitedCheckpointId(checkpointId);
				if (checkpointId == e.getParkour().getCheckpointList().size() - 1) {
					if (plugin.getConfig().getBoolean("last-checkpoint-info")) {
						e.getPlayer().getPlayer().sendMessage(plugin.msg("achieveCheckpoint", false));
					}
				} else {
					e.getPlayer().getPlayer().sendMessage(plugin.msg("achieveCheckpoint", false));
				}
			}
			if (checkpointId == pk.getLastCheckpointId()) {
				Bukkit.getPluginManager().callEvent(new PlayerCompleteParkourEvent(e.getParkour(), e.getPlayer()));
			}
			if (checkpointId == 0) {
				PlayerManager.getParkourSinglePlayer(e.getPlayer().getPlayer()).reset();
			}
		}
	}

	@EventHandler
	public void enterRegion(final PlayerEnterRegionEvent e) {
		if (e.getRegion() instanceof EffectRegion) {
			final EffectRegion region = (EffectRegion) e.getRegion();
			final List<PotionEffectType> effectsToAdd = region.getEffectsToAdd();
			final List<PotionEffectType> effectsToRemove = region.getEffectsToDel();

			for (final PotionEffectType p : effectsToAdd) {
				e.getPlayer().getPlayer().addPotionEffect(new PotionEffect(p, 72000, 10));
			}

			for (final PotionEffectType p : effectsToRemove) {
				e.getPlayer().getPlayer().removePotionEffect(p);
			}
		}
	}

	@EventHandler
	public void complete(final PlayerCompleteParkourEvent e) {
		final Player player = e.getPlayer().getPlayer();
		player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5F, 0.5F);
		if (!SchedulerManager.TELEPORT_COUNT_2.containsKey(player.getUniqueId())) {
			if (e.getParkour().getOptions().isCountingRecords()) {
				final float curr = PlayerManager.getParkourSinglePlayer(player).getTime();
				if (curr < e.getParkour().getOptions().getFair()) {
					Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), "tban " + player.getName() + " 1");
					return;
				}
				final String time = String.valueOf(curr);
				player.sendMessage(plugin.msg("finishTime", false).replace("%TIME%", Math.abs(time.lastIndexOf('.') - time.length()) == 2 ? (time + "0") : time));
				Bukkit.getScheduler().runTaskAsynchronously(plugin, new DataBaseOperations(plugin, player, e.getParkour(), curr));
			}
			Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> startScheduling(player), 5L);
		}
	}

	@EventHandler
	public void back(final PlayerTeleportBackEvent e) {
		if (e.getCheckpoint() == e.getParkour().getSpawn()) {
			PlayerManager.getParkourSinglePlayer(e.getPlayer().getPlayer()).reset();
		}
		if (e.getParkour().getOptions().isBoat()) {
			final Boat b = (Boat) e.getPlayer().getPlayer().getLocation().getWorld().spawnEntity(e.getPlayer().getPlayer().getLocation(), EntityType.BOAT);
			b.addPassenger(e.getPlayer().getPlayer());
		}
	}

	@EventHandler
	public void leaveBoat(final VehicleExitEvent e) {
		if (e.getExited() instanceof Player) {
			final Player player = (Player) e.getExited();
			final ParkourGame parkour = ParkourManager.getParkour(player);
			if (parkour != null && PlayerManager.getParkourPlayer(player) != null && parkour.getOptions().isBoat() && e.getVehicle() instanceof Boat && parkour.isRunning() && player.getWorld().equals(parkour.getWorld())) {
				e.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void wall(final PlayerHitWallEvent e) {
		final ParkourPlayer pp = PlayerManager.getParkourSinglePlayer(e.getPlayer().getPlayer());
		if (e.getParkour().getOptions().isAlwaysSpawn()) {
			pp.teleportToSpawn();
		} else {
			pp.teleportToCheckpoint(pp.getLastVisitedCheckpointId());
		}
	}

	@EventHandler
	public synchronized void joinGame(final PlayerJoinGameEvent e) {
		if (e.getParkour().getOptions().isCountingRecords() && !e.getPlayer().getPlayer().getGameMode().equals(GameMode.CREATIVE)) {
			e.getPlayer().getPlayer().setLevel(0);
			e.getPlayer().getPlayer().setExp(0);
			if (!SchedulerManager.COUNT_TIME.containsKey(e.getPlayer().getPlayer().getUniqueId()) && !PlayerManager.getParkourSinglePlayer(e.getPlayer().getPlayer()).isSpectating()) {
				final int s = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, PlayerManager.getParkourSinglePlayer(e.getPlayer().getPlayer()), 1, 1);
				SchedulerManager.COUNT_TIME.put(e.getPlayer().getPlayer().getUniqueId(), s);
			}
		}
		if (e.getParkour().isRunning() && !PlayerManager.getParkourSinglePlayer(e.getPlayer().getPlayer()).isIgnoring() && e.getParkour().getOptions().isModifyInventory()) {
			e.getPlayer().getPlayer().getInventory().setItem(8, plugin.getExit());
		}
		final String tmp = plugin.msg("joinParkour", false).split("%")[0];
		final String color = "\u00A7" + tmp.charAt(tmp.lastIndexOf('\u00A7') + 1);
		e.getPlayer().getPlayer().sendMessage(plugin.msg("joinParkour", false).replace("%PARKOUR%", e.getParkour().getOptions().getDisplayName().replace('&', '\u00A7') + color));
		for (final ParkourGame p : ParkourManager.getAllGames()) {
			if (!p.equals(e.getParkour())) {
				for (final ParkourPlayer pl : p.getPlayers()) {
					pl.getPlayer().hidePlayer(plugin, e.getPlayer().getPlayer());
					e.getPlayer().getPlayer().hidePlayer(plugin, pl.getPlayer());
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void interact(final PlayerInteractEvent e) {
		final ParkourPlayer pp = PlayerManager.getParkourSinglePlayer(e.getPlayer());
		for (final ParkourGame p : ParkourManager.getAllGames()) {
			if (!e.getAction().equals(Action.PHYSICAL) && p.getTeleportBlock() != null && e.getClickedBlock() != null && p.getTeleportBlock().equals(e.getClickedBlock().getLocation()) && !pp.isIgnoring()) {
				p.addPlayer(e.getPlayer());
				pp.teleportToSpawn();
				e.setCancelled(true);
				pp.reset();
				break;
			}
		}
		if (!ParkourManager.getPlayersInGames().contains(e.getPlayer())) {
			return;
		}
		if (e.getClickedBlock() != null && (Data.getInteractiveMaterials().contains(e.getClickedBlock().getType()))) {
			e.setCancelled(true);
		}

		if (e.getItem() != null && (e.getAction().equals(Action.RIGHT_CLICK_AIR) || e.getAction().equals(Action.RIGHT_CLICK_BLOCK) || e.getAction().equals(Action.LEFT_CLICK_AIR) || e.getAction().equals(Action.LEFT_CLICK_BLOCK)) && plugin.getExit().getItemMeta().getDisplayName().equals(e.getItem().getItemMeta().getDisplayName())) {
			e.setCancelled(true);
			PlayerManager.getParkourSinglePlayer(e.getPlayer()).teleportToLobby();
		}
	}

	@EventHandler
	public void quitGame(final PlayerQuitGameEvent e) {
		if (SchedulerManager.COUNT_TIME.containsKey(e.getPlayer().getPlayer().getUniqueId())) {
			Bukkit.getScheduler().cancelTask(SchedulerManager.COUNT_TIME.get(e.getPlayer().getPlayer().getUniqueId()));
			PlayerManager.getParkourSinglePlayer(e.getPlayer().getPlayer()).reset();
			SchedulerManager.COUNT_TIME.remove(e.getPlayer().getPlayer().getUniqueId());
		}
		if (e.getParkour().isRunning() && !PlayerManager.getParkourSinglePlayer(e.getPlayer().getPlayer()).isIgnoring()) {
			e.getPlayer().getPlayer().getInventory().setItem(8, new ItemStack(Material.AIR));
			for (final Entry<PotionEffectType, Integer> entry : e.getParkour().getOptions().getEffects().entrySet()) {
				e.getPlayer().getPlayer().removePotionEffect(entry.getKey());
			}
		}
		if (SchedulerManager.TELEPORT_COUNT.containsKey(e.getPlayer().getPlayer().getUniqueId())) {
			if (SchedulerManager.TELEPORT_COUNT_2.get(e.getPlayer().getPlayer().getUniqueId()).getLeastTime() > 0) {
				e.getPlayer().getPlayer().sendMessage(plugin.msg("teleportationCanceled", false));
			}
			Bukkit.getScheduler().cancelTask(SchedulerManager.TELEPORT_COUNT.get(e.getPlayer().getPlayer().getUniqueId()));
			SchedulerManager.TELEPORT_COUNT.remove(e.getPlayer().getPlayer().getUniqueId());
			SchedulerManager.TELEPORT_COUNT_2.remove(e.getPlayer().getPlayer().getUniqueId());
		}
		PlayerManager.remove(e.getPlayer().getPlayer());
		for (final Entry<PotionEffectType, Integer> entry : e.getParkour().getOptions().getEffects().entrySet()) {
			e.getPlayer().getPlayer().removePotionEffect(entry.getKey());
		}
	}

	@EventHandler(priority = EventPriority.LOW)
	public void join(final PlayerJoinEvent e) {
		for (final ParkourGame p : ParkourManager.getAllGames()) {
			for (final AbstractRegion r : p.getAllRegions()) {
				if (r.contains(e.getPlayer().getLocation()) && p.getWorld().equals(e.getPlayer().getWorld()) && !PlayerManager.getParkourSinglePlayer(e.getPlayer()).isIgnoring()) {
					p.addPlayer(e.getPlayer());
					return;
				}
			}
		}
	}

	@EventHandler
	public void tp(final PlayerTeleportEvent e) {
		for (final ParkourGame p : ParkourManager.getAllGames()) {
			if (p.inAnyRegion(e.getTo()) && !p.getPlayers().contains(PlayerManager.getParkourSinglePlayer(e.getPlayer())) && !PlayerManager.getParkourSinglePlayer(e.getPlayer()).isIgnoring()) {
				p.addPlayer(e.getPlayer());
				if (!SchedulerManager.COUNT_TIME.containsKey(e.getPlayer().getUniqueId()) && p.getOptions().isCountingRecords()) {
					final int s = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, PlayerManager.getParkourSinglePlayer(e.getPlayer()), 1, 1);
					SchedulerManager.COUNT_TIME.put(e.getPlayer().getUniqueId(), s);
				}
				continue;
			}
			if (!p.inAnyRegion(e.getTo()) && p.getPlayers().contains(PlayerManager.getParkourPlayer(e.getPlayer()))) {
				p.removePlayer(e.getPlayer());
				SchedulerManager.COUNT_TIME.remove(e.getPlayer().getUniqueId());
			}
		}
	}

	@EventHandler
	public void dmg(final EntityDamageEvent e) {
		if (e.getEntity() instanceof Player) {
			final Player pl = (Player) e.getEntity();
			final ParkourGame parkour = ParkourManager.getParkour(pl);
			if (parkour != null && !parkour.getOptions().isAllowingDamage() && parkour.getWorld().equals(pl.getWorld())) {
				e.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void destroy(final BlockBreakEvent e) {
		if (e.getPlayer().hasPermission("ats.parkour.modify")) {
			return;
		}
		ParkourManager.getAllGames().stream()
				.flatMap(p -> p.getAllRegions().stream())
				.filter(r -> r.contains(e.getBlock().getLocation()) && r.getRegion().getWorld().equals(new BukkitWorld(e.getPlayer().getWorld())))
				.findFirst().ifPresent(r -> e.setCancelled(true));
	}

	@EventHandler
	public void place(final BlockPlaceEvent e) {
		if (e.getPlayer().hasPermission("ats.parkour.modify")) {
			return;
		}
		ParkourManager.getAllGames().stream()
				.flatMap(p -> p.getAllRegions().stream())
				.filter(r -> r.contains(e.getBlock().getLocation()) && r.getRegion().getWorld().equals(new BukkitWorld(e.getPlayer().getWorld())))
				.findFirst().ifPresent(r -> e.setCancelled(true));
	}

	@EventHandler
	public void drop(final PlayerDropItemEvent e) {
		if (!PlayerManager.getParkourSinglePlayer(e.getPlayer()).isIgnoring()) {
			e.setCancelled(true);
		}
	}

	@EventHandler
	public void leave(final PlayerQuitEvent e) {
		final ParkourGame parkour = ParkourManager.getParkour(e.getPlayer());
		if (parkour != null) {
			parkour.removePlayer(e.getPlayer());
		}
		PlayerManager.remove(e.getPlayer());
	}

	public void stopScheduling(final Player pl) {
		if (SchedulerManager.TELEPORT_COUNT.containsKey(pl.getUniqueId())) {
			Bukkit.getScheduler().cancelTask(SchedulerManager.TELEPORT_COUNT.get(pl.getUniqueId()));
			PlayerManager.getParkourSinglePlayer(pl).teleportToLobby();
		}
	}

	public void startScheduling(final Player pl) {
		final TeleportCount t = new TeleportCount(plugin, PlayerManager.getParkourPlayer(pl));
		final int s = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, t, 0, 20);
		SchedulerManager.TELEPORT_COUNT.put(pl.getUniqueId(), s);
		SchedulerManager.TELEPORT_COUNT_2.put(pl.getUniqueId(), t);
	}
}
