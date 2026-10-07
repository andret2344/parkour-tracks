package eu.andret.parkourtracks.game;

import eu.andret.parkourtracks.track.Track;
import eu.andret.parkourtracks.track.TrackOptions;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerRiptideEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Reports to the {@link GameManager} what players do; decides nothing itself. Handlers of cancelable events ignore
 * canceled ones and run at {@code HIGH}, after protection plugins canceling at {@code NORMAL} or lower.
 */
public final class GameListener implements Listener {
	@NotNull
	private final GameManager games;

	public GameListener(@NotNull final GameManager games) {
		this.games = games;
	}

	/**
	 * A player riding moves with the vehicle, which {@link #vehicleMove} handles.
	 */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void move(@NotNull final PlayerMoveEvent event) {
		if (event.hasChangedPosition() && !event.getPlayer().isInsideVehicle()) {
			games.move(event.getPlayer(), event.getFrom(), event.getTo(), true);
		}
	}

	@EventHandler
	public void vehicleMove(@NotNull final VehicleMoveEvent event) {
		for (final Entity passenger : List.copyOf(event.getVehicle().getPassengers())) {
			if (passenger instanceof final Player player) {
				games.move(player, event.getFrom(), event.getTo(), true);
			}
		}
	}

	/**
	 * A player on a boat track stays in their boat; only the plugin takes them out.
	 */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void vehicleExit(@NotNull final VehicleExitEvent event) {
		if (!games.isBoating() && event.getExited() instanceof final Player player
				&& games.getSession(player).map(session -> session.getTrack().getOptions().isBoat()).orElse(false)) {
			event.setCancelled(true);
		}
	}

	/**
	 * Nothing but its player gets into a game boat, and players in a game get into no other vehicle: riding, their
	 * moves along the track would not be checked.
	 */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void vehicleEnter(@NotNull final VehicleEnterEvent event) {
		if (games.isBoating()) {
			return;
		}
		final boolean inGame = event.getEntered() instanceof final Player player && games.getSession(player).isPresent();
		if (inGame || games.isGameBoat(event.getVehicle())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void vehicleDamage(@NotNull final VehicleDamageEvent event) {
		if (games.isGameBoat(event.getVehicle())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void vehicleDestroy(@NotNull final VehicleDestroyEvent event) {
		if (games.isGameBoat(event.getVehicle())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void teleport(@NotNull final PlayerTeleportEvent event) {
		if (games.isTeleporting()) {
			return;
		}
		final Player player = event.getPlayer();
		if (event.getCause() == PlayerTeleportEvent.TeleportCause.CONSUMABLE_EFFECT && games.getSession(player).isPresent()) {
			// Chorus fruit
			event.setCancelled(true);
			return;
		}
		final boolean pearl = event.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL
				&& games.getSession(player)
				.map(GameSession::getTrack)
				.map(Track::getOptions)
				.map(TrackOptions::isEnderPearls)
				.orElse(false);
		final Location target = games.handleTeleport(player, event.getFrom(), event.getTo(), pearl);
		if (target != null) {
			event.setTo(target);
		}
	}

	@EventHandler
	public void join(@NotNull final PlayerJoinEvent event) {
		games.arrive(event.getPlayer());
	}

	@EventHandler
	public void quit(@NotNull final PlayerQuitEvent event) {
		games.leave(event.getPlayer(), LeaveReason.DISCONNECT);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void gameMode(@NotNull final PlayerGameModeChangeEvent event) {
		final GameMode mode = event.getNewGameMode();
		if (mode == GameMode.CREATIVE || mode == GameMode.SPECTATOR) {
			games.leave(event.getPlayer(), LeaveReason.NOT_A_PLAYER);
		}
	}

	/**
	 * Damage from other players never counts during a game; damage from anything else only when the track allows it.
	 */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void damage(@NotNull final EntityDamageEvent event) {
		if (!(event.getEntity() instanceof final Player player)) {
			return;
		}
		games.getSession(player).ifPresent(session -> {
			if (isFromPlayer(event) || !session.getTrack().getOptions().isDamageAllowed()) {
				event.setCancelled(true);
			}
		});
	}

	private static boolean isFromPlayer(@NotNull final EntityDamageEvent event) {
		if (!(event instanceof final EntityDamageByEntityEvent byEntity)) {
			return false;
		}
		final Entity damager = byEntity.getDamager();
		return damager instanceof Player
				|| damager instanceof final Projectile projectile && projectile.getShooter() instanceof Player;
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void hunger(@NotNull final FoodLevelChangeEvent event) {
		if (event.getEntity() instanceof final Player player && games.getSession(player).isPresent()) {
			event.setCancelled(true);
			player.setFoodLevel(20);
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void glide(@NotNull final EntityToggleGlideEvent event) {
		if (event.isGliding() && event.getEntity() instanceof final Player player && games.getSession(player).isPresent()) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void riptide(@NotNull final PlayerRiptideEvent event) {
		if (games.getSession(event.getPlayer()).isPresent()) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void flight(@NotNull final PlayerToggleFlightEvent event) {
		if (event.isFlying() && games.getSession(event.getPlayer()).isPresent()) {
			event.setCancelled(true);
		}
	}

	/**
	 * Nothing drops on death during a game: the inventory holds only game items, and the player's own are safe in
	 * the snapshot.
	 */
	@EventHandler(priority = EventPriority.HIGH)
	public void death(@NotNull final PlayerDeathEvent event) {
		if (games.getSession(event.getPlayer()).isPresent()) {
			event.setKeepInventory(true);
			event.getDrops().clear();
			event.setKeepLevel(true);
			event.setDroppedExp(0);
		}
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void respawn(@NotNull final PlayerRespawnEvent event) {
		games.getSession(event.getPlayer())
				.ifPresent(session -> event.setRespawnLocation(games.findRespawnLocation(session)));
	}
}
