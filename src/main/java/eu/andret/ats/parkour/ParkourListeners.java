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
	public synchronized void move(final PlayerMoveEvent event) {
		final Player pl = event.getPlayer();
		final ParkourPlayer pp = PlayerManager.getParkourSinglePlayer(event.getPlayer());
		if (event.getTo().getY() < -10) {
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
				if (!pp.isSpectating() && parkour.getOptions().isSprintForced() && !pl.isSprinting()) {
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
						if (r.contains(event.getFrom()) && !r.contains(event.getTo())) {
							plugin.getServer().getPluginManager().callEvent(new PlayerLeaveRegionEvent(p, pl, r));
						} else if (!r.contains(event.getFrom()) && r.contains(event.getTo())) {
							plugin.getServer().getPluginManager().callEvent(new PlayerEnterRegionEvent(p, pl, r));
							if (r instanceof Checkpoint) {
								plugin.getServer().getPluginManager().callEvent(new PlayerAchieveCheckpointEvent(p, PlayerManager.getParkourPlayer(pl), (Checkpoint) r));
							} else if (r instanceof Wall) {
								plugin.getServer().getPluginManager().callEvent(new PlayerHitWallEvent(p, PlayerManager.getParkourPlayer(pl), (Wall) r));
							}
						}
					}
				}
			}
		}

		if (!event.getPlayer().getGameMode().equals(GameMode.CREATIVE)) {
			boolean fly = false;
			for (final ParkourGame p : ParkourManager.getAllGames()) {
				if (PlayerManager.getParkourSinglePlayer(pl).isSpectating()) {
					if (p.inAnyRegion(pl.getLocation())) {
						fly = true;
						for (final Entry<PotionEffectType, Integer> entry : p.getOptions().getEffects().entrySet()) {
							event.getPlayer().addPotionEffect(new PotionEffect(entry.getKey(), 99999999, entry.getValue()));
						}
						pl.setFoodLevel(20);
						pl.setHealth(20D);
					} else {
						for (final Entry<PotionEffectType, Integer> entry : p.getOptions().getEffects().entrySet()) {
							event.getPlayer().removePotionEffect(entry.getKey());
						}
					}
				}
			}
			pl.setAllowFlight(fly);
		}
		for (final ParkourGame p : ParkourManager.getAllGames()) {
			if (!pp.isSpectating() && pl.getWorld().equals(p.getWorld()) && !pp.isIgnoring()) {
				if (!p.inAnyRegion(event.getPlayer().getLocation()) && p.getPlayers().contains(PlayerManager.getParkourPlayer(event.getPlayer()))) {
					p.removePlayer(pl);
				} else if (p.inAnyRegion(event.getPlayer().getLocation()) && !p.getPlayers().contains(PlayerManager.getParkourPlayer(event.getPlayer()))) {
					p.addPlayer(pl);
				}
			}
		}
	}

	@EventHandler
	public void checkpoint(final PlayerAchieveCheckpointEvent event) {
		final ParkourGame pk = event.getParkourGame();
		final ParkourPlayer pp = PlayerManager.getParkourSinglePlayer(event.getParkourPlayer().getPlayer());
		if (event.getParkourPlayer().getPlayer().getWorld().equals(pk.getWorld())) {
			final int checkpointId = event.getParkourGame().getCheckpointList().indexOf(event.getCheckpoint());
			if (checkpointId > pp.getLastVisitedCheckpointId() // &&
				// !e.getPlayer().getLocation().clone().add(0, -1,
				// 0).getBlock().getType().equals(Material.AIR)
			) {
				pp.setLastVisitedCheckpointId(checkpointId);
				if (checkpointId == event.getParkourGame().getCheckpointList().size() - 1) {
					if (plugin.getConfig().getBoolean("last-checkpoint-info")) {
						event.getParkourPlayer().getPlayer().sendMessage(plugin.msg("achieveCheckpoint", false));
					}
				} else {
					event.getParkourPlayer().getPlayer().sendMessage(plugin.msg("achieveCheckpoint", false));
				}
			}
			if (checkpointId == pk.getLastCheckpointId()) {
				plugin.getServer().getPluginManager().callEvent(new PlayerCompleteParkourEvent(event.getParkourGame(), event.getParkourPlayer()));
			}
			if (checkpointId == 0) {
				PlayerManager.getParkourSinglePlayer(event.getParkourPlayer().getPlayer()).reset();
			}
		}
	}

	@EventHandler
	public void enterRegion(final PlayerEnterRegionEvent event) {
		if (event.getAbstractRegion() instanceof EffectRegion) {
			final EffectRegion region = (EffectRegion) event.getAbstractRegion();
			final List<PotionEffectType> effectsToAdd = region.getEffectsToAdd();
			final List<PotionEffectType> effectsToRemove = region.getEffectsToDel();

			for (final PotionEffectType p : effectsToAdd) {
				event.getPlayer().getPlayer().addPotionEffect(new PotionEffect(p, 72000, 10));
			}

			for (final PotionEffectType p : effectsToRemove) {
				event.getPlayer().getPlayer().removePotionEffect(p);
			}
		}
	}

	@EventHandler
	public void complete(final PlayerCompleteParkourEvent event) {
		final Player player = event.getParkourPlayer().getPlayer();
		player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5F, 0.5F);
		if (!SchedulerManager.TELEPORT_COUNT_2.containsKey(player.getUniqueId())) {
			if (event.getParkourGame().getOptions().isRecordsCounting()) {
				final float curr = PlayerManager.getParkourSinglePlayer(player).getTime();
				if (curr < event.getParkourGame().getOptions().getFair()) {
					plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), "tban " + player.getName() + " 1");
					return;
				}
				final String time = String.valueOf(curr);
				player.sendMessage(plugin.msg("finishTime", false).replace("%TIME%", Math.abs(time.lastIndexOf('.') - time.length()) == 2 ? (time + "0") : time));
				plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new DataBaseOperations(plugin, player, event.getParkourGame(), curr));
			}
			plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> startScheduling(player), 5L);
		}
	}

	@EventHandler
	public void back(final PlayerTeleportBackEvent event) {
		if (event.getCheckpoint() == event.getParkourGame().getSpawn()) {
			PlayerManager.getParkourSinglePlayer(event.getParkourPlayer().getPlayer()).reset();
		}
		if (event.getParkourGame().getOptions().isBoat()) {
			final Boat b = (Boat) event.getParkourPlayer().getPlayer().getLocation().getWorld().spawnEntity(event.getParkourPlayer().getPlayer().getLocation(), EntityType.BOAT);
			b.addPassenger(event.getParkourPlayer().getPlayer());
		}
	}

	@EventHandler
	public void leaveBoat(final VehicleExitEvent event) {
		if (event.getExited() instanceof Player) {
			final Player player = (Player) event.getExited();
			final ParkourGame parkour = ParkourManager.getParkour(player);
			if (parkour != null && PlayerManager.getParkourPlayer(player) != null && parkour.getOptions().isBoat() && event.getVehicle() instanceof Boat && parkour.isRunning() && player.getWorld().equals(parkour.getWorld())) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void wall(final PlayerHitWallEvent event) {
		final ParkourPlayer pp = PlayerManager.getParkourSinglePlayer(event.getParkourPlayer().getPlayer());
		if (event.getParkourGame().getOptions().isAlwaysSpawn()) {
			pp.teleportToSpawn();
		} else {
			pp.teleportToCheckpoint(pp.getLastVisitedCheckpointId());
		}
	}

	@EventHandler
	public synchronized void joinGame(final PlayerJoinGameEvent event) {
		if (event.getParkourGame().getOptions().isRecordsCounting() && !event.getParkourPlayer().getPlayer().getGameMode().equals(GameMode.CREATIVE)) {
			event.getParkourPlayer().getPlayer().setLevel(0);
			event.getParkourPlayer().getPlayer().setExp(0);
			if (!SchedulerManager.COUNT_TIME.containsKey(event.getParkourPlayer().getPlayer().getUniqueId()) && !PlayerManager.getParkourSinglePlayer(event.getParkourPlayer().getPlayer()).isSpectating()) {
				final int s = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, PlayerManager.getParkourSinglePlayer(event.getParkourPlayer().getPlayer()), 1, 1);
				SchedulerManager.COUNT_TIME.put(event.getParkourPlayer().getPlayer().getUniqueId(), s);
			}
		}
		if (event.getParkourGame().isRunning() && !PlayerManager.getParkourSinglePlayer(event.getParkourPlayer().getPlayer()).isIgnoring() && event.getParkourGame().getOptions().isModifyInventory()) {
			event.getParkourPlayer().getPlayer().getInventory().setItem(8, plugin.getExit());
		}
		final String tmp = plugin.msg("joinParkour", false).split("%")[0];
		final String color = "\u00A7" + tmp.charAt(tmp.lastIndexOf('\u00A7') + 1);
		event.getParkourPlayer().getPlayer().sendMessage(plugin.msg("joinParkour", false).replace("%PARKOUR%", event.getParkourGame().getOptions().getDisplayName().replace('&', '\u00A7') + color));
		for (final ParkourGame p : ParkourManager.getAllGames()) {
			if (!p.equals(event.getParkourGame())) {
				for (final ParkourPlayer pl : p.getPlayers()) {
					pl.getPlayer().hidePlayer(plugin, event.getParkourPlayer().getPlayer());
					event.getParkourPlayer().getPlayer().hidePlayer(plugin, pl.getPlayer());
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void interact(final PlayerInteractEvent event) {
		final ParkourPlayer pp = PlayerManager.getParkourSinglePlayer(event.getPlayer());
		for (final ParkourGame p : ParkourManager.getAllGames()) {
			if (!event.getAction().equals(Action.PHYSICAL) && p.getTeleportBlock() != null && event.getClickedBlock() != null && p.getTeleportBlock().equals(event.getClickedBlock().getLocation()) && !pp.isIgnoring()) {
				p.addPlayer(event.getPlayer());
				pp.teleportToSpawn();
				event.setCancelled(true);
				pp.reset();
				break;
			}
		}
		if (!ParkourManager.getPlayersInGames().contains(event.getPlayer())) {
			return;
		}
		if (event.getClickedBlock() != null && (Data.getInteractiveMaterials().contains(event.getClickedBlock().getType()))) {
			event.setCancelled(true);
		}

		if (event.getItem() != null && (event.getAction().equals(Action.RIGHT_CLICK_AIR) || event.getAction().equals(Action.RIGHT_CLICK_BLOCK) || event.getAction().equals(Action.LEFT_CLICK_AIR) || event.getAction().equals(Action.LEFT_CLICK_BLOCK)) && plugin.getExit().getItemMeta().getDisplayName().equals(event.getItem().getItemMeta().getDisplayName())) {
			event.setCancelled(true);
			PlayerManager.getParkourSinglePlayer(event.getPlayer()).teleportToLobby();
		}
	}

	@EventHandler
	public void quitGame(final PlayerQuitGameEvent event) {
		if (SchedulerManager.COUNT_TIME.containsKey(event.getParkourPlayer().getPlayer().getUniqueId())) {
			plugin.getServer().getScheduler().cancelTask(SchedulerManager.COUNT_TIME.get(event.getParkourPlayer().getPlayer().getUniqueId()));
			PlayerManager.getParkourSinglePlayer(event.getParkourPlayer().getPlayer()).reset();
			SchedulerManager.COUNT_TIME.remove(event.getParkourPlayer().getPlayer().getUniqueId());
		}
		if (event.getParkourGame().isRunning() && !PlayerManager.getParkourSinglePlayer(event.getParkourPlayer().getPlayer()).isIgnoring()) {
			event.getParkourPlayer().getPlayer().getInventory().setItem(8, new ItemStack(Material.AIR));
			for (final Entry<PotionEffectType, Integer> entry : event.getParkourGame().getOptions().getEffects().entrySet()) {
				event.getParkourPlayer().getPlayer().removePotionEffect(entry.getKey());
			}
		}
		if (SchedulerManager.TELEPORT_COUNT.containsKey(event.getParkourPlayer().getPlayer().getUniqueId())) {
			if (SchedulerManager.TELEPORT_COUNT_2.get(event.getParkourPlayer().getPlayer().getUniqueId()).getLeastTime() > 0) {
				event.getParkourPlayer().getPlayer().sendMessage(plugin.msg("teleportationCanceled", false));
			}
			plugin.getServer().getScheduler().cancelTask(SchedulerManager.TELEPORT_COUNT.get(event.getParkourPlayer().getPlayer().getUniqueId()));
			SchedulerManager.TELEPORT_COUNT.remove(event.getParkourPlayer().getPlayer().getUniqueId());
			SchedulerManager.TELEPORT_COUNT_2.remove(event.getParkourPlayer().getPlayer().getUniqueId());
		}
		PlayerManager.remove(event.getParkourPlayer().getPlayer());
		for (final Entry<PotionEffectType, Integer> entry : event.getParkourGame().getOptions().getEffects().entrySet()) {
			event.getParkourPlayer().getPlayer().removePotionEffect(entry.getKey());
		}
	}

	@EventHandler(priority = EventPriority.LOW)
	public void join(final PlayerJoinEvent event) {
		for (final ParkourGame p : ParkourManager.getAllGames()) {
			for (final AbstractRegion r : p.getAllRegions()) {
				if (r.contains(event.getPlayer().getLocation()) && p.getWorld().equals(event.getPlayer().getWorld()) && !PlayerManager.getParkourSinglePlayer(event.getPlayer()).isIgnoring()) {
					p.addPlayer(event.getPlayer());
					return;
				}
			}
		}
	}

	@EventHandler
	public void tp(final PlayerTeleportEvent event) {
		for (final ParkourGame p : ParkourManager.getAllGames()) {
			if (p.inAnyRegion(event.getTo()) && !p.getPlayers().contains(PlayerManager.getParkourSinglePlayer(event.getPlayer())) && !PlayerManager.getParkourSinglePlayer(event.getPlayer()).isIgnoring()) {
				p.addPlayer(event.getPlayer());
				if (!SchedulerManager.COUNT_TIME.containsKey(event.getPlayer().getUniqueId()) && p.getOptions().isRecordsCounting()) {
					final int s = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, PlayerManager.getParkourSinglePlayer(event.getPlayer()), 1, 1);
					SchedulerManager.COUNT_TIME.put(event.getPlayer().getUniqueId(), s);
				}
				continue;
			}
			if (!p.inAnyRegion(event.getTo()) && p.getPlayers().contains(PlayerManager.getParkourPlayer(event.getPlayer()))) {
				p.removePlayer(event.getPlayer());
				SchedulerManager.COUNT_TIME.remove(event.getPlayer().getUniqueId());
			}
		}
	}

	@EventHandler
	public void dmg(final EntityDamageEvent event) {
		if (event.getEntity() instanceof Player) {
			final Player pl = (Player) event.getEntity();
			final ParkourGame parkour = ParkourManager.getParkour(pl);
			if (parkour != null && !parkour.getOptions().isDamageAllowed() && parkour.getWorld().equals(pl.getWorld())) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void destroy(final BlockBreakEvent event) {
		if (event.getPlayer().hasPermission("ats.parkour.modify")) {
			return;
		}
		ParkourManager.getAllGames().stream()
				.flatMap(p -> p.getAllRegions().stream())
				.filter(r -> r.contains(event.getBlock().getLocation()) && r.getCuboidRegion().getWorld().equals(new BukkitWorld(event.getPlayer().getWorld())))
				.findFirst().ifPresent(r -> event.setCancelled(true));
	}

	@EventHandler
	public void place(final BlockPlaceEvent event) {
		if (event.getPlayer().hasPermission("ats.parkour.modify")) {
			return;
		}
		ParkourManager.getAllGames().stream()
				.flatMap(p -> p.getAllRegions().stream())
				.filter(r -> r.contains(event.getBlock().getLocation()) && r.getCuboidRegion().getWorld().equals(new BukkitWorld(event.getPlayer().getWorld())))
				.findFirst().ifPresent(r -> event.setCancelled(true));
	}

	@EventHandler
	public void drop(final PlayerDropItemEvent event) {
		if (!PlayerManager.getParkourSinglePlayer(event.getPlayer()).isIgnoring()) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void leave(final PlayerQuitEvent event) {
		final ParkourGame parkour = ParkourManager.getParkour(event.getPlayer());
		if (parkour != null) {
			parkour.removePlayer(event.getPlayer());
		}
		PlayerManager.remove(event.getPlayer());
	}

	public void stopScheduling(final Player player) {
		if (SchedulerManager.TELEPORT_COUNT.containsKey(player.getUniqueId())) {
			plugin.getServer().getScheduler().cancelTask(SchedulerManager.TELEPORT_COUNT.get(player.getUniqueId()));
			PlayerManager.getParkourSinglePlayer(player).teleportToLobby();
		}
	}

	public void startScheduling(final Player player) {
		final TeleportCount t = new TeleportCount(plugin, PlayerManager.getParkourPlayer(player));
		final int s = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, t, 0, 20);
		SchedulerManager.TELEPORT_COUNT.put(player.getUniqueId(), s);
		SchedulerManager.TELEPORT_COUNT_2.put(player.getUniqueId(), t);
	}
}
