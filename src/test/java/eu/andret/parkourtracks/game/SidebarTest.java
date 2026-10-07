package eu.andret.parkourtracks.game;

import eu.andret.parkourtracks.track.MedalThreshold;
import eu.andret.parkourtracks.track.TrackType;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.io.IOException;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class SidebarTest extends GameTest {
	void waitForResults() {
		plugin.getResults().flush();
		server.getScheduler().performTicks(1);
	}

	@Test
	void joiningShowsTheResultsOnTheTrack() {
		// given
		track.setMedal("gold", new MedalThreshold(500, 0));
		final PlayerMock player = player();
		final PlayerMock bob = server.addPlayer("Bob");
		plugin.getResults().recordRun(track.getId(), player.getUniqueId(), 450, Instant.now()).join();
		plugin.getResults().recordRun(track.getId(), bob.getUniqueId(), 300, Instant.now()).join();

		// when
		player.teleport(at(-5, 1, 2.5));
		move(player, at(2.5, 1, 2.5));
		waitForResults();

		// then
		assertThat(sidebar.of(player)).containsExactly("tower", "Your best: 00:22.50", "Record: 00:15.00 by Bob",
				"Medal: Gold", "Completions: 1");
	}

	@Test
	void aNewPlayerSeesNoResultsYet() {
		// when
		final PlayerMock player = onSpawn();
		waitForResults();

		// then
		assertThat(sidebar.of(player)).containsExactly("tower", "Your best: none", "Record: none by nobody yet",
				"Medal: none", "Completions: 0");
	}

	@Test
	void finishingUpdatesTheSidebarOfEveryoneOnTheTrack() {
		// given
		final PlayerMock runner = running();
		final PlayerMock watcher = onSpawn();
		waitForResults();

		// when
		walkTo(runner, 8.5);
		walkTo(runner, 12.5);
		walkTo(runner, 17.5);
		waitForResults();
		waitForResults();

		// then
		assertThat(sidebar.of(runner)).contains("Completions: 1");
		assertThat(sidebar.of(watcher)).anyMatch(line -> line.startsWith("Record: ") && line.contains(runner.getName()));
	}

	@Test
	void leavingHidesIt() {
		// given
		final PlayerMock player = onSpawn();
		waitForResults();

		// when
		games().leave(player, LeaveReason.EXIT);

		// then
		assertThat(sidebar.of(player)).isNull();
	}

	@Test
	void anEmptyTextLeavesItsLineOut() throws IOException {
		// given
		writeMessages("scoreboard-medal: ''\nscoreboard-completions: ''\n");
		plugin.reload();

		// when
		final PlayerMock player = onSpawn();
		waitForResults();

		// then
		assertThat(sidebar.of(player)).containsExactly("tower", "Your best: none", "Record: none by nobody yet");
	}

	@Test
	void noSidebarWhenTurnedOffOrOnTrainingTracks() throws IOException {
		// given
		writeConfig("scoreboard: false\n");
		plugin.reload();
		final PlayerMock off = onSpawn();
		waitForResults();
		writeConfig("");
		plugin.reload();
		track.setType(TrackType.TRAINING);

		// when
		final PlayerMock training = onSpawn();
		waitForResults();

		// then
		assertThat(sidebar.of(off)).isNull();
		assertThat(sidebar.of(training)).isNull();
	}
}
