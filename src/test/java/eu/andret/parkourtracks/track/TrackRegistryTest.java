package eu.andret.parkourtracks.track;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TrackRegistryTest {
	@TempDir
	Path directory;

	TrackRegistry newRegistry() {
		return new TrackRegistry(new TrackStore(directory.resolve("tracks.json")));
	}

	@Test
	void createAddsStoppedTrack() {
		// given
		final TrackRegistry registry = newRegistry();

		// when
		final Track track = registry.create("tower", "world", new Cuboid(0, 0, 0, 10, 10, 10));

		// then
		assertThat(track.isRunning()).isFalse();
		assertThat(registry.getTracks()).containsExactly(track);
	}

	@Test
	void createRejectsTakenNameIgnoringCase() {
		// given
		final TrackRegistry registry = newRegistry();
		registry.create("tower", "world", new Cuboid(0, 0, 0, 10, 10, 10));

		// when / then
		assertThatThrownBy(() -> registry.create("Tower", "world", new Cuboid(50, 0, 0, 60, 10, 10)))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void createRejectsInvalidName() {
		// given
		final TrackRegistry registry = newRegistry();

		// when / then
		assertThatThrownBy(() -> registry.create("the tower", "world", new Cuboid(0, 0, 0, 10, 10, 10)))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> registry.create("a".repeat(33), "world", new Cuboid(0, 0, 0, 10, 10, 10)))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void createRejectsRegionOverlappingAnotherTrack() {
		// given
		final TrackRegistry registry = newRegistry();
		registry.create("tower", "world", new Cuboid(0, 0, 0, 10, 10, 10));

		// when / then
		assertThatThrownBy(() -> registry.create("cave", "world", new Cuboid(10, 10, 10, 20, 20, 20)))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void createAllowsSameCoordinatesInAnotherWorld() {
		// given
		final TrackRegistry registry = newRegistry();
		registry.create("tower", "world", new Cuboid(0, 0, 0, 10, 10, 10));

		// when
		final Track track = registry.create("cave", "nether", new Cuboid(0, 0, 0, 10, 10, 10));

		// then
		assertThat(registry.getTracks()).contains(track);
	}

	@Test
	void findOverlappingSkipsTheGivenTrack() {
		// given
		final TrackRegistry registry = newRegistry();
		final Track track = registry.create("tower", "world", new Cuboid(0, 0, 0, 10, 10, 10));

		// when / then
		assertThat(registry.findOverlapping("world", new Cuboid(0, 0, 0, 12, 12, 12), track)).isEmpty();
		assertThat(registry.findOverlapping("world", new Cuboid(0, 0, 0, 12, 12, 12), null)).contains(track);
	}

	@Test
	void findByNameIgnoresCaseAndFindById() {
		// given
		final TrackRegistry registry = newRegistry();
		final Track track = registry.create("Tower", "world", new Cuboid(0, 0, 0, 10, 10, 10));

		// when / then
		assertThat(registry.find("tower")).contains(track);
		assertThat(registry.find("TOWER")).contains(track);
		assertThat(registry.find("cave")).isEmpty();
		assertThat(registry.find(track.getId())).contains(track);
	}

	@Test
	void removeRejectsRunningTrack() {
		// given
		final TrackRegistry registry = newRegistry();
		final Track track = registry.create("tower", "world", new Cuboid(0, 0, 0, 10, 10, 10));
		track.setRunning(true);

		// when / then
		assertThatThrownBy(() -> registry.remove(track)).isInstanceOf(IllegalStateException.class);
		assertThat(registry.getTracks()).contains(track);
	}

	@Test
	void removeDropsStoppedTrack() {
		// given
		final TrackRegistry registry = newRegistry();
		final Track track = registry.create("tower", "world", new Cuboid(0, 0, 0, 10, 10, 10));

		// when
		registry.remove(track);

		// then
		assertThat(registry.getTracks()).isEmpty();
	}

	@Test
	void renameRejectsNameOfAnotherTrack() {
		// given
		final TrackRegistry registry = newRegistry();
		final Track track = registry.create("tower", "world", new Cuboid(0, 0, 0, 10, 10, 10));
		registry.create("cave", "world", new Cuboid(50, 0, 0, 60, 10, 10));

		// when / then
		assertThatThrownBy(() -> registry.rename(track, "CAVE")).isInstanceOf(IllegalArgumentException.class);
		assertThat(track.getName()).isEqualTo("tower");
	}

	@Test
	void renameKeepsTheIdAndAllowsChangingCase() {
		// given
		final TrackRegistry registry = newRegistry();
		final Track track = registry.create("tower", "world", new Cuboid(0, 0, 0, 10, 10, 10));

		// when
		registry.rename(track, "Tower");

		// then
		assertThat(track.getName()).isEqualTo("Tower");
		assertThat(registry.find(track.getId())).contains(track);
	}

	@Test
	void saveAndLoadKeepTracksAndLobby() {
		// given
		final TrackRegistry registry = newRegistry();
		final Track track = registry.create("tower", "world", new Cuboid(0, 0, 0, 10, 10, 10));
		final WorldSpot lobby = new WorldSpot("world", new Spot(0, 64, 0, 0, 0));
		registry.setLobby(lobby);

		// when
		registry.save();
		final TrackRegistry loaded = newRegistry();
		loaded.load();

		// then
		assertThat(loaded.getLobby()).isEqualTo(lobby);
		assertThat(loaded.getTracks()).containsExactly(track);
	}

	@Test
	void loadRejectsTracksSharingAName() throws IOException {
		// given
		Files.writeString(directory.resolve("tracks.json"), """
				{"tracks": [
					{"id": "6f1c3f6e-2f4a-4a8e-9c1b-1d2e3f4a5b6c", "name": "tower", "world": "world",
					"region": {"minX": 0, "minY": 0, "minZ": 0, "maxX": 5, "maxY": 5, "maxZ": 5}},
					{"id": "7f1c3f6e-2f4a-4a8e-9c1b-1d2e3f4a5b6c", "name": "TOWER", "world": "world",
					"region": {"minX": 10, "minY": 0, "minZ": 0, "maxX": 15, "maxY": 5, "maxZ": 5}}
				]}
				""");
		final TrackRegistry registry = newRegistry();

		// when / then
		assertThatThrownBy(registry::load).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void loadRejectsTracksSharingAnId() throws IOException {
		// given
		Files.writeString(directory.resolve("tracks.json"), """
				{"tracks": [
					{"id": "6f1c3f6e-2f4a-4a8e-9c1b-1d2e3f4a5b6c", "name": "tower", "world": "world",
					"region": {"minX": 0, "minY": 0, "minZ": 0, "maxX": 5, "maxY": 5, "maxZ": 5}},
					{"id": "6f1c3f6e-2f4a-4a8e-9c1b-1d2e3f4a5b6c", "name": "cave", "world": "world",
					"region": {"minX": 10, "minY": 0, "minZ": 0, "maxX": 15, "maxY": 5, "maxZ": 5}}
				]}
				""");
		final TrackRegistry registry = newRegistry();

		// when / then
		assertThatThrownBy(registry::load).isInstanceOf(IllegalStateException.class);
	}
}
