package eu.andret.parkourtracks.result;

import eu.andret.parkourtracks.helper.PluginTest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class BackupsTest extends PluginTest {
	Path backups() {
		return plugin.getDataFolder().toPath().resolve("backups");
	}

	@Test
	void copiesTheTracksAndTheResults() {
		// given
		tower();
		plugin.getResults().recordRun(UUID.randomUUID(), UUID.randomUUID(), 100, Instant.now()).join();

		// when
		final Path backup = plugin.getBackups().backUp().join();

		// then
		assertThat(backup.getParent()).isEqualTo(backups());
		assertThat(backup.resolve("tracks.json")).exists();
		assertThat(backup.resolve("results.db")).exists();
		final ResultStore copy = ResultStore.open(backup.resolve("results.db"));
		try {
			assertThat(copy.fetchRanked(plugin.getTrackRegistry().find("tower").orElseThrow().getId(), 1).join()).isEmpty();
		} finally {
			copy.close();
		}
	}

	@Test
	void keepsOnlyTheNewest() throws IOException {
		// given
		writeConfig("backup-keep: 2\n");
		plugin.reload();
		Files.createDirectories(backups().resolve("2020-01-01_00-00-00-000"));
		Files.createDirectories(backups().resolve("2021-01-01_00-00-00-000"));
		Files.writeString(backups().resolve("2021-01-01_00-00-00-000").resolve("tracks.json"), "{}");

		// when
		final Path backup = plugin.getBackups().backUp().join();

		// then
		try (final Stream<Path> children = Files.list(backups())) {
			assertThat(children.map(Path::getFileName).map(Path::toString))
					.containsExactlyInAnyOrder("2021-01-01_00-00-00-000", backup.getFileName().toString());
		}
	}

	@Test
	void runsOnTheSchedule() throws IOException {
		// given
		writeConfig("backup-frequency: 1\n");
		plugin.reload();

		// when
		server.getScheduler().performTicks(20 * 60);
		plugin.getResults().flush();

		// then
		try (final Stream<Path> children = Files.list(backups())) {
			assertThat(children).hasSize(1);
		}
	}

	@Test
	void noScheduleWhenTurnedOff() throws IOException {
		// given
		writeConfig("backup-frequency: 0\n");
		plugin.reload();

		// when
		server.getScheduler().performTicks(20 * 60 * 1440);
		plugin.getResults().flush();

		// then
		assertThat(backups()).doesNotExist();
	}
}
