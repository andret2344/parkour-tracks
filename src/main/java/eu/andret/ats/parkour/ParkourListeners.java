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
import eu.andret.ats.parkour.parkour.ParkourRecord;
import eu.andret.ats.parkour.player.ParkourPlayer;
import eu.andret.ats.parkour.player.ParkourSinglePlayer;
import eu.andret.ats.parkour.player.PlayerManager;
import eu.andret.ats.parkour.region.BasicRegion;
import eu.andret.ats.parkour.region.DirectionalRegion;
import eu.andret.ats.parkour.tasks.counter.ParkourCountdown;
import eu.andret.ats.parkour.tasks.counter.PlayerTimeCounter;
import eu.andret.ats.parkour.tasks.database.DataBaseOperations;
import eu.andret.ats.parkour.util.Data;
import lombok.Value;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Boat;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockEvent;
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
import java.util.Optional;
import java.util.UUID;

@Value
public class ParkourListeners implements Listener {
	ParkourPlugin plugin;

	@EventHandler(priority = EventPriority.HIGHEST)
	public synchronized void move(final PlayerMoveEvent event) {
		final Player player = event.getPlayer();
		final ParkourPlayer parkourPlayer = PlayerManager.getParkourSinglePlayer(event.getPlayer());
		if (event.getTo() != null && event.getTo().getY() < -10 && plugin.getParkourManager().getLobbyLocation() != null) {
			player.teleport(plugin.getParkourManager().getLobbyLocation());
		}
		final ParkourGame parkour = plugin.getParkourManager().getParkour(player);
		if (parkour != null && !parkourPlayer.isIgnoring()) {
			if (player.getWorld().equals(parkour.getWorld()) && parkour.isRunning()) {
				player.setFoodLevel(20);
				Optional.ofNullable(player.getAttribute(Attribute.GENERIC_MAX_HEALTH))
						.map(AttributeInstance::getValue)
						.ifPresent(player::setHealth);
				for (final Entry<PotionEffectType, Integer> entry : parkour.getOptions().getEffects().entrySet()) {
					player.addPotionEffect(new PotionEffect(entry.getKey(), 99999999, entry.getValue()));
				}
				final int lastVisitedCheckpointId = parkourPlayer.getLastVisitedCheckpointId();
				if (!parkourPlayer.isIgnoring() && player.isFlying() && !parkour.getOptions().isBoat() && !parkour.getSpawn().contains(player.getLocation())) {
					final DirectionalRegion region = lastVisitedCheckpointId == -1 ? parkour.getSpawn() : parkour.getCheckpoints().get(lastVisitedCheckpointId);
					PlayerManager.teleportToRegion(parkourPlayer, region);
					plugin.getServer().getPluginManager().callEvent(new PlayerTeleportBackEvent(parkour, parkourPlayer, region));
				}
				if (!parkourPlayer.isIgnoring() && parkour.getOptions().isSprintForced() && !player.isSprinting()) {
					parkour.getCheckpoints().stream()
							.filter(x -> x.contains(player.getLocation()))
							.findAny()
							.ifPresent(x -> {
								final DirectionalRegion region;
								if (parkour.getOptions().isAlwaysSpawn()) {
									region = parkour.getSpawn();
								} else {
									region = parkour.getCheckpoints().get(lastVisitedCheckpointId);
								}
								if (region == null) {
									return;
								}
								PlayerManager.teleportToRegion(parkourPlayer, region);
								plugin.getServer().getPluginManager().callEvent(new PlayerTeleportBackEvent(parkour, parkourPlayer, region));
							});
				}
			}

			final PluginManager pluginManager = plugin.getServer().getPluginManager();
			for (final ParkourGame parkourGame : plugin.getParkourManager().getAllGames()) {
				if (parkourGame.isRunning() && !parkourPlayer.isIgnoring() && parkourGame.getWorld().equals(parkour.getWorld())) {
					for (final DirectionalRegion region : parkourGame.getCheckpoints()) {
						if (region.contains(event.getFrom()) && !region.contains(event.getTo())) {
							pluginManager.callEvent(new PlayerLeaveRegionEvent(parkourGame, player, region));
						} else if (!region.contains(event.getFrom()) && region.contains(event.getTo())) {
							pluginManager.callEvent(new PlayerEnterRegionEvent(parkourGame, player, region));
							pluginManager.callEvent(new PlayerAchieveCheckpointEvent(parkourGame, PlayerManager.getParkourPlayer(player), region));
						}
					}

					for (final BasicRegion region : parkourGame.getWalls()) {
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
			for (final ParkourGame parkourGame : plugin.getParkourManager().getAllGames()) {
				if (PlayerManager.getParkourSinglePlayer(player).isIgnoring()) {
					if (parkourGame.getAllRegions().stream().filter(Objects::nonNull).anyMatch(x -> x.contains(player.getLocation()))) {
						fly = true;
						for (final Entry<PotionEffectType, Integer> entry : parkourGame.getOptions().getEffects().entrySet()) {
							event.getPlayer().addPotionEffect(new PotionEffect(entry.getKey(), 99999999, entry.getValue()));
						}
						player.setFoodLevel(20);
						player.setHealth(20D);
					} else {
						for (final Entry<PotionEffectType, Integer> entry : parkourGame.getOptions().getEffects().entrySet()) {
							event.getPlayer().removePotionEffect(entry.getKey());
						}
					}
				}
			}
			player.setAllowFlight(fly);
		}
		plugin.getParkourManager().getAllGames()
				.stream()
				.filter(parkourGame -> parkourGame.isRunning() && !parkourPlayer.isIgnoring() && player.getWorld().equals(parkourGame.getWorld()))
				.forEach(parkourGame -> {
					final boolean inAnyRegion = parkourGame.getAllRegions().stream()
							.filter(Objects::nonNull)
							.anyMatch(x -> x.contains(event.getPlayer().getLocation()));
					if (!inAnyRegion && parkourGame.getPlayers().contains(parkourPlayer)) {
						parkourGame.removePlayer(parkourPlayer);
					} else if (inAnyRegion && !parkourGame.getPlayers().contains(parkourPlayer)) {
						parkourGame.addPlayer(player);
					}
				});
	}

	@EventHandler
	public void checkpoint(final PlayerAchieveCheckpointEvent event) {
		final ParkourGame parkourGame = event.getParkourGame();
		final ParkourPlayer parkourPlayer = PlayerManager.getParkourSinglePlayer(event.getParkourPlayer().getPlayer());
		if (!event.getParkourPlayer().getPlayer().getWorld().equals(parkourGame.getWorld())) {
			return;
		}
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
		if (checkpointId == parkourGame.getCheckpoints().size() - 1) {
			plugin.getServer().getPluginManager().callEvent(new PlayerCompleteParkourEvent(event.getParkourGame(), event.getParkourPlayer()));
		}
	}

	@EventHandler
	public void enterSpawn(final PlayerEnterSpawnEvent event) {
		PlayerManager.getParkourSinglePlayer(event.getParkourPlayer().getPlayer()).reset();
	}

	@EventHandler
	public void complete(final PlayerCompleteParkourEvent event) {
		final Player player = event.getParkourPlayer().getPlayer();
		final UUID uniqueId = player.getUniqueId();
		final ParkourGame parkourGame = event.getParkourGame();
		player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5F, 0.5F);
		if (plugin.getTeleportCount().containsKey(uniqueId)) {
			return;
		}
		final ParkourCountdown task = new ParkourCountdown(5,
				() -> player.sendMessage(plugin.msg("teleportingTime", false).replace("%SECONDS%", "5")),
				i -> player.sendMessage(plugin.msg("counting", false).replace("%NUMBER%", String.valueOf(i))),
				() -> {
					plugin.getServer().getScheduler().cancelTask(plugin.getTeleportCount().get(uniqueId));
					plugin.getTeleportCount().remove(uniqueId);
					player.teleport(plugin.getParkourManager().getLobbyLocation());
				});
		final int schedulerId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, task, 10, 20);
		plugin.getTeleportCount().put(uniqueId, schedulerId);
		if (!parkourGame.getOptions().isRecordsCounting()) {
			return;
		}
		// FIXME: FInd better way
		final float currentTime = player.getLevel() + player.getExp();
		final String time = String.valueOf(currentTime);
		player.sendMessage(plugin.msg("finishTime", false).replace("%TIME%", Math.abs(time.lastIndexOf('.') - time.length()) == 2 ? (time + "0") : time));
		if (player.hasPermission("ats.parkour.ignoreRecords")) {
			player.sendMessage("Your time hasn't been saved to database");
		} else {
			plugin.getConnection()
					.map(connection -> new DataBaseOperations(connection, parkourGame, player, currentTime, (previousCount, previousPlayerBest, previousParkourBest) -> {
						player.sendMessage(plugin.msg("howMany", false).replace("%COUNT%", String.valueOf(previousCount + 1)));
						if (previousParkourBest > currentTime) {
							player.sendMessage(plugin.msg("newParkourBestTime", false));
							plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () ->
									plugin.updateSign(new ParkourRecord(player.getName(), parkourGame, currentTime)));
						}
						if (previousPlayerBest > currentTime) {
							player.sendMessage(plugin.msg("newPersonalBestTime", false));
						}
					}))
					.ifPresent(dataBaseOperations -> plugin.getServer().getScheduler().runTaskAsynchronously(plugin, dataBaseOperations));
		}
	}

	@EventHandler
	public void back(final PlayerTeleportBackEvent event) {
		if (event.getCheckpointRegion() == event.getParkourGame().getSpawn()) {
			PlayerManager.getParkourSinglePlayer(event.getParkourPlayer().getPlayer()).reset();
		}
		if (!event.getParkourGame().getOptions().isBoat()) {
			return;
		}
		final Location location = event.getParkourPlayer().getPlayer().getLocation();
		if (location.getWorld() == null) {
			return;
		}
		final Boat boat = (Boat) location.getWorld().spawnEntity(location, EntityType.BOAT);
		boat.addPassenger(event.getParkourPlayer().getPlayer());
	}

	@EventHandler
	public void leaveBoat(final VehicleExitEvent event) {
		if (!(event.getExited() instanceof Player)) {
			return;
		}
		final Player player = (Player) event.getExited();
		final ParkourGame parkourGame = plugin.getParkourManager().getParkour(player);
		if (parkourGame == null) {
			return;
		}
		if (PlayerManager.getParkourPlayer(player) == null) {
			return;
		}
		if (!parkourGame.getOptions().isBoat()) {
			return;
		}
		if (parkourGame.isRunning()) {
			return;
		}
		if (!(event.getVehicle() instanceof Boat) || !player.getWorld().equals(parkourGame.getWorld())) {
			return;
		}
		event.setCancelled(true);
	}

	@EventHandler
	public void wall(final PlayerHitWallEvent event) {
		final ParkourPlayer parkourPlayer = event.getParkourPlayer();
		final ParkourGame parkourGame = event.getParkourGame();
		final DirectionalRegion region;
		final int lastVisitedCheckpointId = parkourPlayer.getLastVisitedCheckpointId();
		if (parkourGame.getOptions().isAlwaysSpawn() || lastVisitedCheckpointId == -1) {
			region = parkourGame.getSpawn();
		} else {
			region = parkourGame.getCheckpoints().get(lastVisitedCheckpointId);
		}
		if (region == null) {
			return;
		}
		PlayerManager.teleportToRegion(event.getParkourPlayer(), region);
		plugin.getServer().getPluginManager().callEvent(new PlayerTeleportBackEvent(parkourGame, parkourPlayer, region));
	}

	@EventHandler
	public synchronized void joinGame(final PlayerJoinGameEvent event) {
		final ParkourPlayer parkourPlayer = event.getParkourPlayer();
		if (event.getParkourGame().getOptions().isRecordsCounting() && !parkourPlayer.getPlayer().getGameMode().equals(GameMode.CREATIVE)) {
			parkourPlayer.getPlayer().setLevel(0);
			parkourPlayer.getPlayer().setExp(0);
			if (!plugin.getPlayerTimeCounters().containsKey(parkourPlayer.getPlayer().getUniqueId()) && !parkourPlayer.isIgnoring()) {
				final int schedulerId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new PlayerTimeCounter(plugin, parkourPlayer, event.getParkourGame()), 1, 1);
				plugin.getPlayerTimeCounters().put(parkourPlayer.getPlayer().getUniqueId(), schedulerId);
			}
		}
		if (event.getParkourGame().isRunning() && !PlayerManager.getParkourSinglePlayer(parkourPlayer.getPlayer()).isIgnoring() && event.getParkourGame().getOptions().isModifyInventory()) {
			parkourPlayer.getPlayer().getInventory().setItem(8, plugin.getExit());
		}
		final String tmp = plugin.msg("joinParkour", false).split("%")[0];
		final String color = "\u00A7" + tmp.charAt(tmp.lastIndexOf('\u00A7') + 1);
		parkourPlayer.getPlayer().sendMessage(plugin.msg("joinParkour", false).replace("%PARKOUR%", event.getParkourGame().getDisplayName().replace('&', '\u00A7') + color));
		for (final ParkourGame p : plugin.getParkourManager().getAllGames()) {
			if (!p.equals(event.getParkourGame())) {
				for (final ParkourPlayer pl : p.getPlayers()) {
					pl.getPlayer().hidePlayer(plugin, parkourPlayer.getPlayer());
					parkourPlayer.getPlayer().hidePlayer(plugin, pl.getPlayer());
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void interact(final PlayerInteractEvent event) {
		final ParkourPlayer parkourPlayer = PlayerManager.getParkourSinglePlayer(event.getPlayer());
		for (final ParkourGame parkourGame : plugin.getParkourManager().getAllGames()) {
			if (!event.getAction().equals(Action.PHYSICAL) && parkourGame.getTeleportBlock() != null && event.getClickedBlock() != null && parkourGame.getTeleportBlock().equals(event.getClickedBlock().getLocation()) && !parkourPlayer.isIgnoring()) {
				final boolean isVip = plugin.getRankProvider().map(x -> x.isVip(event.getPlayer())).isPresent();
				if (!parkourGame.getOptions().isVipOnly() || isVip) {
					parkourGame.addPlayer(event.getPlayer());
					final DirectionalRegion region = parkourGame.getSpawn();
					if (region == null) {
						return;
					}
					PlayerManager.teleportToRegion(parkourPlayer, region);
					plugin.getServer().getPluginManager().callEvent(new PlayerTeleportBackEvent(parkourGame, parkourPlayer, region));
					event.setCancelled(true);
					parkourPlayer.reset();
				} else {
					event.getPlayer().sendMessage("this parkour is o only for vip");
				}
				break;
			}
		}
		if (!plugin.getParkourManager().getPlayersInGames().contains(event.getPlayer())) {
			return;
		}
		if (event.getClickedBlock() != null && (Data.getInteractiveMaterials().contains(event.getClickedBlock().getType()))) {
			event.setCancelled(true);
		}

		if (event.getItem() == null) {
			return;
		}

		if (!plugin.getExit().getItemMeta().getDisplayName().equals(event.getItem().getItemMeta().getDisplayName())) {
			return;
		}
		event.setCancelled(true);
		if (plugin.getParkourManager().getLobbyLocation() != null) {
			parkourPlayer.getPlayer().teleport(plugin.getParkourManager().getLobbyLocation());
		}
	}

	@EventHandler
	public void quitGame(final PlayerQuitGameEvent event) {
		final Player player = event.getParkourPlayer().getPlayer();
		if (plugin.getPlayerTimeCounters().containsKey(player.getUniqueId())) {
			plugin.getServer().getScheduler().cancelTask(plugin.getPlayerTimeCounters().get(player.getUniqueId()));
			PlayerManager.getParkourSinglePlayer(player).reset();
			plugin.getPlayerTimeCounters().remove(player.getUniqueId());
		}
		if (event.getParkourGame().isRunning() && !PlayerManager.getParkourSinglePlayer(player).isIgnoring()) {
			player.getInventory().setItem(8, new ItemStack(Material.AIR));
		}
		if (plugin.getTeleportCount().containsKey(player.getUniqueId())) {
			player.sendMessage(plugin.msg("teleportationCanceled", false));
			plugin.getServer().getScheduler().cancelTask(plugin.getTeleportCount().get(player.getUniqueId()));
			plugin.getTeleportCount().remove(player.getUniqueId());
		}
		event.getParkourGame().getOptions().getEffects().keySet().forEach(player::removePotionEffect);
		PlayerManager.remove(player);
	}

	@EventHandler(priority = EventPriority.LOW)
	public void join(final PlayerJoinEvent event) {
		for (final ParkourGame parkourGame : plugin.getParkourManager().getAllGames()) {
			for (final BasicRegion basicRegion : parkourGame.getAllRegions()) {
				if (basicRegion.contains(event.getPlayer().getLocation())
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
		for (final ParkourGame parkourGame : plugin.getParkourManager().getAllGames()) {
			final boolean inAnyRegion = parkourGame.getAllRegions().stream().filter(Objects::nonNull).anyMatch(x -> x.contains(event.getTo()));
			final ParkourSinglePlayer parkourPlayer = PlayerManager.getParkourSinglePlayer(event.getPlayer());
			if (inAnyRegion && !parkourGame.getPlayers().contains(parkourPlayer) && !parkourPlayer.isIgnoring()) {
				parkourGame.addPlayer(event.getPlayer());
				if (!plugin.getPlayerTimeCounters().containsKey(event.getPlayer().getUniqueId()) && parkourGame.getOptions().isRecordsCounting()) {
					final int schedulerId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new PlayerTimeCounter(plugin, parkourPlayer, parkourGame), 1, 1);
					plugin.getPlayerTimeCounters().put(event.getPlayer().getUniqueId(), schedulerId);
				}
				continue;
			}

			if (!inAnyRegion && parkourGame.getPlayers().contains(PlayerManager.getParkourPlayer(event.getPlayer()))) {
				parkourGame.removePlayer(parkourPlayer);
				plugin.getPlayerTimeCounters().remove(event.getPlayer().getUniqueId());
			}
		}
	}

	@EventHandler
	public void dmg(final EntityDamageEvent event) {
		if (!(event.getEntity() instanceof Player)) {
			return;
		}
		final Player player = (Player) event.getEntity();
		final ParkourGame parkourGame = plugin.getParkourManager().getParkour(player);
		if (parkourGame != null && !parkourGame.getOptions().isDamageAllowed() && parkourGame.getWorld().equals(player.getWorld())) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void destroy(final BlockBreakEvent event) {
		if (event.getPlayer().hasPermission("ats.parkour.modify")) {
			return;
		}
		universalBlockEventHandler(event, event.getPlayer());
	}

	@EventHandler
	public void place(final BlockPlaceEvent event) {
		if (event.getPlayer().hasPermission("ats.parkour.modify")) {
			return;
		}
		universalBlockEventHandler(event, event.getPlayer());
	}

	private void universalBlockEventHandler(final BlockEvent event, final Player player) {
		plugin.getParkourManager().getAllGames().stream()
				.map(ParkourGame::getAllRegions)
				.flatMap(Collection::stream)
				.filter(Objects::nonNull)
				.filter(r -> r.contains(event.getBlock().getLocation()) && new BukkitWorld(player.getWorld()).equals(r.getCuboidRegion().getWorld()))
				.findFirst()
				.ifPresent(r -> ((Cancellable) event).setCancelled(true));
	}

	@EventHandler
	public void drop(final PlayerDropItemEvent event) {
		if (!PlayerManager.getParkourSinglePlayer(event.getPlayer()).isIgnoring()) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void leave(final PlayerQuitEvent event) {
		final ParkourGame parkourGame = plugin.getParkourManager().getParkour(event.getPlayer());
		final ParkourPlayer parkourPlayer = PlayerManager.getParkourPlayer(event.getPlayer());
		if (parkourGame != null) {
			parkourGame.removePlayer(parkourPlayer);
		}
		PlayerManager.remove(event.getPlayer());
	}
}
