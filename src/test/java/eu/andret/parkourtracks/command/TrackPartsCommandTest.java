package eu.andret.parkourtracks.command;

import eu.andret.parkourtracks.helper.PluginTest;
import eu.andret.parkourtracks.track.Checkpoint;
import eu.andret.parkourtracks.track.Cuboid;
import eu.andret.parkourtracks.track.Spot;
import eu.andret.parkourtracks.track.Track;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.assertj.core.api.Assertions.assertThat;

class TrackPartsCommandTest extends PluginTest {
	@Test
	void spawnIsTheSelectionWithTheSpotWhereTheAdminStands() {
		// given
		final PlayerMock admin = admin(1.5, 1, 1.5);
		final Track track = tower();
		select(admin, world, new Cuboid(1, 1, 1, 2, 2, 2));

		// when
		admin.performCommand("ptracks spawn tower");

		// then
		assertThat(messages(admin)).containsExactly("Set the spawn of track tower.");
		assertThat(track.getSpawn()).isEqualTo(new Checkpoint(new Cuboid(1, 1, 1, 2, 2, 2), new Spot(1.5, 1, 1.5, 0, 0)));
	}

	@Test
	void finishIsSetTheSameWay() {
		// given
		final PlayerMock admin = admin(18.5, 1, 18.5);
		final Track track = tower();
		select(admin, world, new Cuboid(18, 1, 18, 19, 2, 19));

		// when
		admin.performCommand("ptracks finish tower");

		// then
		assertThat(messages(admin)).containsExactly("Set the finish of track tower.");
		assertThat(track.getFinish()).isNotNull();
	}

	@Test
	void partsMustLieInTheTrackWorldInsideItsRegionWithTheAdminInsideTheArea() {
		// given
		final PlayerMock admin = admin(1.5, 1, 1.5);
		final Track track = tower();
		final World nether = server.addSimpleWorld("nether");

		// when
		select(admin, nether, new Cuboid(1, 1, 1, 2, 2, 2));
		admin.performCommand("ptracks spawn tower");
		select(admin, world, new Cuboid(15, 15, 15, 25, 25, 25));
		admin.performCommand("ptracks spawn tower");
		select(admin, world, new Cuboid(5, 5, 5, 6, 6, 6));
		admin.performCommand("ptracks spawn tower");
		select(admin, world, new Cuboid(1, 1, 1, 2, 2, 2));
		admin.teleport(new Location(nether, 1.5, 1, 1.5));
		admin.performCommand("ptracks spawn tower");

		// then
		assertThat(messages(admin)).containsExactly(
				"Select it, and stand, in the world of the track, world.",
				"The selection has to lie wholly inside the region of track tower.",
				"Stand inside the selection: players coming back here land where you stand.",
				"Select it, and stand, in the world of the track, world.");
		assertThat(track.getSpawn()).isNull();
	}

	@Test
	void checkpointsAreAddedAtTheEndOrAtAPositionWithoutTouchingTheFinish() {
		// given
		final PlayerMock admin = admin(0, 0, 0);
		final Track track = tower();
		final Checkpoint finish = new Checkpoint(new Cuboid(18, 1, 18, 19, 2, 19), new Spot(18.5, 1, 18.5, 0, 0));
		track.setFinish(finish);

		// when
		addCheckpointAt(admin, 5, "ptracks checkpoint add tower");
		addCheckpointAt(admin, 10, "ptracks checkpoint add tower");
		addCheckpointAt(admin, 7, "ptracks checkpoint add tower 2");

		// then
		assertThat(messages(admin)).containsExactly("Added checkpoint 1 to track tower.",
				"Added checkpoint 2 to track tower.", "Added checkpoint 2 to track tower.");
		assertThat(track.getCheckpoints()).extracting(checkpoint -> checkpoint.area().minX()).containsExactly(5, 7, 10);
		assertThat(track.getFinish()).isEqualTo(finish);
	}

	@Test
	void checkpointsCanBeReplacedAndRemoved() {
		// given
		final PlayerMock admin = admin(0, 0, 0);
		final Track track = tower();
		addCheckpointAt(admin, 5, "ptracks checkpoint add tower");
		addCheckpointAt(admin, 10, "ptracks checkpoint add tower");
		messages(admin);

		// when
		addCheckpointAt(admin, 12, "ptracks checkpoint set tower 2");
		admin.performCommand("ptracks checkpoint remove tower 1");

		// then
		assertThat(messages(admin)).containsExactly("Set checkpoint 2 of track tower.",
				"Removed checkpoint 1 of track tower.");
		assertThat(track.getCheckpoints()).extracting(checkpoint -> checkpoint.area().minX()).containsExactly(12);
	}

	@Test
	void checkpointPositionsAreChecked() {
		// given
		final PlayerMock admin = admin(0, 0, 0);
		final Track track = tower();
		addCheckpointAt(admin, 5, "ptracks checkpoint add tower");
		messages(admin);

		// when
		addCheckpointAt(admin, 6, "ptracks checkpoint add tower 3");
		addCheckpointAt(admin, 6, "ptracks checkpoint set tower 0");
		admin.performCommand("ptracks checkpoint remove tower 2");

		// then
		assertThat(messages(admin)).containsExactly("The position has to be between 1 and 2.",
				"The position has to be between 1 and 1.", "The position has to be between 1 and 1.");
		assertThat(track.getCheckpoints()).hasSize(1);
	}

	@Test
	void wallsAreAddedReplacedAndRemoved() {
		// given
		final PlayerMock admin = admin(0, 0, 0);
		final Track track = tower();

		// when
		select(admin, world, new Cuboid(0, 0, 0, 20, 0, 20));
		admin.performCommand("ptracks wall add tower");
		select(admin, world, new Cuboid(0, 5, 0, 20, 5, 20));
		admin.performCommand("ptracks wall add tower");
		select(admin, world, new Cuboid(0, 1, 0, 20, 1, 20));
		admin.performCommand("ptracks wall set tower 1");
		admin.performCommand("ptracks wall remove tower 2");

		// then
		assertThat(messages(admin)).containsExactly("Added wall 1 to track tower.", "Added wall 2 to track tower.",
				"Set wall 1 of track tower.", "Removed wall 2 of track tower.");
		assertThat(track.getWalls()).containsExactly(new Cuboid(0, 1, 0, 20, 1, 20));
	}

	@Test
	void wallsMustLieInsideTheRegion() {
		// given
		final PlayerMock admin = admin(0, 0, 0);
		final Track track = tower();
		select(admin, world, new Cuboid(0, -1, 0, 20, 0, 20));

		// when
		admin.performCommand("ptracks wall add tower");

		// then
		assertThat(messages(admin)).containsExactly("The selection has to lie wholly inside the region of track tower.");
		assertThat(track.getWalls()).isEmpty();
	}

	@Test
	void editLockRejectsChangesOfARunningTrack() {
		// given
		final PlayerMock admin = admin(1.5, 1, 1.5);
		final Track track = tower();
		track.setRunning(true);
		select(admin, world, new Cuboid(1, 1, 1, 2, 2, 2));

		// when
		admin.performCommand("ptracks spawn tower");
		admin.performCommand("ptracks checkpoint add tower");
		admin.performCommand("ptracks wall add tower");

		// then
		assertThat(messages(admin)).hasSize(3).allMatch("Track tower is running. Stop it before changing it."::equals);
		assertThat(track.getSpawn()).isNull();
		assertThat(track.getCheckpoints()).isEmpty();
		assertThat(track.getWalls()).isEmpty();
	}

	/**
	 * Selects a one-block-wide checkpoint at x, stands in it and runs the command.
	 */
	void addCheckpointAt(final PlayerMock admin, final int x, final String command) {
		select(admin, world, new Cuboid(x, 1, 1, x, 2, 2));
		admin.teleport(new Location(world, x + 0.5, 1, 1.5));
		admin.performCommand(command);
	}
}
