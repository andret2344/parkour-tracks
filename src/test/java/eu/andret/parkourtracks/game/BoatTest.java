package eu.andret.parkourtracks.game;

import org.bukkit.Location;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BoatTest extends GameTest {
	@BeforeEach
	void setUpBoat() {
		track.getOptions().setBoat(true);
	}

	@NotNull
	List<Boat> boats() {
		return world.getEntitiesByClass(Boat.class).stream().filter(Entity::isValid).toList();
	}

	/**
	 * Drives the player's boat from where it is to the given x, one block per move.
	 */
	void driveTo(@NotNull final PlayerMock player, final double x) {
		final Vehicle boat = (Vehicle) player.getVehicle();
		double current = boat.getLocation().getX();
		while (Math.abs(x - current) > 0.0001 && player.getVehicle() == boat) {
			current += Math.max(-1, Math.min(1, x - current));
			final Location from = boat.getLocation();
			final Location to = at(current, 1, 2.5);
			boat.teleport(to);
			server.getPluginManager().callEvent(new VehicleMoveEvent(boat, from, to));
		}
	}

	@Test
	void joiningPutsThePlayerInABoatOnTheSpawn() {
		// when
		final PlayerMock player = onSpawn();

		// then
		assertThat(player.getVehicle()).isInstanceOf(Boat.class);
		assertThat(player.getVehicle().getType()).isEqualTo(EntityType.OAK_BOAT);
		assertThat(player.getVehicle().isPersistent()).isFalse();
		assertThat(player.getVehicle().getLocation()).isEqualTo(at(2.5, 1, 2.5));
		assertThat(session(player).getBoat()).isEqualTo(player.getVehicle().getUniqueId());
	}

	@Test
	void theBoatTypeIsTheTracks() {
		// given
		track.getOptions().setBoatType(EntityType.BAMBOO_RAFT);

		// when
		final PlayerMock player = onSpawn();

		// then
		assertThat(player.getVehicle().getType()).isEqualTo(EntityType.BAMBOO_RAFT);
	}

	@Test
	void everyWayInStartsInABoat() {
		// given
		final PlayerMock side = player();
		final PlayerMock entered = player();

		// when
		side.simulatePlayerMove(at(10, 1, 20));
		games().enter(entered, track);

		// then
		assertThat(side.getVehicle()).isInstanceOf(Boat.class);
		assertThat(entered.getVehicle()).isInstanceOf(Boat.class);
	}

	@Test
	void thePlayerCannotGetOut() {
		// given
		final PlayerMock player = onSpawn();
		final Entity boat = player.getVehicle();

		// when
		player.leaveVehicle();

		// then
		assertThat(player.getVehicle()).isEqualTo(boat);
	}

	@Test
	void movesInTheBoatAreChecked() {
		// given
		final PlayerMock player = onSpawn();

		// when
		driveTo(player, 8.5);

		// then
		assertThat(session(player).getPhase()).isEqualTo(Phase.RUNNING);
		assertThat(session(player).getLastCheckpoint()).isZero();
	}

	@Test
	void goingBackReplacesTheBoatWithOneStandingStill() {
		// given
		final PlayerMock player = onSpawn();
		driveTo(player, 9.5);
		final Entity oldBoat = player.getVehicle();
		oldBoat.setVelocity(new Vector(3, 0, 0));

		// when
		games().goBack(player, session(player));

		// then
		assertThat(oldBoat.isValid()).isFalse();
		assertThat(player.getVehicle()).isNotNull().isNotEqualTo(oldBoat);
		assertThat(player.getVehicle().getLocation()).isEqualTo(at(8.5, 1, 2.5));
		assertThat(player.getVehicle().getVelocity().lengthSquared()).isZero();
		assertThat(boats()).hasSize(1);
	}

	@Test
	void nothingElseGetsIntoTheBoat() {
		// given
		final PlayerMock player = onSpawn();
		final PlayerMock other = player();
		final Vehicle boat = (Vehicle) player.getVehicle();
		final VehicleEnterEvent event = new VehicleEnterEvent(boat, other);

		// when
		server.getPluginManager().callEvent(event);

		// then
		assertThat(event.isCancelled()).isTrue();
	}

	@Test
	void theBoatCannotBeDamagedOrDestroyed() {
		// given
		final PlayerMock player = onSpawn();
		final Vehicle boat = (Vehicle) player.getVehicle();
		final VehicleDamageEvent damage = new VehicleDamageEvent(boat, DamageSource.builder(DamageType.PLAYER_ATTACK).build(), player(), 10);
		final VehicleDestroyEvent destroy = new VehicleDestroyEvent(boat, DamageSource.builder(DamageType.PLAYER_ATTACK).build(), player());

		// when
		server.getPluginManager().callEvent(damage);
		server.getPluginManager().callEvent(destroy);

		// then
		assertThat(damage.isCancelled()).isTrue();
		assertThat(destroy.isCancelled()).isTrue();
	}

	@Test
	void aBoatGoneAnywaySendsBackInANewOne() {
		// given
		final PlayerMock player = onSpawn();
		driveTo(player, 9.5);

		// when
		player.getVehicle().remove();
		server.getScheduler().performTicks(1);

		// then
		assertThat(player.getVehicle()).isInstanceOf(Boat.class);
		assertThat(player.getVehicle().getLocation()).isEqualTo(at(8.5, 1, 2.5));
	}

	@Test
	void leavingTheGameRemovesTheBoat() {
		// given
		final PlayerMock player = onSpawn();
		final Entity boat = player.getVehicle();

		// when
		games().leave(player, LeaveReason.EXIT);

		// then
		assertThat(player.getVehicle()).isNull();
		assertThat(boat.isValid()).isFalse();
		assertThat(player.getLocation()).isEqualTo(at(100, 64, 100));
	}

	@Test
	void drivingOutOfTheRegionEndsTheGameAndRemovesTheBoat() {
		// given
		final PlayerMock player = onSpawn();
		final Vehicle boat = (Vehicle) player.getVehicle();

		// when
		server.getPluginManager().callEvent(new VehicleMoveEvent(boat, at(2.5, 1, 2.5), at(-1.5, 1, 2.5)));

		// then
		assertThat(games().getSession(player)).isEmpty();
		assertThat(boat.isValid()).isFalse();
	}

	@Test
	void finishingInTheBoatWorks() {
		// given
		final PlayerMock player = onSpawn();

		// when
		driveTo(player, 8.5);
		driveTo(player, 12.5);
		driveTo(player, 17.5);

		// then
		assertThat(session(player).getPhase()).isEqualTo(Phase.FINISHED);
	}

	@Test
	void playersOnAFootTrackGetIntoNoVehicle() {
		// given
		track.getOptions().setBoat(false);
		final PlayerMock player = running();
		final Minecart minecart = world.spawn(at(5.5, 1, 2.5), Minecart.class);

		// when
		final boolean entered = minecart.addPassenger(player);

		// then
		assertThat(entered).isFalse();
		assertThat(player.getVehicle()).isNull();
	}
}
