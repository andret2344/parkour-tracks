package eu.andret.parkourtracks.command;

import eu.andret.parkourtracks.helper.PluginTest;
import eu.andret.parkourtracks.selection.SelectionException;
import eu.andret.parkourtracks.track.Checkpoint;
import eu.andret.parkourtracks.track.Cuboid;
import eu.andret.parkourtracks.track.MedalThreshold;
import eu.andret.parkourtracks.track.Spot;
import eu.andret.parkourtracks.track.Track;
import eu.andret.parkourtracks.track.TrackEffect;
import eu.andret.parkourtracks.track.WorldSpot;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TrackCommandTest extends PluginTest {
	void completeTower(final Track track) {
		track.setSpawn(new Checkpoint(new Cuboid(1, 1, 1, 2, 2, 2), new Spot(1.5, 1, 1.5, 0, 0)));
		track.setFinish(new Checkpoint(new Cuboid(18, 1, 18, 19, 2, 19), new Spot(18.5, 1, 18.5, 0, 0)));
		plugin.getTrackRegistry().setLobby(new WorldSpot("world", new Spot(100, 64, 100, 0, 0)));
	}

	@Test
	void createMakesStoppedTrackInTheSelectionAndSavesIt() {
		// given
		final PlayerMock admin = admin(0, 0, 0);
		select(admin, world, new Cuboid(0, 0, 0, 10, 10, 10));

		// when
		admin.performCommand("ptracks create tower");

		// then
		assertThat(messages(admin)).containsExactly("Created track tower.");
		final Track track = plugin.getTrackRegistry().find("tower").orElseThrow();
		assertThat(track.isRunning()).isFalse();
		assertThat(track.getWorld()).isEqualTo("world");
		assertThat(track.getRegion()).isEqualTo(new Cuboid(0, 0, 0, 10, 10, 10));
		plugin.getTrackRegistry().load();
		assertThat(plugin.getTrackRegistry().find("tower")).isPresent();
	}

	@Test
	void createRejectsInvalidAndTakenNames() {
		// given
		final PlayerMock admin = admin(0, 0, 0);
		tower();
		select(admin, world, new Cuboid(50, 0, 0, 60, 10, 10));

		// when
		admin.performCommand("ptracks create the-tower!");
		admin.performCommand("ptracks create TOWER");

		// then
		assertThat(messages(admin)).containsExactly(
				"A track name can have 1-32 letters, digits, \"_\" and \"-\", not \"the-tower!\".",
				"A track named TOWER already exists.");
		assertThat(plugin.getTrackRegistry().getTracks()).hasSize(1);
	}

	@Test
	void createNeedsACuboidSelection() {
		// given
		final PlayerMock admin = admin(0, 0, 0);
		final PlayerMock other = admin(0, 0, 0);
		plugin.setSelections(player -> {
			throw new SelectionException(
					player.equals(other)
							? SelectionException.Reason.NOT_CUBOID
							: SelectionException.Reason.INCOMPLETE);
		});

		// when
		admin.performCommand("ptracks create tower");
		other.performCommand("ptracks create tower");

		// then
		assertThat(messages(admin)).containsExactly("Select a region with WorldEdit first.");
		assertThat(messages(other)).containsExactly("Only cuboid selections are supported.");
		assertThat(plugin.getTrackRegistry().getTracks()).isEmpty();
	}

	@Test
	void createRejectsRegionOverlappingAnotherTrack() {
		// given
		final PlayerMock admin = admin(0, 0, 0);
		tower();
		select(admin, world, new Cuboid(20, 20, 20, 30, 30, 30));

		// when
		admin.performCommand("ptracks create cave");

		// then
		assertThat(messages(admin)).containsExactly("The region overlaps the region of track tower.");
		assertThat(plugin.getTrackRegistry().find("cave")).isEmpty();
	}

	@Test
	void editingNeedsThePermission() {
		// given
		final PlayerMock player = server.addPlayer();
		select(player, world, new Cuboid(0, 0, 0, 10, 10, 10));

		// when
		player.performCommand("ptracks create tower");

		// then
		assertThat(plugin.getTrackRegistry().getTracks()).isEmpty();
	}

	@Test
	void listShowsTracksWithTheirState() {
		// given
		final PlayerMock admin = admin(0, 0, 0);
		admin.performCommand("ptracks list");
		final Track tower = tower();
		tower.setDisplayName("<gold>The Tower");
		tower.setRunning(true);
		plugin.getTrackRegistry().create("cave", "world", new Cuboid(50, 0, 0, 60, 10, 10));

		// when
		admin.performCommand("ptracks list");

		// then
		assertThat(messages(admin)).containsExactly(
				"There are no tracks yet. Create one with /ptracks create.",
				"Tracks:",
				"- tower (The Tower), running",
				"- cave (cave), stopped");
	}

	@Test
	void infoListsWhatTheTrackMisses() {
		// given
		final PlayerMock admin = admin(0, 0, 0);
		tower();

		// when
		admin.performCommand("ptracks info tower");

		// then
		assertThat(messages(admin))
				.contains("Track tower (tower), stopped", "Type: server", "Region: world 0, 0, 0 to 20, 20, 20",
						"Spawn: not set", "Finish: not set", "Checkpoints between them: 0", "- difficulty: 1",
						"- skipMode: fail", "- Platinum: not set, reward 0.00", "Effects: none", "Authors: none",
						"Missing before it can start:", "- the spawn (/ptracks spawn)",
						"- the finish (/ptracks finish)", "- the lobby (/ptracks setlobby)")
				.doesNotContain("Ready to start.");
	}

	@Test
	void infoOfACompleteTrackSaysItIsReady() {
		// given
		final PlayerMock admin = admin(0, 0, 0);
		final Track track = tower();
		completeTower(track);
		track.setMedal("gold", new MedalThreshold(610, 25));
		track.setEffect(new TrackEffect("minecraft:speed", 1));
		track.setAuthors(List.of(admin.getUniqueId()));

		// when
		admin.performCommand("ptracks info tower");

		// then
		assertThat(messages(admin))
				.contains("Spawn: set", "Finish: set", "- Gold: 00:30.50, reward 25.00", "Effects: minecraft:speed 2",
						"Authors: " + admin.getName(), "Ready to start.")
				.doesNotContain("Missing before it can start:");
	}

	@Test
	void infoOfUnknownTrack() {
		// given
		final PlayerMock admin = admin(0, 0, 0);

		// when
		admin.performCommand("ptracks info nothing");

		// then
		assertThat(messages(admin)).containsExactly("There is no track named nothing.");
	}

	@Test
	void regionSetMovesTheRegion() {
		// given
		final PlayerMock admin = admin(0, 0, 0);
		final Track track = tower();
		select(admin, world, new Cuboid(-5, 0, -5, 25, 25, 25));

		// when
		admin.performCommand("ptracks region set tower");

		// then
		assertThat(messages(admin)).containsExactly("Set the region of track tower.");
		assertThat(track.getRegion()).isEqualTo(new Cuboid(-5, 0, -5, 25, 25, 25));
	}

	@Test
	void regionSetRejectsRegionLeavingPartsOutOrInAnotherWorld() {
		// given
		final PlayerMock admin = admin(0, 0, 0);
		final Track track = tower();
		completeTower(track);
		final World nether = server.addSimpleWorld("nether");

		// when
		select(admin, world, new Cuboid(0, 0, 0, 10, 10, 10));
		admin.performCommand("ptracks region set tower");
		select(admin, nether, new Cuboid(0, 0, 0, 20, 20, 20));
		admin.performCommand("ptracks region set tower");

		// then
		assertThat(messages(admin)).containsExactly(
				"The region has to contain the spawn, the finish, the checkpoints and the walls of tower.",
				"The region has to contain the spawn, the finish, the checkpoints and the walls of tower.");
		assertThat(track.getRegion()).isEqualTo(new Cuboid(0, 0, 0, 20, 20, 20));
		assertThat(track.getWorld()).isEqualTo("world");
	}

	@Test
	void regionSetRejectsOverlap() {
		// given
		final PlayerMock admin = admin(0, 0, 0);
		final Track track = tower();
		plugin.getTrackRegistry().create("cave", "world", new Cuboid(50, 0, 0, 60, 10, 10));
		select(admin, world, new Cuboid(0, 0, 0, 55, 20, 20));

		// when
		admin.performCommand("ptracks region set tower");

		// then
		assertThat(messages(admin)).containsExactly("The region overlaps the region of track cave.");
		assertThat(track.getRegion()).isEqualTo(new Cuboid(0, 0, 0, 20, 20, 20));
	}

	@Test
	void editLockRejectsChangesOfARunningTrack() {
		// given
		final PlayerMock admin = admin(0, 0, 0);
		final Track track = tower();
		track.setRunning(true);
		select(admin, world, new Cuboid(-5, 0, -5, 25, 25, 25));

		// when
		admin.performCommand("ptracks region set tower");
		admin.performCommand("ptracks rename tower spire");
		admin.performCommand("ptracks remove tower");
		admin.performCommand("ptracks track lobby clear tower");

		// then
		assertThat(messages(admin)).hasSize(4)
				.allMatch("Track tower is running. Stop it before changing it."::equals);
		assertThat(track.getRegion()).isEqualTo(new Cuboid(0, 0, 0, 20, 20, 20));
		assertThat(track.getName()).isEqualTo("tower");
		assertThat(plugin.getTrackRegistry().getTracks()).contains(track);
	}

	@Test
	void renameKeepsTheTrack() {
		// given
		final PlayerMock admin = admin(0, 0, 0);
		final Track track = tower();
		final UUID id = track.getId();

		// when
		admin.performCommand("ptracks rename tower spire");

		// then
		assertThat(messages(admin)).containsExactly("Renamed track tower to spire.");
		assertThat(plugin.getTrackRegistry().find("spire")).map(Track::getId).contains(id);
	}

	@Test
	void renameRejectsInvalidAndTakenNames() {
		// given
		final PlayerMock admin = admin(0, 0, 0);
		tower();
		plugin.getTrackRegistry().create("cave", "world", new Cuboid(50, 0, 0, 60, 10, 10));

		// when
		admin.performCommand("ptracks rename tower cave");
		admin.performCommand("ptracks rename tower sp!re");
		admin.performCommand("ptracks rename tower Tower");

		// then
		assertThat(messages(admin)).containsExactly(
				"A track named cave already exists.",
				"A track name can have 1-32 letters, digits, \"_\" and \"-\", not \"sp!re\".",
				"Renamed track tower to Tower.");
	}

	@Test
	void removeDropsAStoppedTrack() {
		// given
		final PlayerMock admin = admin(0, 0, 0);
		tower();

		// when
		admin.performCommand("ptracks remove tower");

		// then
		assertThat(messages(admin)).containsExactly("Removed track tower. Its results are kept.");
		assertThat(plugin.getTrackRegistry().getTracks()).isEmpty();
	}

	@Test
	void startNeedsSpawnFinishAndLobby() {
		// given
		final PlayerMock admin = admin(0, 0, 0);
		final Track track = tower();

		// when
		admin.performCommand("ptracks start tower");

		// then
		assertThat(messages(admin)).containsExactly("Track tower cannot start yet, it misses: the spawn "
				+ "(/ptracks spawn), the finish (/ptracks finish), the lobby (/ptracks setlobby).");
		assertThat(track.isRunning()).isFalse();
	}

	@Test
	void startAndStop() {
		// given
		final PlayerMock admin = admin(0, 0, 0);
		final Track track = tower();
		completeTower(track);

		// when
		admin.performCommand("ptracks start tower");
		admin.performCommand("ptracks start tower");
		final boolean running = track.isRunning();
		admin.performCommand("ptracks stop tower");
		admin.performCommand("ptracks stop tower");

		// then
		assertThat(running).isTrue();
		assertThat(track.isRunning()).isFalse();
		assertThat(messages(admin)).containsExactly("Started track tower.", "Track tower is already running.",
				"Stopped track tower.", "Track tower is not running.");
	}

	@Test
	void setLobbyRejectsSpotsInsideTracks() {
		// given
		final PlayerMock admin = admin(5, 5, 5);
		tower();

		// when
		admin.performCommand("ptracks setlobby");
		admin.teleport(new Location(world, 50, 64, 50, 90, 0));
		admin.performCommand("ptracks setlobby");

		// then
		assertThat(messages(admin)).containsExactly(
				"The lobby cannot lie inside track tower: players sent there would enter it.", "Set the lobby.");
		assertThat(plugin.getTrackRegistry().getLobby()).isEqualTo(new WorldSpot("world", new Spot(50, 64, 50, 90, 0)));
	}

	@Test
	void trackLobbyCanBeSetAndCleared() {
		// given
		final PlayerMock admin = admin(50, 64, 50);
		final Track track = tower();

		// when
		admin.performCommand("ptracks track lobby set tower");
		final WorldSpot lobby = track.getLobby();
		admin.performCommand("ptracks track lobby clear tower");

		// then
		assertThat(lobby).isEqualTo(new WorldSpot("world", new Spot(50, 64, 50, 0, 0)));
		assertThat(track.getLobby()).isNull();
		assertThat(messages(admin)).containsExactly("Set the own lobby of track tower.",
				"Track tower uses the global lobby again.");
	}

	@Test
	void reloadReadsTheFilesAgain() throws IOException {
		// given
		final PlayerMock admin = admin(0, 0, 0);
		writeMessages("reloaded: 'Done!'\n");

		// when
		admin.performCommand("ptracks reload");

		// then
		assertThat(messages(admin)).containsExactly("Done!");
	}

	@Test
	void reloadOfInvalidFilesChangesNothing() throws IOException {
		// given
		final PlayerMock admin = admin(0, 0, 0);
		writeMessages("reloaded: 'Done!'\n");
		writeConfig("medals: [");

		// when
		admin.performCommand("ptracks reload");
		admin.performCommand("ptracks reload");

		// then: the messages were not replaced either, though their file is valid
		assertThat(messages(admin)).hasSize(2)
				.allMatch(message -> message.startsWith("The files are invalid and were not reloaded: Could not read config.yml"));
	}

	@Test
	void helpListsTheCommands() {
		// given
		final PlayerMock admin = admin(0, 0, 0);

		// when
		admin.performCommand("ptracks");

		// then
		assertThat(admin.nextMessage()).startsWith("/");
	}
}
