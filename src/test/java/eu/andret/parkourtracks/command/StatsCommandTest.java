package eu.andret.parkourtracks.command;

import eu.andret.parkourtracks.helper.PluginTest;
import eu.andret.parkourtracks.track.Cuboid;
import eu.andret.parkourtracks.track.MedalThreshold;
import eu.andret.parkourtracks.track.Track;
import org.bukkit.permissions.PermissionAttachment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StatsCommandTest extends PluginTest {
	private Track track;
	private PlayerMock alice;
	private PlayerMock bob;

	@BeforeEach
	void setUpResults() {
		track = tower();
		alice = server.addPlayer("Alice");
		bob = server.addPlayer("Bob");
		plugin.getResults().recordRun(track.getId(), alice.getUniqueId(), 600, Instant.ofEpochSecond(100)).join();
		plugin.getResults().recordRun(track.getId(), alice.getUniqueId(), 500, Instant.ofEpochSecond(200)).join();
		plugin.getResults().recordRun(track.getId(), bob.getUniqueId(), 400, Instant.ofEpochSecond(300)).join();
	}

	List<String> stats(final PlayerMock sender, final String arguments) {
		sender.performCommand(("ptracks stats " + arguments).trim());
		plugin.getResults().flush();
		server.getScheduler().performTicks(1);
		return messages(sender);
	}

	@Test
	void showsTheSummaryOfAllTracks() {
		// when
		final List<String> messages = stats(alice, "");

		// then
		assertThat(messages).containsExactly("Tracks completed by Alice:",
				"- tower: best 00:25.00, 2 completions");
	}

	@Test
	void showsTheResultsOnOneTrack() {
		// given
		track.setMedal("gold", new MedalThreshold(500, 0));

		// when
		final List<String> messages = stats(alice, "tower");

		// then
		assertThat(messages).hasSize(6).startsWith("Alice on tower:", "Best time: 00:25.00", "Completions: 2")
				.contains("Medal: Gold", "Track record: 00:20.00 by Bob");
		assertThat(messages.get(3)).startsWith("Last completion: ");
	}

	@Test
	void showsAnotherPlayer() {
		// when
		final List<String> messages = stats(alice, "tower Bob");

		// then
		assertThat(messages).startsWith("Bob on tower:", "Best time: 00:20.00", "Completions: 1")
				.contains("Medal: none");
	}

	@Test
	void anotherPlayerNeedsThePermission() {
		// given
		final PermissionAttachment attachment = alice.addAttachment(plugin);
		attachment.setPermission(Permissions.STATS_OTHERS, false);

		// when
		final List<String> messages = stats(alice, "tower Bob");

		// then
		assertThat(messages).containsExactly("You cannot see the results of other players.");
	}

	@Test
	void unknownPlayer() {
		// when
		final List<String> messages = stats(alice, "tower Nobody");

		// then
		assertThat(messages).containsExactly("No player named Nobody has ever played on this server.");
	}

	@Test
	void noResults() {
		// given
		final PlayerMock carol = server.addPlayer("Carol");
		plugin.getTrackRegistry().create("cave", "world", new Cuboid(50, 0, 0, 60, 10, 10));

		// when
		final List<String> summary = stats(carol, "");
		final List<String> onTrack = stats(carol, "cave");

		// then
		assertThat(summary).containsExactly("Carol has not completed any track yet.");
		assertThat(onTrack).containsExactly("Carol has not completed cave yet.",
				"Track record: nobody has completed it yet.");
	}

	@Test
	void resultsOfRemovedTracksAreKeptButNotShown() {
		// given
		plugin.getTrackRegistry().remove(track);

		// when
		final List<String> messages = stats(alice, "");

		// then
		assertThat(messages).containsExactly("Alice has not completed any track yet.");
		assertThat(plugin.getResults().fetchPlayerSummary(alice.getUniqueId()).join()).hasSize(1);
	}
}
