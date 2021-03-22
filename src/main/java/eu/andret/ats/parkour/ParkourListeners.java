/*
 * Copyright Andret (c) 2019-2021. Copying and modifying allowed only keeping git link reference.
 */
package eu.andret.ats.parkour;

import com.sk89q.worldedit.bukkit.BukkitWorld;
import eu.andret.ats.parkour.event.player.PlayerAchieveCheckpointEvent;
import eu.andret.ats.parkour.event.player.PlayerCompleteParkourEvent;
import eu.andret.ats.parkour.event.player.PlayerEnterRegionEvent;
import eu.andret.ats.parkour.event.player.PlayerEnterSpawnEvent;
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
import eu.andret.ats.parkour.region.DirectionalRegion;
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
import org.bukkit.plugin.PluginManager;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Collection;
import java.util.Map.Entry;
import java.util.Objects;

@Value
public class ParkourListeners implements Listener {
	ParkourPlugin plugin;

	@EventHandler(priority = EventPriority.HIGHEST)
	public synchronized void move(final PlayerMoveEvent event) {
		final Player player = event.getPlayer();
		final ParkourPlayer parkourPlayer = PlayerManager.getParkourSinglePlayer(event.getPlayer());
		if (event.getTo().getY() < -10) {
			parkourPlayer.teleportToLobby();
		}
		final ParkourGame parkour = ParkourManager.getParkour(player);
		if (parkour != null && !parkourPlayer.isIgnoring()) {
			if (player.getWorld().equals(parkour.getWorld()) && parkour.isRunning()) {
				player.setFoodLevel(20);
				player.setHealth(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
				for (final Entry<PotionEffectType, Integer> entry : parkour.getOptions().getEffects().entrySet()) {
					player.addPotionEffect(new PotionEffect(entry.getKey(), 99999999, entry.getValue()));
				}
				if (!parkourPlayer.isSpectating() && player.isFlying() && !parkour.getOptions().isBoat() && !parkour.getSpawn().contains(player.getLocation())) {
					parkourPlayer.teleportToCheckpoint(parkourPlayer.getLastVisitedCheckpointId());
				}
				if (!parkourPlayer.isSpectating() && parkour.getOptions().isSprintForced() && !player.isSprinting()) {
					boolean tp = true;
					for (final DirectionalRegion cr : parkour.getCheckpoints()) {
						if (cr.contains(player.getLocation())) {
							tp = false;
						}
					}

					if (tp) {
						if (parkour.getOptions().isAlwaysSpawn()) {
							parkourPlayer.teleportToSpawn();
						} else {
							parkourPlayer.teleportToCheckpoint(parkourPlayer.getLastVisitedCheckpointId());
						}
					}
				}
			}

			final PluginManager pluginManager = plugin.getServer().getPluginManager();
			for (final ParkourGame parkourGame : ParkourManager.getAllGames()) {
				if (!parkourPlayer.isSpectating() && parkourGame.isRunning() && !parkourPlayer.isIgnoring() && parkourGame.getWorld().equals(parkour.getWorld())) {
					for (final DirectionalRegion region : parkourGame.getCheckpoints()) {
						if (region.contains(event.getFrom()) && !region.contains(event.getTo())) {
							pluginManager.callEvent(new PlayerLeaveRegionEvent(parkourGame, player, region));
						} else if (!region.contains(event.getFrom()) && region.contains(event.getTo())) {
							pluginManager.callEvent(new PlayerEnterRegionEvent(parkourGame, player, region));
							pluginManager.callEvent(new PlayerAchieveCheckpointEvent(parkourGame, PlayerManager.getParkourPlayer(player), region));
						}
					}

					for (final AbstractRegion region : parkourGame.getWalls()) {
						if (region.contains(event.getFrom()) && !region.contains(event.getTo())) {
							pluginManager.callEvent(new PlayerLeaveRegionEvent(parkourGame, player, region));
						} else if (!region.contains(event.getFrom()) && region.contains(event.getTo())) {
							pluginManager.callEvent(new PlayerEnterRegionEvent(parkourGame, player, region));
							pluginManager.callEvent(new PlayerHitWallEvent(parkourGame, PlayerManager.getParkourPlayer(player), region));
						}
					}

					if (parkourGame.getSpawn().contains(event.getFrom()) && !parkourGame.getSpawn().contains(event.getTo())) {
						pluginManager.callEvent(new PlayerLeaveRegionEvent(parkourGame, player, parkourGame.getSpawn()));
					} else if (!parkourGame.getSpawn().contains(event.getFrom()) && parkourGame.getSpawn().contains(event.getTo())) {
						pluginManager.callEvent(new PlayerEnterRegionEvent(parkourGame, player, parkourGame.getSpawn()));
						pluginManager.callEvent(new PlayerEnterSpawnEvent(parkourGame, PlayerManager.getParkourPlayer(player)));
					}
				}
			}
		}

		if (!event.getPlayer().getGameMode().equals(GameMode.CREATIVE)) {
			boolean fly = false;
			for (final ParkourGame p : ParkourManager.getAllGames()) {
				if (PlayerManager.getParkourSinglePlayer(player).isSpectating()) {
					if (p.getAllRegions().stream().filter(Objects::nonNull).anyMatch(x -> x.contains(player.getLocation()))) {
						fly = true;
						for (final Entry<PotionEffectType, Integer> entry : p.getOptions().getEffects().entrySet()) {
							event.getPlayer().addPotionEffect(new PotionEffect(entry.getKey(), 99999999, entry.getValue()));
						}
						player.setFoodLevel(20);
						player.setHealth(20D);
					} else {
						for (final Entry<PotionEffectType, Integer> entry : p.getOptions().getEffects().entrySet()) {
							event.getPlayer().removePotionEffect(entry.getKey());
						}
					}
				}
			}
			player.setAllowFlight(fly);
		}
		for (final ParkourGame p : ParkourManager.getAllGames()) {
			if (!parkourPlayer.isSpectating() && player.getWorld().equals(p.getWorld()) && !parkourPlayer.isIgnoring()) {
				final boolean inAnyRegion = p.getAllRegions().stream()
						.filter(Objects::nonNull)
						.anyMatch(x -> x.contains(event.getPlayer().getLocation()));
				if (!inAnyRegion && p.getPlayers().contains(parkourPlayer)) {
					p.removePlayer(player);
				} else if (inAnyRegion && !p.getPlayers().contains(parkourPlayer)) {
					p.addPlayer(player);
				}
			}
		}
	}

	@EventHandler
	public void checkpoint(final PlayerAchieveCheckpointEvent event) {
		final ParkourGame parkourGame = event.getParkourGame();
		final ParkourPlayer parkourPlayer = PlayerManager.getParkourSinglePlayer(event.getParkourPlayer().getPlayer());
		if (event.getParkourPlayer().getPlayer().getWorld().equals(parkourGame.getWorld())) {
			final int checkpointId = event.getParkourGame().getCheckpoints().indexOf(event.getCheckpointRegion());
			if (checkpointId > parkourPlayer.getLastVisitedCheckpointId()) {
				parkourPlayer.setLastVisitedCheckpointId(checkpointId);
				if (checkpointId == event.getParkourGame().getCheckpoints().size() - 1) {
					if (plugin.getConfig().getBoolean("last-checkpoint-info")) {
						event.getParkourPlayer().getPlayer().sendMessage(plugin.msg("achieveCheckpoint", false));
					}
				} else {
					event.getParkourPlayer().getPlayer().sendMessage(plugin.msg("achieveCheckpoint", false));
				}
			}
			if (checkpointId == parkourGame.getLastCheckpointId()) {
				plugin.getServer().getPluginManager().callEvent(new PlayerCompleteParkourEvent(event.getParkourGame(), event.getParkourPlayer()));
			}
		}
	}

	@EventHandler
	public void enterSpawn(final PlayerEnterSpawnEvent event) {
		PlayerManager.getParkourSinglePlayer(event.getParkourPlayer().getPlayer()).reset();
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
		if (event.getCheckpointRegion() == event.getParkourGame().getSpawn()) {
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
			final ParkourGame parkourGame = ParkourManager.getParkour(player);
			if (parkourGame != null && PlayerManager.getParkourPlayer(player) != null && parkourGame.getOptions().isBoat() && event.getVehicle() instanceof Boat && parkourGame.isRunning() && player.getWorld().equals(parkourGame.getWorld())) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void wall(final PlayerHitWallEvent event) {
		final ParkourPlayer parkourPlayer = PlayerManager.getParkourSinglePlayer(event.getParkourPlayer().getPlayer());
		if (event.getParkourGame().getOptions().isAlwaysSpawn()) {
			parkourPlayer.teleportToSpawn();
		} else {
			parkourPlayer.teleportToCheckpoint(parkourPlayer.getLastVisitedCheckpointId());
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
		event.getParkourPlayer().getPlayer().sendMessage(plugin.msg("joinParkour", false).replace("%PARKOUR%", event.getParkourGame().getDisplayName().replace('&', '\u00A7') + color));
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
		final ParkourPlayer parkourPlayer = PlayerManager.getParkourSinglePlayer(event.getPlayer());
		for (final ParkourGame parkourGame : ParkourManager.getAllGames()) {
			if (!event.getAction().equals(Action.PHYSICAL) && parkourGame.getTeleportBlock() != null && event.getClickedBlock() != null && parkourGame.getTeleportBlock().equals(event.getClickedBlock().getLocation()) && !parkourPlayer.isIgnoring()) {
				if (!parkourGame.getOptions().isVipOnly() || plugin.getRankProvider().map(x -> x.isVip(event.getPlayer())).isPresent()) {
					parkourGame.addPlayer(event.getPlayer());
					parkourPlayer.teleportToSpawn();
					event.setCancelled(true);
					parkourPlayer.reset();
				} else {
					event.getPlayer().sendMessage("this parkour is o nly for vip");
				}
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
		for (final ParkourGame parkourGame : ParkourManager.getAllGames()) {
			for (final AbstractRegion abstractRegion : parkourGame.getAllRegions()) {
				if (abstractRegion.contains(event.getPlayer().getLocation())
						&& parkourGame.getWorld().equals(event.getPlayer().getWorld())
						&& !PlayerManager.getParkourSinglePlayer(event.getPlayer()).isIgnoring()) {
					parkourGame.addPlayer(event.getPlayer());
					return;
				}
			}
		}
	}

	@EventHandler
	public void tp(final PlayerTeleportEvent event) {
		for (final ParkourGame parkourGame : ParkourManager.getAllGames()) {
			final boolean inAnyRegion = parkourGame.getAllRegions().stream().filter(Objects::nonNull).anyMatch(x -> x.contains(event.getTo()));
			if (inAnyRegion && !parkourGame.getPlayers().contains(PlayerManager.getParkourSinglePlayer(event.getPlayer())) && !PlayerManager.getParkourSinglePlayer(event.getPlayer()).isIgnoring()) {
				parkourGame.addPlayer(event.getPlayer());
				if (!SchedulerManager.COUNT_TIME.containsKey(event.getPlayer().getUniqueId()) && parkourGame.getOptions().isRecordsCounting()) {
					final int s = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, PlayerManager.getParkourSinglePlayer(event.getPlayer()), 1, 1);
					SchedulerManager.COUNT_TIME.put(event.getPlayer().getUniqueId(), s);
				}
				continue;
			}

			if (!inAnyRegion && parkourGame.getPlayers().contains(PlayerManager.getParkourPlayer(event.getPlayer()))) {
				parkourGame.removePlayer(event.getPlayer());
				SchedulerManager.COUNT_TIME.remove(event.getPlayer().getUniqueId());
			}
		}
	}

	@EventHandler
	public void dmg(final EntityDamageEvent event) {
		if (event.getEntity() instanceof Player) {
			final Player player = (Player) event.getEntity();
			final ParkourGame parkourGame = ParkourManager.getParkour(player);
			if (parkourGame != null && !parkourGame.getOptions().isDamageAllowed() && parkourGame.getWorld().equals(player.getWorld())) {
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
				.map(ParkourGame::getAllRegions)
				.flatMap(Collection::stream)
				.filter(Objects::nonNull)
				.filter(r -> r.contains(event.getBlock().getLocation()) && new BukkitWorld(event.getPlayer().getWorld()).equals(r.getCuboidRegion().getWorld()))
				.findFirst().ifPresent(r -> event.setCancelled(true));
	}

	@EventHandler
	public void place(final BlockPlaceEvent event) {
		if (event.getPlayer().hasPermission("ats.parkour.modify")) {
			return;
		}
		ParkourManager.getAllGames().stream()
				.map(ParkourGame::getAllRegions)
				.flatMap(Collection::stream)
				.filter(Objects::nonNull)
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
		final ParkourGame parkourGame = ParkourManager.getParkour(event.getPlayer());
		if (parkourGame != null) {
			parkourGame.removePlayer(event.getPlayer());
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
