package eu.andret.parkourtracks.game;

import org.bukkit.Material;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.player.PlayerRiptideEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class MovementRulesTest extends GameTest {
	@Test
	void stoppingSprintingOnASprintForcedTrackSendsBackAfterTheGracePeriod() {
		// given
		track.getOptions().setSprintForced(true);
		final PlayerMock player = running();
		walkTo(player, 9.5);
		player.setSprinting(false);

		// when
		server.getScheduler().performTicks(5);
		final boolean stillThere = player.getLocation().equals(at(9.5, 1, 2.5));
		server.getScheduler().performTicks(1);

		// then
		assertThat(stillThere).isTrue();
		assertThat(player.getLocation()).isEqualTo(at(8.5, 1, 2.5));
		assertThat(messages(player)).contains("You have to keep sprinting on this track.");
	}

	@Test
	void sprintingKeepsThePlayerGoing() {
		// given
		track.getOptions().setSprintForced(true);
		final PlayerMock player = running();
		walkTo(player, 9.5);
		player.setSprinting(true);

		// when
		server.getScheduler().performTicks(40);

		// then
		assertThat(player.getLocation()).isEqualTo(at(9.5, 1, 2.5));
	}

	@Test
	void standingInACheckpointOrOnTheSpawnIsNoStoppedSprint() {
		// given
		track.getOptions().setSprintForced(true);
		final PlayerMock waiting = onSpawn();
		final PlayerMock resting = running();
		walkTo(resting, 8.5);
		resting.setSprinting(false);

		// when
		server.getScheduler().performTicks(40);

		// then
		assertThat(waiting.getLocation()).isEqualTo(at(2.5, 1, 2.5));
		assertThat(resting.getLocation()).isEqualTo(at(8.5, 1, 2.5));
	}

	@Test
	void theGracePeriodIsSetInTheConfig() throws IOException {
		// given
		writeConfig("sprint-grace-ticks: 20\n");
		plugin.reload();
		track.getOptions().setSprintForced(true);
		final PlayerMock player = running();
		walkTo(player, 9.5);
		player.setSprinting(false);

		// when
		server.getScheduler().performTicks(20);

		// then
		assertThat(player.getLocation()).isEqualTo(at(9.5, 1, 2.5));
	}

	@Test
	void glidingIsBlocked() {
		// given
		final PlayerMock player = running();
		final PlayerMock outside = player();
		final EntityToggleGlideEvent start = new EntityToggleGlideEvent(player, true);
		final EntityToggleGlideEvent startOutside = new EntityToggleGlideEvent(outside, true);

		// when
		server.getPluginManager().callEvent(start);
		server.getPluginManager().callEvent(startOutside);

		// then
		assertThat(start.isCancelled()).isTrue();
		assertThat(startOutside.isCancelled()).isFalse();
	}

	@Test
	void riptideIsBlocked() {
		// given
		final PlayerMock player = running();
		final PlayerRiptideEvent event = new PlayerRiptideEvent(player, new ItemStack(Material.TRIDENT),
				player.getVelocity());

		// when
		server.getPluginManager().callEvent(event);

		// then
		assertThat(event.isCancelled()).isTrue();
	}

	@Test
	void chorusFruitDoesNotTeleport() {
		// given
		final PlayerMock player = running();

		// when
		player.teleport(at(9.5, 1, 2.5), PlayerTeleportEvent.TeleportCause.CONSUMABLE_EFFECT);

		// then
		assertThat(player.getLocation()).isEqualTo(at(5.5, 1, 2.5));
	}
}
