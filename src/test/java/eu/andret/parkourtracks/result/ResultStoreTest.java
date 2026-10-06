package eu.andret.parkourtracks.result;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResultStoreTest {
	private static final UUID TOWER = UUID.randomUUID();
	private static final UUID CAVE = UUID.randomUUID();
	private static final UUID ALICE = UUID.randomUUID();
	private static final UUID BOB = UUID.randomUUID();

	@TempDir
	Path directory;
	ResultStore store;

	@BeforeEach
	void open() {
		store = ResultStore.open(directory.resolve("results.db"));
	}

	@AfterEach
	void close() {
		store.close();
	}

	void run(final UUID track, final UUID player, final int ticks, final long second) {
		store.recordRun(track, player, ticks, Instant.ofEpochSecond(second)).join();
	}

	@Test
	void theFirstRunIsARecordAndAPersonalBest() {
		// when
		final ResultStore.RunOutcome outcome = store.recordRun(TOWER, ALICE, 600, Instant.now()).join();

		// then
		assertThat(outcome.previousBest()).isEmpty();
		assertThat(outcome.previousRecord()).isEmpty();
		assertThat(outcome.completions()).isEqualTo(1);
		assertThat(outcome.isTrackRecord(600)).isTrue();
		assertThat(outcome.isPersonalBest(600)).isTrue();
	}

	@Test
	void laterRunsAreComparedWithTheBestBeforeThem() {
		// given
		run(TOWER, ALICE, 600, 1);
		run(TOWER, BOB, 500, 2);

		// when
		final ResultStore.RunOutcome slower = store.recordRun(TOWER, ALICE, 650, Instant.ofEpochSecond(3)).join();
		final ResultStore.RunOutcome better = store.recordRun(TOWER, ALICE, 550, Instant.ofEpochSecond(4)).join();

		// then
		assertThat(slower.previousBest()).hasValue(600);
		assertThat(slower.previousRecord()).hasValue(500);
		assertThat(slower.isPersonalBest(650)).isFalse();
		assertThat(better.isPersonalBest(550)).isTrue();
		assertThat(better.isTrackRecord(550)).isFalse();
		assertThat(better.completions()).isEqualTo(3);
	}

	@Test
	void playerResultHasTheBestTheCountAndTheLastCompletion() {
		// given
		run(TOWER, ALICE, 600, 10);
		run(TOWER, ALICE, 500, 20);
		run(TOWER, ALICE, 700, 30);
		run(CAVE, ALICE, 100, 40);

		// when
		final ResultStore.PlayerResult result = store.playerResult(TOWER, ALICE).join().orElseThrow();

		// then
		assertThat(result).isEqualTo(new ResultStore.PlayerResult(500, 3, Instant.ofEpochSecond(30)));
		assertThat(store.playerResult(TOWER, BOB).join()).isEmpty();
	}

	@Test
	void theRankingHasEveryPlayerOnceAndTheEarlierFirstAtEqualTimes() {
		// given
		run(TOWER, ALICE, 600, 1);
		run(TOWER, ALICE, 400, 5);
		run(TOWER, BOB, 400, 3);
		run(TOWER, UUID.randomUUID(), 900, 2);

		// when / then
		assertThat(store.ranked(TOWER, 1).join()).contains(new ResultStore.Ranked(BOB, 400));
		assertThat(store.ranked(TOWER, 2).join()).contains(new ResultStore.Ranked(ALICE, 400));
		assertThat(store.ranked(TOWER, 3).join()).map(ResultStore.Ranked::ticks).contains(900);
		assertThat(store.ranked(TOWER, 4).join()).isEmpty();
		assertThat(store.ranked(CAVE, 1).join()).isEmpty();
	}

	@Test
	void theSummaryHasEveryTrackThePlayerCompleted() {
		// given
		run(TOWER, ALICE, 600, 1);
		run(TOWER, ALICE, 500, 2);
		run(CAVE, ALICE, 100, 3);
		run(CAVE, BOB, 50, 4);

		// when / then
		assertThat(store.playerSummary(ALICE).join()).containsExactlyInAnyOrder(
				new ResultStore.TrackResult(TOWER, 500, 2), new ResultStore.TrackResult(CAVE, 100, 1));
		assertThat(store.playerSummary(UUID.randomUUID()).join()).isEmpty();
	}

	@Test
	void resultsSurviveReopening() {
		// given
		run(TOWER, ALICE, 600, 1);
		store.close();

		// when
		store = ResultStore.open(directory.resolve("results.db"));

		// then
		assertThat(store.playerResult(TOWER, ALICE).join()).isPresent();
	}

	@Test
	void aFileThatIsNoDatabaseCannotBeOpened() throws IOException {
		// given
		final Path file = directory.resolve("broken.db");
		Files.writeString(file, "this is not a database, it only pretends to be one for long enough");

		// when / then
		assertThatThrownBy(() -> ResultStore.open(file)).isInstanceOf(IllegalStateException.class)
				.hasMessageStartingWith("Could not open the results database");
	}
}
