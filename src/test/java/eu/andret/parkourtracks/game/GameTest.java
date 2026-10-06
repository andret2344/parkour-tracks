package eu.andret.parkourtracks.game;

import eu.andret.parkourtracks.helper.PluginTest;
import eu.andret.parkourtracks.track.Checkpoint;
import eu.andret.parkourtracks.track.Cuboid;
import eu.andret.parkourtracks.track.Spot;
import eu.andret.parkourtracks.track.Track;
import eu.andret.parkourtracks.track.WorldSpot;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * A running track along the x axis, at y 1 to 3 and z 1 to 3:
 * <pre>
 * spawn x 1-3 | checkpoint 1 x 8 | checkpoint 2 x 12 | finish x 17-19, a wall at y 0 under all of it
 * </pre>
 * The region is 0,0,0 - 20,20,20 in {@code world}; the lobby is at 100, 64, 100.
 */
abstract class GameTest extends PluginTest {
	protected Track track;

	@BeforeEach
	void setUpTrack() {
		// New players appear at the world spawn, which must not be on the track
		world.setSpawnLocation(50, 1, 2);
		track = plugin.getTrackRegistry().create("tower", "world", new Cuboid(0, 0, 0, 20, 20, 20));
		track.setSpawn(new Checkpoint(new Cuboid(1, 1, 1, 3, 3, 3), new Spot(2.5, 1, 2.5, 0, 0)));
		track.addCheckpoint(0, new Checkpoint(new Cuboid(8, 1, 1, 8, 3, 3), new Spot(8.5, 1, 2.5, 0, 0)));
		track.addCheckpoint(1, new Checkpoint(new Cuboid(12, 1, 1, 12, 3, 3), new Spot(12.5, 1, 2.5, 0, 0)));
		track.setFinish(new Checkpoint(new Cuboid(17, 1, 1, 19, 3, 3), new Spot(18.5, 1, 2.5, 0, 0)));
		track.addWall(new Cuboid(0, 0, 0, 20, 0, 20));
		track.setRunning(true);
		plugin.getTrackRegistry().setLobby(new WorldSpot("world", new Spot(100, 64, 100, 0, 0)));
		plugin.getTrackRegistry().save();
	}

	@NotNull
	protected GameManager games() {
		return plugin.getGames();
	}

	@NotNull
	protected Location at(final double x, final double y, final double z) {
		return new Location(world, x, y, z);
	}

	/**
	 * A survival player standing outside the track, at 50, 1, 2.
	 */
	@NotNull
	protected PlayerMock player() {
		final PlayerMock player = server.addPlayer();
		player.setGameMode(GameMode.SURVIVAL);
		player.teleport(at(50, 1, 2.5));
		return player;
	}

	/**
	 * A player who walked into the spawn and is on the track, the run not started.
	 */
	@NotNull
	protected PlayerMock onSpawn() {
		final PlayerMock player = player();
		player.teleport(at(-5, 1, 2.5));
		player.simulatePlayerMove(at(2.5, 1, 2.5));
		messages(player);
		return player;
	}

	/**
	 * A player whose run is going, standing at x 5, between the spawn and checkpoint 1.
	 */
	@NotNull
	protected PlayerMock running() {
		final PlayerMock player = onSpawn();
		player.simulatePlayerMove(at(5.5, 1, 2.5));
		return player;
	}

	@NotNull
	protected GameSession session(@NotNull final PlayerMock player) {
		return games().getSession(player).orElseThrow();
	}

	/**
	 * Moves the player step by step along x to the given x, one block per move.
	 */
	protected void walkTo(@NotNull final PlayerMock player, final double x) {
		double current = player.getLocation().getX();
		while (Math.abs(x - current) > 0.0001) {
			current += Math.max(-1, Math.min(1, x - current));
			player.simulatePlayerMove(at(current, 1, 2.5));
		}
	}
}
