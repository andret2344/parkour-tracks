/*
 * Copyright Andret (c) 2019-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour;

import eu.andret.ats.parkour.api.FinancialProvider;
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
import eu.andret.ats.parkour.region.DirectionalRegion;
import eu.andret.ats.parkour.tasks.counter.ParkourCountdown;
import eu.andret.ats.parkour.tasks.counter.TimeCounter;
import eu.andret.ats.parkour.tasks.database.FetchAndInsertDataTask;
import eu.andret.ats.parkour.util.Constants;
import eu.andret.ats.parkour.util.Data;
import eu.andret.ats.parkour.util.EventSound;
import eu.andret.ats.parkour.util.M;
import lombok.Value;
import org.bukkit.Location;
import org.bukkit.Material;
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
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Value
public class ParkourListeners implements Listener {
	@NotNull
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
		final ParkourPlayer parkourPlayer = plugin.getPlayerManager().getParkourPlayer(player);
		if (parkourPlayer.isIgnoring()) {
			return;
		}
		if (!player.getWorld().equals(parkour.getWorld())) {
			return;
		}
		plugin.getParkourManager().teleportToLobby(parkourPlayer);
		player.sendMessage(plugin.msg(M.Error.DEFAULT.forbiddenFlying));
	}

	@EventHandler
	public void moveInParkour(final PlayerMoveEvent event) {
		final Player player = event.getPlayer();
		final ParkourPlayer parkourPlayer = plugin.getPlayerManager().getParkourPlayer(event.getPlayer());
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
		player.setExhaustion(0);
		player.setSaturation(20);
		final int lastVisitedCheckpointId = parkourPlayer.getLastCheckpoint();
		if (!parkour.getOptions().isSprintForced() || player.isSprinting()) {
			return;
		}
		parkour.getCheckpoints().stream()
				.filter(region -> region.contains(player.getLocation()))
				.findAny()
				.ifPresent(directionalRegion -> {
					final DirectionalRegion region = parkour.getOptions().isAlwaysSpawn() ? parkour.getSpawn() : parkour.getCheckpoints().get(lastVisitedCheckpointId);
					if (region == null) {
						return;
					}
					plugin.getPlayerManager().teleportToRegion(parkourPlayer, region);
					plugin.getServer().getPluginManager().callEvent(new PlayerTeleportBackEvent(parkour, parkourPlayer, region));
				});
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void move(final PlayerMoveEvent event) {
		final Player player = event.getPlayer();
		final ParkourPlayer parkourPlayer = plugin.getPlayerManager().getParkourPlayer(event.getPlayer());
		final ParkourGame parkour = plugin.getParkourManager().getParkour(player);
		if (parkour == null || parkourPlayer.isIgnoring()) {
			return;
		}

		plugin.getParkourManager().getAllGames()
				.stream()
				.filter(ParkourGame::isRunning)
				.filter(parkourGame -> Objects.equals(parkourGame.getWorld(), parkour.getWorld()))
				.forEach(parkourGame -> iterateOverRegions(event.getFrom(), event.getTo(), parkourPlayer, parkourGame));
	}

	@EventHandler
	public void joinLeaveMove(final PlayerMoveEvent event) {
		final Player player = event.getPlayer();
		final ParkourPlayer parkourPlayer = plugin.getPlayerManager().getParkourPlayer(player);
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
					} else if (inParkour && !parkourGame.getPlayers().contains(parkourPlayer) && parkourGame.addPlayer(parkourPlayer)) {
						parkourPlayer.reset();
						pluginManager.callEvent(new PlayerJoinGameEvent(parkourGame, parkourPlayer));
					}
				});
	}

	@EventHandler
	public void checkpoint(final PlayerAchieveCheckpointEvent event) {
		final ParkourGame parkourGame = event.getGame();
		final ParkourPlayer parkourPlayer = event.getPlayer();
		if (!parkourPlayer.getPlayer().getWorld().equals(parkourGame.getWorld())) {
			return;
		}
		if (parkourPlayer.getLastCheckpoint() == parkourGame.getCheckpoints().size() - 1) {
			return;
		}
		final int checkpointId = parkourGame.getCheckpoints().indexOf(event.getRegion());
		if (checkpointId > parkourPlayer.getLastCheckpoint()) {
			parkourPlayer.setLastCheckpoint(checkpointId);
			if (checkpointId != parkourGame.getCheckpoints().size() - 1) {
				parkourPlayer.getPlayer().sendMessage(plugin.msg("checkpoint-achieved"));
				plugin.getSound(EventSound.CHECKPOINT)
						.ifPresent(sound -> parkourPlayer.getPlayer().playSound(parkourPlayer.getPlayer().getLocation(), sound, 0.5F, 0.5F));
			}
		}
		if (checkpointId == parkourGame.getCheckpoints().size() - 1) {
			plugin.getServer().getPluginManager().callEvent(new PlayerCompleteParkourEvent(parkourGame, parkourPlayer));
		}
	}

	@EventHandler
	public void enterSpawn(final PlayerEnterSpawnEvent event) {
		event.getPlayer().reset();
	}

	@EventHandler
	public void complete(final PlayerCompleteParkourEvent event) {
		final ParkourPlayer parkourPlayer = event.getPlayer();
		final Player player = parkourPlayer.getPlayer();
		final UUID uniqueId = player.getUniqueId();
		final ParkourGame parkourGame = event.getGame();
		if (plugin.getTeleportCountdown().containsKey(uniqueId)) {
			return;
		}
		plugin.getSound(EventSound.COMPLETE)
				.ifPresent(sound -> player.playSound(player.getLocation(), sound, 0.5F, 0.5F));
		plugin.getFinancialProvider().ifPresent(financialProvider -> {
			financialProvider.addMoney(player, parkourGame.getOptions().getReward());
			player.sendMessage(plugin.msg("reward-parkour"));
		});
		final ParkourCountdown task = new ParkourCountdown(plugin.getTeleportationTimeout(),
				() -> {
					final Map<UUID, Integer> timeCounter = plugin.getTimeCounter();
					plugin.getServer().getScheduler().cancelTask(timeCounter.get(uniqueId));
					timeCounter.remove(uniqueId);
					player.sendMessage(plugin.msg("teleportation-forecast").replace(Constants.SECONDS, String.valueOf(plugin.getTeleportationTimeout())));
				},
				i -> player.sendMessage(plugin.msg("counting-element").replace(Constants.NUMBER, String.valueOf(i))),
				() -> {
					final Map<UUID, Integer> teleportCountdown = plugin.getTeleportCountdown();
					plugin.getServer().getScheduler().cancelTask(teleportCountdown.get(uniqueId));
					teleportCountdown.remove(uniqueId);
					plugin.getParkourManager().teleportToLobby(parkourPlayer);
					plugin.getServer().getPluginManager().callEvent(new PlayerQuitGameEvent(event.getGame(), event.getPlayer()));
				});
		final int schedulerId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, task, 10, 20);
		plugin.getTeleportCountdown().put(uniqueId, schedulerId);
		final double currentTime = parkourPlayer.getTime();
		if (!parkourGame.getOptions().isSavingResults()) {
			return;
		}
		player.sendMessage(plugin.msg("personal-time").replace(Constants.PERSONAL_TIME, plugin.formatTime(currentTime)));
		if (player.hasPermission("ats.parkour.ignoreRecords")) {
			player.sendMessage(plugin.msg("warn-no-database"));
			return;
		}
		performDatabaseOperations(player, parkourGame, currentTime);
	}

	@EventHandler
	public void back(final PlayerTeleportBackEvent event) {
		if (event.getRegion().equals(event.getGame().getSpawn())) {
			event.getPlayer().reset();
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
		plugin.getPlayerManager().teleportToRegion(event.getPlayer(), region);
		plugin.getServer().getPluginManager().callEvent(new PlayerTeleportBackEvent(parkourGame, parkourPlayer, region));
	}

	@EventHandler
	public void exitItemInventoryClick(final InventoryClickEvent event) {
		if (!(event.getWhoClicked() instanceof Player)) {
			return;
		}
		if (Objects.equals(plugin.getExitItem(), event.getCurrentItem())) {
			final Player player = (Player) event.getWhoClicked();
			plugin.getParkourManager().teleportToLobby(player);
			event.setCancelled(true);
			player.closeInventory();
		}
	}

	@EventHandler
	public void hidingItemInventoryClick(final InventoryClickEvent event) {
		if (!(event.getWhoClicked() instanceof Player)) {
			return;
		}
		if (Objects.equals(plugin.getHidingItem(), event.getCurrentItem())) {
			switchHiddenState(plugin.getPlayerManager().getParkourPlayer((Player) event.getWhoClicked()));
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void joinGame(final PlayerJoinGameEvent event) {
		final ParkourPlayer parkourPlayer = event.getPlayer();
		final Player player = parkourPlayer.getPlayer();
		plugin.getSound(EventSound.JOIN)
				.ifPresent(sound -> player.playSound(player.getLocation(), sound, 0.5F, 0.5F));
		final ParkourGame parkourGame = event.getGame();
		if (parkourGame.getOptions().isSavingResults()) {
			final TimeCounter timeCounter = new TimeCounter(
					() -> parkourGame.inSpawn(parkourPlayer),
					i -> {
						if (i == 0) {
							plugin.getSound(EventSound.START)
									.ifPresent(sound -> player.playSound(player.getLocation(), sound, 0.5F, 0.5F));
						}

						final double time = i / 20.;
						parkourPlayer.setTime(time);
						parkourPlayer.getPlayer().setLevel((int) time);
						parkourPlayer.getPlayer().setExp((float) time % 1);
					},
					() -> !parkourPlayer.isIgnoring()
							&& parkourGame.isRunning()
							&& !parkourGame.inCheckpoint(parkourPlayer)
							&& !parkourGame.inSpawn(parkourPlayer));
			final int schedulerId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, timeCounter, 1, 1);
			plugin.getTimeCounter().put(player.getUniqueId(), schedulerId);
		}
		if (parkourGame.getOptions().isModifyInventory()) {
			plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () ->
					plugin.getGameItemMap().iterate(player.getInventory()::setItem), 2);
		}
		parkourGame.getEffects().entrySet().stream()
				.map(entry -> new PotionEffect(entry.getKey(), 99999999, entry.getValue()))
				.forEach(player::addPotionEffect);
		player.sendMessage(plugin.msg("joined-parkour")
				.replace(Constants.NAME, parkourGame.getName())
				.replace(Constants.DISPLAY_NAME, parkourGame.getDisplayName()));
	}

	@EventHandler
	public void parkourTeleportBlockClick(final PlayerInteractEvent event) {
		if (event.getAction().equals(Action.PHYSICAL)) {
			return;
		}
		if (event.getClickedBlock() == null) {
			return;
		}
		plugin.getParkourManager().getAllGames().stream()
				.filter(parkourGame -> Objects.equals(parkourGame.getTeleportBlock(), event.getClickedBlock().getLocation()))
				.filter(ParkourGame::isRunning)
				.findAny()
				.ifPresent(parkourGame -> {
					final Player player = event.getPlayer();
					final ParkourPlayer parkourPlayer = plugin.getPlayerManager().getParkourPlayer(player);
					if (parkourPlayer.isIgnoring()) {
						player.sendMessage(plugin.msg("warn-ignoring"));
						return;
					}
					event.setCancelled(true);
					final boolean isVip = plugin.getRankProvider().filter(rankProvider -> rankProvider.isVip(player)).isPresent();
					if (parkourGame.getOptions().isVipOnly() && !isVip) {
						player.sendMessage(plugin.msg(M.Error.DEFAULT.notVip));
						return;
					}
					plugin.getFinancialProvider().ifPresentOrElse(
							financialProvider -> organizeFinances(financialProvider, parkourGame, parkourPlayer),
							() -> plugin.getPlayerManager().teleportToRegion(parkourPlayer, parkourGame.getSpawn()));
				});
	}

	@EventHandler
	public void hidingItemClick(final PlayerInteractEvent event) {
		Optional.of(event)
				.map(PlayerInteractEvent::getItem)
				.filter(plugin.getHidingItem()::equals)
				.ifPresent(result -> {
					switchHiddenState(plugin.getPlayerManager().getParkourPlayer(event.getPlayer()));
					event.setCancelled(true);
				});
	}

	@EventHandler
	public void entitySpawnInGame(final EntitySpawnEvent event) {
		plugin.getParkourManager().getAllGames()
				.stream()
				.filter(game -> plugin.getParkourManager().inAnyRegion(game, event.getLocation()))
				.findAny()
				.ifPresent(ignored -> event.setCancelled(true));
	}

	@EventHandler
	public void clickInsideGame(final PlayerInteractEvent event) {
		if (!plugin.getParkourManager().getPlayersInGames().contains(event.getPlayer())) {
			return;
		}
		if (event.getClickedBlock() == null) {
			return;
		}
		if (!plugin.isEditLockActive()) {
			return;
		}
		if (Data.getInteractiveMaterials().contains(event.getClickedBlock().getType())
				|| Data.BOATS.contains(event.getMaterial())
				|| event.getMaterial().equals(Material.ARMOR_STAND)) {
			event.getPlayer().sendMessage(plugin.msg(M.Error.DEFAULT.forbiddenModification));
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void exitItemClick(final PlayerInteractEvent event) {
		Optional.of(event)
				.map(PlayerInteractEvent::getItem)
				.filter(plugin.getExitItem()::equals)
				.ifPresent(result -> {
					event.setCancelled(true);
					plugin.getParkourManager().teleportToLobby(event.getPlayer());
				});
	}

	@EventHandler
	public void quitGame(final PlayerQuitGameEvent event) {
		final ParkourPlayer parkourPlayer = event.getPlayer();
		final Player player = parkourPlayer.getPlayer();
		final UUID uniqueId = player.getUniqueId();
		plugin.getSound(EventSound.LEAVE)
				.ifPresent(sound -> player.playSound(player.getLocation(), sound, 0.5F, 0.5F));
		parkourPlayer.reset();
		if (plugin.getTimeCounter().containsKey(uniqueId)) {
			plugin.getServer().getScheduler().cancelTask(plugin.getTimeCounter().get(uniqueId));
			plugin.getTimeCounter().remove(uniqueId);
		}
		plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () ->
				plugin.getWorldItemMap().iterate(player.getInventory()::setItem), 2);
		if (plugin.getTeleportCountdown().containsKey(uniqueId)) {
			player.sendMessage(plugin.msg("warn-cancel-teleportation"));
			plugin.getServer().getScheduler().cancelTask(plugin.getTeleportCountdown().get(uniqueId));
			plugin.getTeleportCountdown().remove(uniqueId);
		}
		event.getGame().getEffects().keySet().forEach(player::removePotionEffect);
		plugin.getPlayerManager().remove(player);
	}

	@EventHandler
	public void gameStop(final GameStopEvent event) {
		event.getGame().getPlayers().forEach(plugin.getParkourManager()::teleportToLobby);
	}

	@EventHandler
	public void gameStart(final GameStartEvent event) {
		final ParkourManager<JSONObject> parkourManager = plugin.getParkourManager();
		event.getGame().getWorld().getPlayers().stream()
				.filter(player -> !plugin.getPlayerManager().getParkourPlayer(player).isIgnoring())
				.filter(player -> parkourManager.inAnyRegion(event.getGame(), player))
				.forEach(parkourManager::teleportToLobby);
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
		final ParkourPlayer parkourPlayer = plugin.getPlayerManager().getParkourPlayer(event.getPlayer());
		if (parkourPlayer.isIgnoring()) {
			return;
		}
		plugin.getParkourManager().getAllGames().forEach(parkourGame -> {
			final boolean destinationInParkour = plugin.getParkourManager().inAnyRegion(parkourGame, event.getTo());
			if (destinationInParkour && !parkourGame.getPlayers().contains(parkourPlayer)) {
				parkourGame.addPlayer(parkourPlayer);
				plugin.getServer().getPluginManager().callEvent(new PlayerJoinGameEvent(parkourGame, parkourPlayer));
			}
			if (!destinationInParkour && parkourGame.getPlayers().contains(parkourPlayer)) {
				parkourGame.removePlayer(parkourPlayer);
				plugin.getServer().getPluginManager().callEvent(new PlayerQuitGameEvent(parkourGame, parkourPlayer));
			}
		});
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
		universalBlockEventHandler(event, event.getPlayer());
	}

	@EventHandler
	public void place(final BlockPlaceEvent event) {
		universalBlockEventHandler(event, event.getPlayer());
	}

	@EventHandler
	public void drop(final PlayerDropItemEvent event) {
		final ParkourPlayer parkourPlayer = plugin.getPlayerManager().getParkourPlayer(event.getPlayer());
		plugin.getParkourManager().getAllGames().stream()
				.filter(parkourGame -> plugin.getParkourManager().inAnyRegion(parkourGame, parkourPlayer))
				.forEach(parkourGame -> {
					if (!parkourPlayer.isIgnoring()) {
						event.setCancelled(true);
					}
				});
	}

	@EventHandler
	public void leave(final PlayerQuitEvent event) {
		final Player player = event.getPlayer();
		final ParkourPlayer parkourPlayer = plugin.getPlayerManager().getParkourPlayer(player);
		Optional.of(plugin)
				.map(ParkourPlugin::getParkourManager)
				.map(parkourManager -> parkourManager.getParkour(player))
				.ifPresent(parkourGame -> parkourGame.removePlayer(parkourPlayer));
		plugin.getPlayerManager().remove(player);
	}

	@EventHandler
	public void breakSpecialBlock(final BlockBreakEvent event) {
		final Location brokenBlockLocation = event.getBlock().getLocation();
		plugin.getParkourManager().getAllGames().forEach(parkourGame -> {
			if (parkourGame.getTeleportBlock() != null && parkourGame.getTeleportBlock().getBlock().getLocation().equals(brokenBlockLocation)) {
				if (parkourGame.isRunning() && plugin.isEditLockActive()) {
					event.getPlayer().sendMessage(plugin.msg(M.Error.DEFAULT.forbiddenModification));
					event.setCancelled(true);
					return;
				}
				parkourGame.setTeleportBlock(null);
				event.getPlayer().sendMessage(plugin.msg("teleport-block-broken"));
			}
			if (parkourGame.getRecordsBlock() != null && parkourGame.getRecordsBlock().getBlock().getLocation().equals(brokenBlockLocation)) {
				if (parkourGame.isRunning() && plugin.isEditLockActive()) {
					event.getPlayer().sendMessage(plugin.msg(M.Error.DEFAULT.forbiddenModification));
					event.setCancelled(true);
					return;
				}
				parkourGame.setRecordsBlock(null);
				event.getPlayer().sendMessage(plugin.msg("records-block-broken"));
			}
		});
	}

	// ======= Helper methods =======

	private void iterateOverRegions(@Nullable final Location from, @Nullable final Location to, @NotNull final ParkourPlayer parkourPlayer, @NotNull final ParkourGame parkourGame) {
		final Player player = parkourPlayer.getPlayer();
		final PluginManager pluginManager = plugin.getServer().getPluginManager();
		parkourGame.getAllRegions().forEach(region -> {
			if (region.contains(from) && !region.contains(to)) {
				pluginManager.callEvent(new PlayerLeaveRegionEvent(parkourGame, player, region));
				return;
			}
			if (region.contains(from) || !region.contains(to)) {
				return;
			}
			pluginManager.callEvent(new PlayerEnterRegionEvent(parkourGame, player, region));
			if (parkourGame.getWalls().contains(region)) {
				pluginManager.callEvent(new PlayerHitWallEvent(parkourGame, parkourPlayer, region));
			}
			if (!(region instanceof DirectionalRegion)) {
				return;
			}
			if (parkourGame.getCheckpoints().contains(region)) {
				pluginManager.callEvent(new PlayerAchieveCheckpointEvent(parkourGame, parkourPlayer, (DirectionalRegion) region));
			}
			if (Objects.equals(parkourGame.getSpawn(), region)) {
				pluginManager.callEvent(new PlayerEnterSpawnEvent(parkourGame, parkourPlayer));
			}
		});
	}

	private void organizeFinances(@NotNull final FinancialProvider financialProvider, @NotNull final ParkourGame parkourGame, @NotNull final ParkourPlayer parkourPlayer) {
		final Player player = parkourPlayer.getPlayer();
		final double fee = parkourGame.getOptions().getFee();
		if (fee <= 0) {
			plugin.getPlayerManager().teleportToRegion(parkourPlayer, parkourGame.getSpawn());
			return;
		}
		if (financialProvider.addMoney(player, -fee)) {
			player.sendMessage(plugin.msg("paid-fee")
					.replace(Constants.FEE, plugin.formatMoney(fee))
					.replace(Constants.NAME, parkourGame.getName())
					.replace(Constants.DISPLAY_NAME, parkourGame.getDisplayName()));
			plugin.getPlayerManager().teleportToRegion(parkourPlayer, parkourGame.getSpawn());
			return;
		}
		player.sendMessage(plugin.msg("warn-no-money")
				.replace(Constants.FEE, plugin.formatMoney(fee))
				.replace(Constants.NAME, parkourGame.getName())
				.replace(Constants.DISPLAY_NAME, parkourGame.getDisplayName())
				.replace(Constants.BALANCE, plugin.formatMoney(financialProvider.getMoney(player))));
	}

	private void switchHiddenState(@NotNull final ParkourPlayer parkourPlayer) {
		parkourPlayer.getPlayer().sendMessage(plugin.msg(parkourPlayer.isHidden() ? "players-shown" : "players-hidden"));
		plugin.getParkourManager().setHidden(parkourPlayer.getPlayer(), plugin, !parkourPlayer.isHidden());
		parkourPlayer.setHidden(!parkourPlayer.isHidden());
	}

	private void universalBlockEventHandler(@NotNull final BlockEvent event, @NotNull final Player player) {
		plugin.getParkourManager().getAllGames()
				.stream()
				.filter(parkourGame -> plugin.getParkourManager().inAnyRegion(parkourGame, event.getBlock().getLocation()))
				.forEach(parkourGame -> {
					final Cancellable cancellableEvent = (Cancellable) event;
					if (!player.hasPermission("ats.parkour.modify")) {
						cancellableEvent.setCancelled(true);
						return;
					}
					if (parkourGame.isRunning() && plugin.isEditLockActive()) {
						player.sendMessage(plugin.msg(M.Error.DEFAULT.forbiddenModification));
						cancellableEvent.setCancelled(true);
					}
				});
	}

	private void performDatabaseOperations(@NotNull final Player player, @NotNull final ParkourGame parkourGame, final double currentTime) {
		plugin.getConnection()
				.map(connection -> new FetchAndInsertDataTask(connection, parkourGame, player.getUniqueId(), currentTime, (previousCount, previousPlayerBest, previousParkourBest) -> {
					player.sendMessage(plugin.msg("completes-count").replace(Constants.COUNT, String.valueOf(previousCount + 1)));
					if (previousParkourBest > currentTime) {
						player.sendMessage(plugin.msg("new-parkour-best")
								.replace(Constants.NAME, parkourGame.getName())
								.replace(Constants.DISPLAY_NAME, parkourGame.getDisplayName()));
						plugin.updateSyncSign(new ParkourRecord(player.getUniqueId(), parkourGame, currentTime));
					}
					if (previousPlayerBest > currentTime) {
						player.sendMessage(plugin.msg("new-personal-best")
								.replace(Constants.NAME, parkourGame.getName())
								.replace(Constants.DISPLAY_NAME, parkourGame.getDisplayName()));
					}
					final ParkourGame.Result previousResult = parkourGame.getResult(previousPlayerBest);
					final ParkourGame.Result result = parkourGame.getResult(currentTime);
					if (result.getMedal() == null || result.getMedal().equals(previousResult.getMedal())) {
						return;
					}
					player.sendMessage(plugin.msg("medal-got").replace(Constants.MEDAL, result.getMedal().getDisplayName()));
					plugin.getFinancialProvider().ifPresent(financialProvider -> {
						final double finalReward = result.getReward() - previousResult.getReward();
						if (finalReward > 0) {
							financialProvider.addMoney(player, finalReward);
							player.sendMessage(plugin.msg("reward-medal").replace(Constants.REWARD, plugin.formatMoney(finalReward)));
						}
					});
				}))
				.ifPresent(fetchAndInsertDataTask -> plugin.getServer().getScheduler().runTaskAsynchronously(plugin, fetchAndInsertDataTask));
	}
}
