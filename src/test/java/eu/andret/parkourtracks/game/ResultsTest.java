package eu.andret.parkourtracks.game;

import eu.andret.parkourtracks.track.MedalThreshold;
import eu.andret.parkourtracks.track.TrackType;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ResultsTest extends GameTest {
	/**
	 * Runs the track from the spawn to the finish, taking the given ticks between the spawn and checkpoint 1, and
	 * waits for the results to come back.
	 */
	void finish(final PlayerMock player, final int ticks) {
		player.teleport(at(-5, 1, 2.5));
		player.simulatePlayerMove(at(2.5, 1, 2.5));
		player.simulatePlayerMove(at(4.5, 1, 2.5));
		server.getScheduler().performTicks(ticks);
		walkTo(player, 8.5);
		walkTo(player, 12.5);
		walkTo(player, 17.5);
		plugin.getResults().flush();
		server.getScheduler().performTicks(1);
	}

	@Test
	void theFirstCompletionIsATrackRecord() {
		// given
		final PlayerMock player = player();

		// when
		finish(player, 100);

		// then
		assertThat(messages(player)).contains("You finished track tower in 00:05.00!", "New track record on tower!");
		assertThat(plugin.getResults().playerResult(track.getId(), player.getUniqueId()).join())
				.hasValueSatisfying(result -> assertThat(result.bestTicks()).isEqualTo(100));
	}

	@Test
	void aFasterRunIsAPersonalBestASlowerOneNothing() {
		// given
		final PlayerMock fast = player();
		final PlayerMock player = player();
		finish(fast, 40);
		finish(player, 100);
		messages(player);

		// when
		finish(player, 80);
		final List<String> better = messages(player);
		games().leave(player, LeaveReason.EXIT);
		finish(player, 90);

		// then
		assertThat(better).contains("New personal best on tower!").doesNotContain("New track record on tower!");
		assertThat(messages(player)).noneMatch(message -> message.startsWith("New"));
	}

	@Test
	void aBetterMedalIsAnnounced() {
		// given
		track.setMedal("gold", new MedalThreshold(100, 0));
		track.setMedal("silver", new MedalThreshold(200, 0));
		final PlayerMock player = player();
		finish(player, 150);
		final List<String> silver = messages(player);

		// when
		games().leave(player, LeaveReason.EXIT);
		finish(player, 160);
		final List<String> again = messages(player);
		games().leave(player, LeaveReason.EXIT);
		finish(player, 50);

		// then
		assertThat(silver).contains("You earned Silver!");
		assertThat(again).noneMatch(message -> message.startsWith("You earned"));
		assertThat(messages(player)).contains("You earned Gold!");
	}

	@Test
	void trainingRunsAreNotSaved() {
		// given
		track.setType(TrackType.TRAINING);
		final PlayerMock player = player();

		// when
		finish(player, 100);

		// then
		assertThat(plugin.getResults().playerResult(track.getId(), player.getUniqueId()).join()).isEmpty();
		assertThat(messages(player)).noneMatch(message -> message.startsWith("New"));
	}
}
