package eu.andret.parkourtracks.track;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TrackStoreTest {
	@TempDir
	Path directory;

	@Test
	void loadOfMissingFileIsEmpty() {
		// given
		final TrackStore store = new TrackStore(directory.resolve("tracks.json"));

		// when
		final TrackStore.Content content = store.load();

		// then
		assertThat(content.lobby()).isNull();
		assertThat(content.tracks()).isEmpty();
	}

	@Test
	void saveAndLoadKeepEverything() {
		// given
		final TrackStore store = new TrackStore(directory.resolve("tracks.json"));
		final UUID author = UUID.randomUUID();
		final Track track = new Track(UUID.randomUUID(), "tower", "world", new Cuboid(0, 0, 0, 20, 20, 20));
		track.setDisplayName("<gold>The Tower");
		track.setType(TrackType.PLAYERS);
		track.setSpawn(new Checkpoint(new Cuboid(1, 1, 1, 2, 2, 2), new Spot(1.5, 1, 1.5, 90, 10)));
		track.setFinish(new Checkpoint(new Cuboid(18, 18, 18, 19, 19, 19), new Spot(18.5, 18, 18.5, 0, 0)));
		track.addCheckpoint(0, new Checkpoint(new Cuboid(5, 5, 5, 6, 6, 6), new Spot(5.5, 5, 5.5, 180, 0)));
		track.addWall(new Cuboid(0, 0, 0, 20, 0, 20));
		track.setAuthors(List.of(author));
		track.setMedal("gold", new MedalThreshold(600, 100));
		track.setEffect(new TrackEffect("minecraft:jump_boost", 1));
		track.setLobby(new WorldSpot("world", new Spot(100, 64, 100, 0, 0)));
		track.setRunning(true);
		track.getOptions().setBoat(true);
		track.getOptions().setBoatType(EntityType.BIRCH_BOAT);
		track.getOptions().setSkipMode(SkipMode.NOTIFY);
		track.getOptions().setFee(25);
		track.getOptions().setDifficulty(4);
		track.getOptions().setIcon(Material.LADDER);
		track.getOptions().setPermission("vip.tracks");
		track.getOptions().setAfterFinish(AfterFinish.SPAWN);
		final WorldSpot lobby = new WorldSpot("lobby", new Spot(0, 70, 0, 45, 0));

		// when
		store.save(new TrackStore.Content(lobby, List.of(track)));
		final TrackStore.Content content = store.load();

		// then
		assertThat(content.lobby()).isEqualTo(lobby);
		assertThat(content.tracks()).hasSize(1);
		final Track loaded = content.tracks().getFirst();
		assertThat(loaded).usingRecursiveComparison().isEqualTo(track);
	}

	@Test
	void saveLeavesNoTemporaryFile() throws IOException {
		// given
		final TrackStore store = new TrackStore(directory.resolve("tracks.json"));

		// when
		store.save(new TrackStore.Content(null, List.of()));

		// then
		try (final Stream<Path> files = Files.list(directory)) {
			assertThat(files.map(path -> path.getFileName().toString())).containsExactly("tracks.json");
		}
	}

	@Test
	void loadFillsInMissingFieldsWithDefaults() throws IOException {
		// given
		final Path file = directory.resolve("tracks.json");
		Files.writeString(file, """
				{
					"tracks": [
						{
							"id": "6f1c3f6e-2f4a-4a8e-9c1b-1d2e3f4a5b6c",
							"name": "tower",
							"world": "world",
							"region": {"minX": 0, "minY": 0, "minZ": 0, "maxX": 5, "maxY": 5, "maxZ": 5}
						}
					]
				}
				""");

		// when
		final Track track = new TrackStore(file).load().tracks().getFirst();

		// then
		assertThat(track.getDisplayName()).isEqualTo("tower");
		assertThat(track.getType()).isEqualTo(TrackType.SERVER);
		assertThat(track.getCheckpoints()).isEmpty();
		assertThat(track.getMedals()).isEmpty();
		assertThat(track.getOptions().getSkipMode()).isEqualTo(SkipMode.FAIL);
		assertThat(track.getOptions().getDifficulty()).isEqualTo(1);
		assertThat(track.getOptions().getBoatType()).isEqualTo(EntityType.OAK_BOAT);
	}

	@Test
	void loadRejectsInvalidJson() throws IOException {
		// given
		final Path file = directory.resolve("tracks.json");
		Files.writeString(file, "{ not json");

		// when / then
		assertThatThrownBy(() -> new TrackStore(file).load())
				.isInstanceOf(IllegalStateException.class)
				.hasMessageStartingWith("Could not read tracks.json");
	}

	@Test
	void loadRejectsEmptyFile() throws IOException {
		// given
		final Path file = directory.resolve("tracks.json");
		Files.writeString(file, "");

		// when / then
		assertThatThrownBy(() -> new TrackStore(file).load())
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("empty");
	}

	@Test
	void loadRejectsTrackWithoutRegion() throws IOException {
		// given
		final Path file = directory.resolve("tracks.json");
		Files.writeString(file, """
				{"tracks": [{"id": "6f1c3f6e-2f4a-4a8e-9c1b-1d2e3f4a5b6c", "name": "tower", "world": "world"}]}
				""");

		// when / then
		assertThatThrownBy(() -> new TrackStore(file).load())
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("region");
	}

	@Test
	void loadRejectsInvalidName() throws IOException {
		// given
		final Path file = directory.resolve("tracks.json");
		Files.writeString(file, """
				{"tracks": [{"id": "6f1c3f6e-2f4a-4a8e-9c1b-1d2e3f4a5b6c", "name": "the tower", "world": "world",
				"region": {"minX": 0, "minY": 0, "minZ": 0, "maxX": 5, "maxY": 5, "maxZ": 5}}]}
				""");

		// when / then
		assertThatThrownBy(() -> new TrackStore(file).load())
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("the tower");
	}

	@Test
	void loadRejectsInvalidRegion() throws IOException {
		// given
		final Path file = directory.resolve("tracks.json");
		Files.writeString(file, """
				{"tracks": [{"id": "6f1c3f6e-2f4a-4a8e-9c1b-1d2e3f4a5b6c", "name": "tower", "world": "world",
				"region": {"minX": 9, "minY": 0, "minZ": 0, "maxX": 5, "maxY": 5, "maxZ": 5}}]}
				""");

		// when / then
		assertThatThrownBy(() -> new TrackStore(file).load())
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("minimum corner");
	}

	@Test
	void saveFailsWhenTheFileCannotBeWritten() throws IOException {
		// given
		final Path file = directory.resolve("tracks.json");
		// A directory in the way of the temporary file
		Files.createDirectory(directory.resolve("tracks.json.tmp"));

		// when / then
		assertThatThrownBy(() -> new TrackStore(file).save(new TrackStore.Content(null, List.of())))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageStartingWith("Could not write tracks.json");
	}
}
