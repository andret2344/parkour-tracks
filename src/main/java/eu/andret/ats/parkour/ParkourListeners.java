/*
 * Copyright Andret (c) 2019-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import eu.andret.ats.parkour.event.game.GameStartEvent;
import eu.andret.ats.parkour.event.game.GameStopEvent;
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
import eu.andret.ats.parkour.parkour.ParkourRecord;
import eu.andret.ats.parkour.player.ParkourPlayer;
import eu.andret.ats.parkour.player.ParkourSinglePlayer;
import eu.andret.ats.parkour.player.PlayerManager;
import eu.andret.ats.parkour.region.BasicRegion;
import eu.andret.ats.parkour.region.DirectionalRegion;
import eu.andret.ats.parkour.tasks.counter.ParkourCountdown;
import eu.andret.ats.parkour.tasks.counter.TimeCounter;
import eu.andret.ats.parkour.tasks.database.FetchAndInsertDataTask;
import eu.andret.ats.parkour.util.Data;
import eu.andret.ats.parkour.util.M;
import lombok.Value;
import org.bukkit.ChatColor;
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
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.PluginManager;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Value
public class ParkourListeners implements Listener {
	ParkourPlugin plugin;

	@EventHandler
	public void flying(final PlayerMoveEvent event) {
		final Player player = event.getPlayer();
		if (!player.isFlying()) {
			return;
		}
		final ParkourGame parkour = plugin.getParkourManager().getParkour(player);
		if (parkour == null) {
			return;
		}
		if (!parkour.isRunning()) {
			return;
		}
		final ParkourPlayer parkourPlayer = PlayerManager.getParkourSinglePlayer(event.getPlayer());
		if (parkourPlayer.isIgnoring()) {
			return;
		}
		if (!player.getWorld().equals(parkour.getWorld())) {
			return;
		}
		plugin.getParkourManager().teleportToLobby(parkourPlayer);
		plugin.getServer().getPluginManager().callEvent(new PlayerQuitGameEvent(parkour, parkourPlayer));
		player.sendMessage(plugin.msg(M.Error.DEFAULT.forbiddenFlying));
	}

	@EventHandler
	public void moveInParkour(final PlayerMoveEvent event) {
		final Player player = event.getPlayer();
		final ParkourPlayer parkourPlayer = PlayerManager.getParkourSinglePlayer(event.getPlayer());
		final ParkourGame parkour = plugin.getParkourManager().getParkour(player);
		if (parkour == null || parkourPlayer.isIgnoring() || !parkour.isRunning()) {
			return;
		}
		if (!player.getWorld().equals(parkour.getWorld())) {
			return;
		}
		player.setFoodLevel(20);
		Optional.ofNullable(player.getAttribute(Attribute.GENERIC_MAX_HEALTH))
				.map(AttributeInstance::getValue)
				.ifPresent(player::setHealth);
		final int lastVisitedCheckpointId = parkourPlayer.getLastCheckpoint();
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

	@EventHandler(priority = EventPriority.HIGHEST)
	public void move(final PlayerMoveEvent event) {
		final Player player = event.getPlayer();
		final ParkourPlayer parkourPlayer = PlayerManager.getParkourSinglePlayer(event.getPlayer());
		final ParkourGame parkour = plugin.getParkourManager().getParkour(player);
		if (parkour == null || parkourPlayer.isIgnoring()) {
			return;
		}

		final PluginManager pluginManager = plugin.getServer().getPluginManager();
		plugin.getParkourManager().getAllGames()
				.stream()
				.filter(ParkourGame::isRunning)
				.filter(parkourGame -> Objects.equals(parkourGame.getWorld(), parkour.getWorld()))
				.forEach(parkourGame -> {
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
				});
	}

	@EventHandler
	public void joinLeaveMove(final PlayerMoveEvent event) {
		final Player player = event.getPlayer();
		final ParkourPlayer parkourPlayer = PlayerManager.getParkourSinglePlayer(player);
		if (parkourPlayer.isIgnoring()) {
			return;
		}
		plugin.getParkourManager().getAllGames()
				.stream()
				.filter(parkourGame -> player.getWorld().equals(parkourGame.getWorld()))
				.filter(ParkourGame::isRunning)
				.forEach(parkourGame -> {
					final PluginManager pluginManager = plugin.getServer().getPluginManager();
					final boolean inParkour = plugin.getParkourManager().inAnyRegion(parkourGame, player);
					if (!inParkour && parkourGame.getPlayers().contains(parkourPlayer) && parkourGame.removePlayer(parkourPlayer)) {
						parkourPlayer.reset();
						pluginManager.callEvent(new PlayerQuitGameEvent(parkourGame, parkourPlayer));
					} else if (inParkour && !parkourGame.getPlayers().contains(parkourPlayer) && parkourGame.addPlayer(player)) {
						parkourPlayer.reset();
						pluginManager.callEvent(new PlayerJoinGameEvent(parkourGame, parkourPlayer));
					}
				});
	}

	@EventHandler
	public void checkpoint(final PlayerAchieveCheckpointEvent event) {
		final ParkourGame parkourGame = event.getGame();
		final ParkourPlayer parkourPlayer = PlayerManager.getParkourSinglePlayer(event.getPlayer().getPlayer());
		if (!event.getPlayer().getPlayer().getWorld().equals(parkourGame.getWorld())) {
			return;
		}
		final int checkpointId = event.getGame().getCheckpoints().indexOf(event.getRegion());
		if (checkpointId > parkourPlayer.getLastCheckpoint()) {
			parkourPlayer.setLastCheckpoint(checkpointId);
			if (checkpointId == event.getGame().getCheckpoints().size() - 1) {
				if (plugin.getConfig().getBoolean("last-checkpoint-info")) {
					event.getPlayer().getPlayer().sendMessage(plugin.msg("achieveCheckpoint"));
				}
			} else {
				event.getPlayer().getPlayer().sendMessage(plugin.msg("achieveCheckpoint"));
			}
		}
		if (checkpointId == parkourGame.getCheckpoints().size() - 1) {
			plugin.getServer().getPluginManager().callEvent(new PlayerCompleteParkourEvent(event.getGame(), event.getPlayer()));
		}
	}

	@EventHandler
	public void enterSpawn(final PlayerEnterSpawnEvent event) {
		PlayerManager.getParkourSinglePlayer(event.getPlayer().getPlayer()).reset();
	}

	@EventHandler
	public void complete(final PlayerCompleteParkourEvent event) {
		final ParkourPlayer parkourPlayer = event.getPlayer();
		final Player player = parkourPlayer.getPlayer();
		final UUID uniqueId = player.getUniqueId();
		final ParkourGame parkourGame = event.getGame();
		player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5F, 0.5F);
		if (plugin.getTeleportCountdown().containsKey(uniqueId)) {
			return;
		}
		final ParkourCountdown task = new ParkourCountdown(5,
				() -> player.sendMessage(plugin.msg("teleportingTime").replace("%SECONDS%", "5")),
				i -> player.sendMessage(plugin.msg("counting").replace("%NUMBER%", String.valueOf(i))),
				() -> {
					plugin.getServer().getScheduler().cancelTask(plugin.getTeleportCountdown().get(uniqueId));
					plugin.getServer().getScheduler().cancelTask(plugin.getTimeCounter().get(uniqueId));
					plugin.getTeleportCountdown().remove(uniqueId);
					plugin.getTimeCounter().remove(uniqueId);
					plugin.getParkourManager().teleportToLobby(parkourPlayer);
					plugin.getServer().getPluginManager().callEvent(new PlayerQuitGameEvent(event.getGame(), event.getPlayer()));
				});
		final int schedulerId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, task, 10, 20);
		plugin.getTeleportCountdown().put(uniqueId, schedulerId);
		if (!parkourGame.getOptions().isSavingResults()) {
			return;
		}
		final double currentTime = parkourPlayer.getTime();
		final String time = String.valueOf(currentTime);
		player.sendMessage(plugin.msg("finishTime").replace("%TIME%", Math.abs(time.lastIndexOf('.') - time.length()) == 2 ? (time + "0") : time));
		if (player.hasPermission("ats.parkour.ignoreRecords")) {
			player.sendMessage("Your time hasn't been saved to database");
		} else {
			plugin.getConnection()
					.map(connection -> new FetchAndInsertDataTask(connection, parkourGame, player, currentTime, (previousCount, previousPlayerBest, previousParkourBest) -> {
						player.sendMessage(plugin.msg("howMany").replace("%COUNT%", String.valueOf(previousCount + 1)));
						if (previousParkourBest > currentTime) {
							player.sendMessage(plugin.msg("newParkourBestTime"));
							plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () ->
									plugin.updateSign(new ParkourRecord(player.getName(), parkourGame, currentTime)));
						}
						if (previousPlayerBest > currentTime) {
							player.sendMessage(plugin.msg("newPersonalBestTime"));
						}
					}))
					.ifPresent(fetchAndInsertDataTask -> plugin.getServer().getScheduler().runTaskAsynchronously(plugin, fetchAndInsertDataTask));
		}
	}

	@EventHandler
	public void back(final PlayerTeleportBackEvent event) {
		if (event.getRegion() == event.getGame().getSpawn()) {
			PlayerManager.getParkourSinglePlayer(event.getPlayer().getPlayer()).reset();
		}
		if (!event.getGame().getOptions().isBoat()) {
			return;
		}
		final Location location = event.getPlayer().getPlayer().getLocation();
		if (location.getWorld() == null) {
			return;
		}
		final Boat boat = (Boat) location.getWorld().spawnEntity(location, EntityType.BOAT);
		boat.addPassenger(event.getPlayer().getPlayer());
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
		final ParkourPlayer parkourPlayer = event.getPlayer();
		final ParkourGame parkourGame = event.getGame();
		final DirectionalRegion region;
		final int lastCheckpoint = parkourPlayer.getLastCheckpoint();
		if (parkourGame.getOptions().isAlwaysSpawn() || lastCheckpoint == -1) {
			region = parkourGame.getSpawn();
		} else {
			region = parkourGame.getCheckpoints().get(lastCheckpoint);
		}
		if (region == null) {
			return;
		}
		PlayerManager.teleportToRegion(event.getPlayer(), region);
		plugin.getServer().getPluginManager().callEvent(new PlayerTeleportBackEvent(parkourGame, parkourPlayer, region));
	}

	@EventHandler
	public void joinGame(final PlayerJoinGameEvent event) {
		final ParkourPlayer parkourPlayer = event.getPlayer();
		final Player joiningPlayer = parkourPlayer.getPlayer();
		if (event.getGame().getOptions().isSavingResults()) {
			final int schedulerId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new TimeCounter(parkourPlayer, event.getGame()), 1, 1);
			plugin.getTimeCounter().put(joiningPlayer.getUniqueId(), schedulerId);
		}
		if (event.getGame().getOptions().isModifyInventory()) {
			plugin.getExitItem().ifPresent(item -> joiningPlayer.getInventory().setItem(8, item));
		}
		event.getGame().getOptions().getEffects().entrySet().stream()
				.map(entry -> new PotionEffect(entry.getKey(), 99999999, entry.getValue()))
				.forEach(joiningPlayer::addPotionEffect);
		joiningPlayer.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.msg("joinParkour").replace("%PARKOUR%", event.getGame().getDisplayName())));
		plugin.getParkourManager().getAllGames().stream()
				.filter(game -> !game.equals(event.getGame()))
				.map(ParkourGame::getPlayers)
				.flatMap(Collection::stream)
				.map(ParkourPlayer::getPlayer)
				.forEach(player -> {
					player.hidePlayer(plugin, joiningPlayer);
					joiningPlayer.hidePlayer(plugin, player);
				});
	}

	@EventHandler
	public void parkourTeleportBlockClick(final PlayerInteractEvent event) {
		if (event.getAction().equals(Action.PHYSICAL)) {
			return;
		}
		if (event.getClickedBlock() == null) {
			return;
		}
		final ParkourPlayer parkourPlayer = PlayerManager.getParkourSinglePlayer(event.getPlayer());
		if (parkourPlayer.isIgnoring()) {
			return;
		}
		plugin.getParkourManager().getAllGames().stream()
				.filter(parkourGame -> parkourGame.getTeleportBlock() != null)
				.filter(parkourGame -> parkourGame.getTeleportBlock().equals(event.getClickedBlock().getLocation()))
				.findAny()
				.ifPresent(parkourGame -> {
					final boolean isVip = plugin.getRankProvider().map(x -> x.isVip(event.getPlayer())).isPresent();
					if (!parkourGame.getOptions().isVipOnly() || isVip) {
						parkourGame.addPlayer(event.getPlayer());
						final DirectionalRegion parkourSpawn = parkourGame.getSpawn();
						if (parkourSpawn == null) {
							return;
						}
						PlayerManager.teleportToRegion(parkourPlayer, parkourSpawn);
						plugin.getServer().getPluginManager().callEvent(new PlayerTeleportBackEvent(parkourGame, parkourPlayer, parkourSpawn));
						event.setCancelled(true);
						parkourPlayer.reset();
					} else {
						event.getPlayer().sendMessage(plugin.msg(M.Error.DEFAULT.notVip));
					}
				});
	}

	@EventHandler
	public void clickInsideGame(final PlayerInteractEvent event) {
		if (!plugin.getParkourManager().getPlayersInGames().contains(event.getPlayer())) {
			return;
		}
		if (event.getClickedBlock() != null && Data.getInteractiveMaterials().contains(event.getClickedBlock().getType())) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void clickDoors(final PlayerInteractEvent event) {
		final ParkourPlayer parkourPlayer = PlayerManager.getParkourSinglePlayer(event.getPlayer());
		Optional.of(event)
				.map(PlayerInteractEvent::getItem)
				.map(ItemStack::getItemMeta)
				.map(ItemMeta::getDisplayName)
				.flatMap(displayName -> plugin.getExitItem()
						.map(ItemStack::getItemMeta)
						.map(ItemMeta::getDisplayName)
						.map(displayName::equals))
				.filter(Boolean.TRUE::equals)
				.ifPresent(result -> {
					event.setCancelled(true);
					if (plugin.getParkourManager().getLobbyLocation() != null) {
						parkourPlayer.getPlayer().teleport(plugin.getParkourManager().getLobbyLocation());
					}
				});
	}

	@EventHandler
	public void quitGame(final PlayerQuitGameEvent event) {
		final ParkourPlayer parkourPlayer = event.getPlayer();
		final Player player = parkourPlayer.getPlayer();
		final UUID uniqueId = player.getUniqueId();
		parkourPlayer.reset();
		if (plugin.getTimeCounter().containsKey(uniqueId)) {
			plugin.getServer().getScheduler().cancelTask(plugin.getTimeCounter().get(uniqueId));
			plugin.getTimeCounter().remove(uniqueId);
		}
		if (event.getGame().isRunning() && !parkourPlayer.isIgnoring()) {
			player.getInventory().setItem(8, new ItemStack(Material.AIR));
		}
		if (plugin.getTeleportCountdown().containsKey(uniqueId)) {
			player.sendMessage(plugin.msg("teleportationCanceled"));
			plugin.getServer().getScheduler().cancelTask(plugin.getTeleportCountdown().get(uniqueId));
			plugin.getTeleportCountdown().remove(uniqueId);
		}
		event.getGame().getOptions().getEffects().keySet().forEach(player::removePotionEffect);
		PlayerManager.remove(player);
	}

	@EventHandler
	public void gameStop(final GameStopEvent event) {
		final ParkourGame parkourGame = event.getGame();
		final ParkourManager parkourManager = plugin.getParkourManager();
		new ArrayList<>(parkourGame.getPlayers()).forEach(parkourPlayer -> {
			plugin.getServer().getPluginManager().callEvent(new PlayerQuitGameEvent(parkourGame, parkourPlayer));
			parkourManager.teleportToLobby(parkourPlayer);
			parkourGame.getPlayers().remove(parkourPlayer);
		});
		parkourManager.sortGames();
	}

	@EventHandler
	public void gameStart(final GameStartEvent event) {
		final ParkourManager parkourManager = plugin.getParkourManager();
		event.getGame().getWorld().getPlayers().stream()
				.filter(player -> parkourManager.inAnyRegion(event.getGame(), player))
				.forEach(parkourManager::teleportToLobby);
		parkourManager.sortGames();
	}

	@EventHandler(priority = EventPriority.LOW)
	public void join(final PlayerJoinEvent event) {
		final Player player = event.getPlayer();
		plugin.getParkourManager().getAllGames().stream()
				.filter(parkourGame -> plugin.getParkourManager().inAnyRegion(parkourGame, player))
				.filter(parkourGame -> parkourGame.getWorld().equals(player.getWorld()))
				.findAny()
				.ifPresent(ignored -> {
					player.setLevel(0);
					player.setExp(0);
					plugin.getParkourManager().teleportToLobby(player);
				});
	}

	@EventHandler
	public void tp(final PlayerTeleportEvent event) {
		final ParkourSinglePlayer parkourPlayer = PlayerManager.getParkourSinglePlayer(event.getPlayer());
		if (parkourPlayer.isIgnoring()) {
			return;
		}
		for (final ParkourGame parkourGame : plugin.getParkourManager().getAllGames()) {
			final boolean inParkour = plugin.getParkourManager().inAnyRegion(parkourGame, event.getTo());
			if (inParkour && !parkourGame.getPlayers().contains(parkourPlayer)) {
				parkourGame.addPlayer(event.getPlayer());
				plugin.getServer().getPluginManager().callEvent(new PlayerJoinGameEvent(parkourGame, parkourPlayer));
			}

			if (!inParkour && parkourGame.getPlayers().contains(PlayerManager.getParkourPlayer(event.getPlayer()))) {
				parkourGame.removePlayer(parkourPlayer);
				plugin.getServer().getPluginManager().callEvent(new PlayerQuitGameEvent(parkourGame, parkourPlayer));
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
				.filter(r -> r.contains(event.getBlock().getLocation()) && BukkitAdapter.adapt(player.getWorld()).equals(r.getRegion().getWorld()))
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

	@EventHandler
	public void breakSpecialBlock(final BlockBreakEvent event) {
		final Location brokenBlockLocation = event.getBlock().getLocation();
		plugin.getParkourManager().getAllGames().forEach(parkourGame -> {
			if (parkourGame.getTeleportBlock() != null && parkourGame.getTeleportBlock().getBlock().getLocation().equals(brokenBlockLocation)) {
				parkourGame.setTeleportBlock(null);
				event.getPlayer().sendMessage("Destroyed teleport block!");
			}
			if (parkourGame.getRecordsBlock() != null && parkourGame.getRecordsBlock().getBlock().getLocation().equals(brokenBlockLocation)) {
				parkourGame.setRecordsBlock(null);
				event.getPlayer().sendMessage("Destroyed records block!");
			}
		});
	}
}
