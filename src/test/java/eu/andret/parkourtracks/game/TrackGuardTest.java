package eu.andret.parkourtracks.game;

import org.bukkit.ExplosionResult;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Creeper;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TrackGuardTest extends GameTest {
	@Test
	void nobodyBreaksBlocksOfARunningTrackNotEvenAnOperator() {
		// given
		final PlayerMock admin = admin(5, 1, 25);
		final Block inside = world.getBlockAt(5, 0, 2);
		final Block outside = world.getBlockAt(5, 0, 25);
		final BlockBreakEvent breakInside = new BlockBreakEvent(inside, admin);
		final BlockBreakEvent breakOutside = new BlockBreakEvent(outside, admin);

		// when
		server.getPluginManager().callEvent(breakInside);
		server.getPluginManager().callEvent(breakOutside);

		// then
		assertThat(breakInside.isCancelled()).isTrue();
		assertThat(breakOutside.isCancelled()).isFalse();
	}

	@Test
	void aStoppedTrackCanBeChanged() {
		// given
		track.setRunning(false);
		final BlockBreakEvent event = new BlockBreakEvent(world.getBlockAt(5, 0, 2), admin(5, 1, 25));

		// when
		server.getPluginManager().callEvent(event);

		// then
		assertThat(event.isCancelled()).isFalse();
	}

	@Test
	void sameCoordinatesInAnotherWorldAreNotLocked() {
		// given
		final Block block = server.addSimpleWorld("nether").getBlockAt(5, 0, 2);
		final BlockBreakEvent event = new BlockBreakEvent(block, admin(5, 1, 25));

		// when
		server.getPluginManager().callEvent(event);

		// then
		assertThat(event.isCancelled()).isFalse();
	}

	@Test
	void explosionsLeaveTheTrackAlone() {
		// given
		final Creeper creeper = world.spawn(at(21, 1, 2), Creeper.class);
		final List<Block> blocks = new ArrayList<>(List.of(world.getBlockAt(20, 1, 2), world.getBlockAt(21, 1, 2)));
		final EntityExplodeEvent event = new EntityExplodeEvent(creeper, at(21, 1, 2), blocks, 1, ExplosionResult.DESTROY);

		// when
		server.getPluginManager().callEvent(event);

		// then
		assertThat(event.blockList()).containsExactly(world.getBlockAt(21, 1, 2));
	}

	@Test
	void liquidsDoNotFlowIn() {
		// given
		final BlockFromToEvent into = new BlockFromToEvent(world.getBlockAt(21, 1, 2), world.getBlockAt(20, 1, 2));
		final BlockFromToEvent away = new BlockFromToEvent(world.getBlockAt(21, 1, 2), world.getBlockAt(22, 1, 2));

		// when
		server.getPluginManager().callEvent(into);
		server.getPluginManager().callEvent(away);

		// then
		assertThat(into.isCancelled()).isTrue();
		assertThat(away.isCancelled()).isFalse();
	}

	@Test
	void iceDoesNotMelt() {
		// given
		final Block ice = world.getBlockAt(5, 0, 2);
		ice.setType(Material.ICE);
		final BlockFadeEvent event = new BlockFadeEvent(ice, ice.getState());

		// when
		server.getPluginManager().callEvent(event);

		// then
		assertThat(event.isCancelled()).isTrue();
	}

	@Test
	void pistonsDoNotPushIntoOrPullOutOfTheTrack() {
		// given: pistons outside the region next to its edge at x 20
		final Block pushing = world.getBlockAt(22, 1, 2);
		final BlockPistonExtendEvent push = new BlockPistonExtendEvent(pushing,
				List.of(world.getBlockAt(21, 1, 2)), BlockFace.WEST);
		final BlockPistonExtendEvent pushAway = new BlockPistonExtendEvent(pushing,
				List.of(world.getBlockAt(23, 1, 2)), BlockFace.EAST);
		final BlockPistonRetractEvent pull = new BlockPistonRetractEvent(world.getBlockAt(23, 1, 2),
				List.of(world.getBlockAt(21, 1, 2)), BlockFace.WEST);
		final BlockPistonRetractEvent pullOut = new BlockPistonRetractEvent(world.getBlockAt(22, 1, 2),
				List.of(world.getBlockAt(20, 1, 2)), BlockFace.WEST);

		// when
		server.getPluginManager().callEvent(push);
		server.getPluginManager().callEvent(pushAway);
		server.getPluginManager().callEvent(pull);
		server.getPluginManager().callEvent(pullOut);

		// then
		assertThat(push.isCancelled()).isTrue();
		assertThat(pushAway.isCancelled()).isFalse();
		assertThat(pull.isCancelled()).isFalse();
		assertThat(pullOut.isCancelled()).isTrue();
	}
}
