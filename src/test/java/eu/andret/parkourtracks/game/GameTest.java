package eu.andret.parkourtracks.game;

import eu.andret.parkourtracks.helper.PluginTest;
import eu.andret.parkourtracks.helper.TestTracks;
import eu.andret.parkourtracks.track.Track;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * Games on {@link TestTracks#tower}.
 */
abstract class GameTest extends PluginTest {
	protected Track track;

	@BeforeEach
	void setUpTrack() {
		track = TestTracks.tower(plugin, world);
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
