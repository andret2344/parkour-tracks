package eu.andret.parkourtracks.game;

import org.bukkit.Material;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Arrow;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.simulate.entity.LivingEntitySimulation;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GameRulesTest extends GameTest {
	/**
	 * Damages the player through MockBukkit, which calls the event and applies the damage unless it is canceled.
	 */
	EntityDamageEvent damage(final PlayerMock player, final DamageSource source) {
		return new LivingEntitySimulation(player).simulateDamage(4, source);
	}

	EntityDamageEvent fall(final PlayerMock player) {
		return damage(player, DamageSource.builder(DamageType.FALL).build());
	}

	@Test
	void damageFromTheEnvironmentIsBlockedUnlessAllowed() {
		// given
		final PlayerMock player = running();
		final PlayerMock outsider = player();

		// when
		final EntityDamageEvent blocked = fall(player);
		track.getOptions().setDamageAllowed(true);
		final EntityDamageEvent allowed = fall(player);
		final EntityDamageEvent outside = fall(outsider);

		// then
		assertThat(blocked.isCancelled()).isTrue();
		assertThat(allowed.isCancelled()).isFalse();
		assertThat(outside.isCancelled()).isFalse();
	}

	@Test
	void damageFromPlayersIsAlwaysBlocked() {
		// given
		track.getOptions().setDamageAllowed(true);
		final PlayerMock player = running();
		final PlayerMock attacker = player();
		final Arrow arrow = world.spawn(at(5, 1, 2), Arrow.class);
		arrow.setShooter(attacker);

		// when
		final EntityDamageEvent hit = damage(player, DamageSource.builder(DamageType.PLAYER_ATTACK)
				.withDirectEntity(attacker)
				.withCausingEntity(attacker)
				.build());
		final EntityDamageEvent shot = damage(player, DamageSource.builder(DamageType.ARROW)
				.withDirectEntity(arrow)
				.withCausingEntity(attacker)
				.build());

		// then
		assertThat(hit.isCancelled()).isTrue();
		assertThat(shot.isCancelled()).isTrue();
		assertThat(player.getHealth()).isEqualTo(20);
	}

	@Test
	void hungerIsOff() {
		// given
		final PlayerMock player = running();
		final FoodLevelChangeEvent event = new FoodLevelChangeEvent(player, 10, null);

		// when
		server.getPluginManager().callEvent(event);

		// then
		assertThat(event.isCancelled()).isTrue();
		assertThat(player.getFoodLevel()).isEqualTo(20);
	}

	@Test
	void flyingIsBlocked() {
		// given
		final PlayerMock player = running();
		final PlayerToggleFlightEvent start = new PlayerToggleFlightEvent(player, true);
		final PlayerToggleFlightEvent stop = new PlayerToggleFlightEvent(player, false);

		// when
		server.getPluginManager().callEvent(start);
		server.getPluginManager().callEvent(stop);

		// then
		assertThat(start.isCancelled()).isTrue();
		assertThat(stop.isCancelled()).isFalse();
	}

	@Test
	void deathKeepsEverythingAndRespawnsOnTheLastCheckpoint() {
		// given
		track.getOptions().setDamageAllowed(true);
		final PlayerMock player = running();
		walkTo(player, 9.5);
		player.getInventory().addItem(new ItemStack(Material.STICK));
		// MockBukkit only follows the keepInventory game rule, so the event is checked instead of the inventory
		final List<PlayerDeathEvent> deaths = new ArrayList<>();
		server.getPluginManager().registerEvents(new Listener() {
			@EventHandler(priority = EventPriority.MONITOR)
			public void death(final PlayerDeathEvent event) {
				deaths.add(event);
			}
		}, plugin);

		// when
		player.setHealth(0);
		player.respawn();

		// then
		assertThat(deaths).singleElement().satisfies(death -> {
			assertThat(death.getKeepInventory()).isTrue();
			assertThat(death.getKeepLevel()).isTrue();
			assertThat(death.getDrops()).isEmpty();
			assertThat(death.getDroppedExp()).isZero();
		});
		assertThat(player.getLocation()).isEqualTo(at(8.5, 1, 2.5));
		assertThat(session(player).getPhase()).isEqualTo(Phase.RUNNING);
	}

	@Test
	void deathBeforeTheFirstCheckpointRestartsOnTheSpawn() {
		// given
		track.getOptions().setDamageAllowed(true);
		final PlayerMock player = running();

		// when
		player.setHealth(0);
		player.respawn();

		// then
		assertThat(player.getLocation()).isEqualTo(at(2.5, 1, 2.5));
		assertThat(session(player).getPhase()).isEqualTo(Phase.WAITING);
	}
}
