package eu.andret.parkourtracks.helper;

import eu.andret.parkourtracks.ParkourTracksPlugin;
import eu.andret.parkourtracks.track.Checkpoint;
import eu.andret.parkourtracks.track.Cuboid;
import eu.andret.parkourtracks.track.Spot;
import eu.andret.parkourtracks.track.Track;
import eu.andret.parkourtracks.track.WorldSpot;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

/**
 * Tracks ready to play, for tests.
 */
public final class TestTracks {
	private TestTracks() {
	}

	/**
	 * A running track named {@code tower} along the x axis, at y 1 to 3 and z 1 to 3:
	 * <pre>
	 * spawn x 1-3 | checkpoint 1 x 8 | checkpoint 2 x 12 | finish x 17-19, a wall at y 0 under all of it
	 * </pre>
	 * The region is 0,0,0 - 20,20,20; the global lobby is set to 100, 64, 100. The world spawn is moved off the
	 * track, as new players appear there.
	 */
	@NotNull
	public static Track tower(@NotNull final ParkourTracksPlugin plugin, @NotNull final World world) {
		world.setSpawnLocation(50, 1, 2);
		final Track track = plugin.getTrackRegistry().create("tower", world.getName(), new Cuboid(0, 0, 0, 20, 20, 20));
		track.setSpawn(new Checkpoint(new Cuboid(1, 1, 1, 3, 3, 3), new Spot(2.5, 1, 2.5, 0, 0)));
		track.addCheckpoint(0, new Checkpoint(new Cuboid(8, 1, 1, 8, 3, 3), new Spot(8.5, 1, 2.5, 0, 0)));
		track.addCheckpoint(1, new Checkpoint(new Cuboid(12, 1, 1, 12, 3, 3), new Spot(12.5, 1, 2.5, 0, 0)));
		track.setFinish(new Checkpoint(new Cuboid(17, 1, 1, 19, 3, 3), new Spot(18.5, 1, 2.5, 0, 0)));
		track.addWall(new Cuboid(0, 0, 0, 20, 0, 20));
		track.setRunning(true);
		plugin.getTrackRegistry().setLobby(new WorldSpot(world.getName(), new Spot(100, 64, 100, 0, 0)));
		plugin.getTrackRegistry().save();
		return track;
	}

	/**
	 * A small running track with its spawn at the low corner and its finish at the high one, 0 to 5 from x.
	 */
	@NotNull
	public static Track small(@NotNull final ParkourTracksPlugin plugin, @NotNull final World world,
							  @NotNull final String name, final int x) {
		final Track track = plugin.getTrackRegistry().create(name, world.getName(), new Cuboid(x, 0, 0, x + 5, 5, 5));
		track.setSpawn(new Checkpoint(new Cuboid(x + 1, 1, 1, x + 1, 2, 2), new Spot(x + 1.5, 1, 1.5, 0, 0)));
		track.setFinish(new Checkpoint(new Cuboid(x + 4, 1, 1, x + 4, 2, 2), new Spot(x + 4.5, 1, 1.5, 0, 0)));
		track.setRunning(true);
		return track;
	}
}
