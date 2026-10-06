package eu.andret.parkourtracks.game;

import eu.andret.parkourtracks.track.AfterFinish;
import eu.andret.parkourtracks.track.SkipMode;
import eu.andret.parkourtracks.track.TrackType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class RunTest extends GameTest {
	@Test
	void leavingTheSpawnStartsTheRunAndTheTimerCountsTicks() {
		// given
		final PlayerMock player = onSpawn();
		server.getScheduler().performTicks(10);
		final int ticksOnSpawn = session(player).getTicks();

		// when
		player.simulatePlayerMove(at(4.5, 1, 2.5));
		server.getScheduler().performTicks(30);

		// then
		assertThat(ticksOnSpawn).isZero();
		assertThat(session(player).getPhase()).isEqualTo(Phase.RUNNING);
		assertThat(session(player).getTicks()).isEqualTo(30);
	}

	@Test
	void theRunningTimeIsShownInTheActionBar() {
		// given
		final PlayerMock player = running();
		server.getScheduler().performTicks(29);
		while (player.nextActionBar() != null) {
			// Only the last one matters
		}

		// when
		server.getScheduler().performTicks(1);

		// then
		final Component bar = player.nextActionBar();
		assertThat(PlainTextComponentSerializer.plainText().serialize(bar)).isEqualTo("00:01.50");
	}

	@Test
	void theXpBarCanShowTheTime() throws IOException {
		// given
		writeConfig("timer-display: xp-bar\n");
		plugin.reload();
		final PlayerMock player = running();

		// when
		server.getScheduler().performTicks(50);

		// then
		assertThat(player.getLevel()).isEqualTo(2);
		assertThat(player.getExp()).isEqualTo(0.5F);
		assertThat(player.nextActionBar()).isNull();
	}

	@Test
	void checkpointsArePassedInOrder() {
		// given
		final PlayerMock player = running();

		// when
		walkTo(player, 8.5);
		walkTo(player, 12.5);

		// then
		assertThat(messages(player)).containsExactly("Checkpoint 1/2.", "Checkpoint 2/2.");
		assertThat(session(player).getLastCheckpoint()).isEqualTo(1);
	}

	@Test
	void aFastMoveThroughAThinCheckpointPassesIt() {
		// given
		final PlayerMock player = running();
		player.simulatePlayerMove(at(7.5, 1, 2.5));

		// when: from x 7.5 to 9.5 in one move, never standing in checkpoint 1 at x 8
		player.simulatePlayerMove(at(9.5, 1, 2.5));

		// then
		assertThat(session(player).getLastCheckpoint()).isZero();
	}

	@Test
	void skippingACheckpointFailsByDefault() {
		// given
		final PlayerMock player = running();

		// when: past checkpoint 1 at the side of the track, straight into checkpoint 2
		player.simulatePlayerMove(at(5.5, 1, 5.5));
		walkToZ(player, 10.5, 5.5);
		player.simulatePlayerMove(at(12.5, 1, 2.5));

		// then
		assertThat(messages(player)).containsExactly("You missed checkpoint 1.");
		assertThat(player.getLocation()).isEqualTo(at(2.5, 1, 2.5));
		assertThat(session(player).getPhase()).isEqualTo(Phase.WAITING);
	}

	@Test
	void skippingWithNotifyCountsAndTells() {
		// given
		track.getOptions().setSkipMode(SkipMode.NOTIFY);
		final PlayerMock player = running();

		// when
		player.simulatePlayerMove(at(5.5, 1, 5.5));
		walkToZ(player, 10.5, 5.5);
		player.simulatePlayerMove(at(12.5, 1, 2.5));

		// then
		assertThat(messages(player)).containsExactly("You missed checkpoint 1.", "Checkpoint 2/2.");
		assertThat(session(player).getLastCheckpoint()).isEqualTo(1);
	}

	@Test
	void skippingWithAllowCountsSilently() {
		// given
		track.getOptions().setSkipMode(SkipMode.ALLOW);
		final PlayerMock player = running();

		// when
		player.simulatePlayerMove(at(5.5, 1, 5.5));
		walkToZ(player, 10.5, 5.5);
		player.simulatePlayerMove(at(12.5, 1, 2.5));

		// then
		assertThat(messages(player)).containsExactly("Checkpoint 2/2.");
		assertThat(session(player).getLastCheckpoint()).isEqualTo(1);
	}

	@Test
	void theFinishCountsOnlyAfterEveryCheckpoint() {
		// given
		final PlayerMock player = running();
		walkTo(player, 8.5);
		messages(player);

		// when: around checkpoint 2, into the finish
		player.simulatePlayerMove(at(9.5, 1, 5.5));
		walkToZ(player, 16.5, 5.5);
		player.simulatePlayerMove(at(17.5, 1, 2.5));

		// then: back to checkpoint 1
		assertThat(messages(player)).containsExactly("You missed checkpoint 2.");
		assertThat(player.getLocation()).isEqualTo(at(8.5, 1, 2.5));
		assertThat(session(player).getPhase()).isEqualTo(Phase.RUNNING);
	}

	@Test
	void finishingShowsTheTimeAndSendsToTheLobbyAfterTheDelay() {
		// given
		final PlayerMock player = running();
		walkTo(player, 8.5);
		server.getScheduler().performTicks(40);
		walkTo(player, 12.5);
		messages(player);

		// when
		walkTo(player, 17.5);
		final Phase phase = session(player).getPhase();
		server.getScheduler().performTicks(99);
		final boolean stillThere = games().getSession(player).isPresent();
		server.getScheduler().performTicks(1);

		// then
		assertThat(phase).isEqualTo(Phase.FINISHED);
		assertThat(stillThere).isTrue();
		assertThat(games().getSession(player)).isEmpty();
		assertThat(player.getLocation()).isEqualTo(at(100, 64, 100));
		assertThat(messages(player)).containsExactly("You finished track tower in 00:02.00!",
				"Back to the lobby in 5 seconds.", "You left track tower.");
	}

	@Test
	void theTimerStopsAtTheFinish() {
		// given
		final PlayerMock player = running();
		walkTo(player, 8.5);
		walkTo(player, 12.5);
		walkTo(player, 17.5);

		// when
		server.getScheduler().performTicks(20);

		// then
		assertThat(session(player).getTicks()).isZero();
	}

	@Test
	void noDelaySendsToTheLobbyRightAway() throws IOException {
		// given
		writeConfig("finish-delay: 0\n");
		plugin.reload();
		final PlayerMock player = running();
		walkTo(player, 8.5);
		walkTo(player, 12.5);

		// when
		walkTo(player, 17.5);

		// then
		assertThat(games().getSession(player)).isEmpty();
		assertThat(player.getLocation()).isEqualTo(at(100, 64, 100));
	}

	@Test
	void finishingCanSendBackToTheSpawnForAnotherTry() {
		// given
		track.getOptions().setAfterFinish(AfterFinish.SPAWN);
		final PlayerMock player = running();
		walkTo(player, 8.5);
		walkTo(player, 12.5);

		// when
		walkTo(player, 17.5);

		// then
		assertThat(player.getLocation()).isEqualTo(at(2.5, 1, 2.5));
		assertThat(session(player).getPhase()).isEqualTo(Phase.WAITING);
		assertThat(session(player).getTicks()).isZero();
	}

	@Test
	void aTrainingTrackHasNoTimer() {
		// given
		track.setType(TrackType.TRAINING);
		final PlayerMock player = running();
		server.getScheduler().performTicks(20);
		walkTo(player, 8.5);
		walkTo(player, 12.5);
		messages(player);

		// when
		walkTo(player, 17.5);

		// then
		assertThat(session(player).getTicks()).isZero();
		assertThat(player.nextActionBar()).isNull();
		assertThat(messages(player)).containsExactly("You finished track tower!", "Back to the lobby in 5 seconds.");
	}

	@Test
	void aWallSendsBackToTheLastCheckpoint() {
		// given
		final PlayerMock player = running();
		walkTo(player, 9.5);

		// when
		player.simulatePlayerMove(at(10.5, 0.5, 2.5));

		// then
		assertThat(player.getLocation()).isEqualTo(at(8.5, 1, 2.5));
		assertThat(session(player).getPhase()).isEqualTo(Phase.RUNNING);
	}

	@Test
	void aWallBeforeTheFirstCheckpointSendsToTheSpawnAndRestartsTheRun() {
		// given
		final PlayerMock player = running();
		server.getScheduler().performTicks(20);

		// when
		player.simulatePlayerMove(at(5.5, 0.5, 2.5));

		// then
		assertThat(player.getLocation()).isEqualTo(at(2.5, 1, 2.5));
		assertThat(session(player).getPhase()).isEqualTo(Phase.WAITING);
		assertThat(session(player).getTicks()).isZero();
	}

	@Test
	void hardcoreSendsEveryFailureToTheSpawn() {
		// given
		track.getOptions().setHardcore(true);
		final PlayerMock player = running();
		walkTo(player, 9.5);

		// when
		player.simulatePlayerMove(at(10.5, 0.5, 2.5));

		// then
		assertThat(player.getLocation()).isEqualTo(at(2.5, 1, 2.5));
		assertThat(session(player).getPhase()).isEqualTo(Phase.WAITING);
		assertThat(session(player).getLastCheckpoint()).isEqualTo(GameSession.SPAWN);
	}

	@Test
	void walkingBackIntoTheSpawnStartsOver() {
		// given
		final PlayerMock player = running();
		walkTo(player, 8.5);

		// when
		walkTo(player, 3.5);

		// then
		assertThat(session(player).getPhase()).isEqualTo(Phase.WAITING);
		assertThat(session(player).getLastCheckpoint()).isEqualTo(GameSession.SPAWN);
	}

	@Test
	void theTimerCanPauseOnCheckpoints() {
		// given
		track.getOptions().setPauseOnCheckpoints(true);
		final PlayerMock player = running();
		walkTo(player, 8.5);

		// when
		server.getScheduler().performTicks(40);
		final int paused = session(player).getTicks();
		walkTo(player, 9.5);
		server.getScheduler().performTicks(10);

		// then
		assertThat(paused).isZero();
		assertThat(session(player).getTicks()).isEqualTo(10);
	}

	@Test
	void theTimerDoesNotPauseOnCheckpointsByDefault() {
		// given
		final PlayerMock player = running();
		walkTo(player, 8.5);

		// when
		server.getScheduler().performTicks(40);

		// then
		assertThat(session(player).getTicks()).isEqualTo(40);
	}

	@Test
	void restartPutsBackOnTheSpawn() {
		// given
		final PlayerMock player = running();
		walkTo(player, 9.5);

		// when
		games().restart(player, session(player));

		// then
		assertThat(player.getLocation()).isEqualTo(at(2.5, 1, 2.5));
		assertThat(session(player).getPhase()).isEqualTo(Phase.WAITING);
	}

	/**
	 * Moves the player along x at the given z, one block per move.
	 */
	void walkToZ(final PlayerMock player, final double x, final double z) {
		double current = player.getLocation().getX();
		while (Math.abs(x - current) > 0.0001) {
			current += Math.max(-1, Math.min(1, x - current));
			player.simulatePlayerMove(at(current, 1, z));
		}
	}
}
