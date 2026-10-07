package eu.andret.parkourtracks.game;

import eu.andret.parkourtracks.track.Spot;
import eu.andret.parkourtracks.track.WorldSpot;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.assertj.core.api.Assertions.assertThat;

class LeavingTest extends GameTest {
	/**
	 * A player with a diamond, level 30 and speed, who then walked onto the spawn.
	 */
	PlayerMock withThings() {
		final PlayerMock player = player();
		player.getInventory().addItem(new ItemStack(Material.DIAMOND, 5));
		player.setLevel(30);
		player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 6000, 2));
		player.teleport(at(-5, 1, 2.5));
		move(player, at(2.5, 1, 2.5));
		messages(player);
		return player;
	}

	void assertHasThingsBack(final PlayerMock player) {
		assertThat(player.getInventory().contains(Material.DIAMOND, 5)).isTrue();
		assertThat(player.getLevel()).isEqualTo(30);
		assertThat(player.getPotionEffect(PotionEffectType.SPEED)).isNotNull();
	}

	@Test
	void walkingOutOfTheRegionEndsTheGameWhereThePlayerIs() {
		// given
		final PlayerMock player = withThings();
		move(player, at(5.5, 1, 2.5));

		// when
		move(player, at(5.5, 1, 21.5));

		// then
		assertThat(games().getSession(player)).isEmpty();
		assertThat(player.getLocation()).isEqualTo(at(5.5, 1, 21.5));
		assertThat(messages(player)).containsExactly("You left track tower.");
		assertHasThingsBack(player);
	}

	@Test
	void leavingThroughTheSpawnEndsTheGameToo() {
		// given
		final PlayerMock player = withThings();

		// when
		move(player, at(-0.5, 1, 2.5));

		// then
		assertThat(games().getSession(player)).isEmpty();
	}

	@Test
	void exitSendsToTheLobbyAndGivesTheStateBack() {
		// given
		final PlayerMock player = withThings();

		// when
		games().leave(player, LeaveReason.EXIT);

		// then
		assertThat(player.getLocation()).isEqualTo(at(100, 64, 100));
		assertHasThingsBack(player);
	}

	@Test
	void aForeignTeleportInsideTheRegionSendsBackToTheLastCheckpoint() {
		// given
		final PlayerMock player = running();
		walkTo(player, 9.5);

		// when
		player.teleport(at(18.5, 1, 2.5), PlayerTeleportEvent.TeleportCause.COMMAND);

		// then
		assertThat(player.getLocation()).isEqualTo(at(8.5, 1, 2.5));
		assertThat(session(player).getPhase()).isEqualTo(Phase.RUNNING);
	}

	@Test
	void aForeignTeleportOutOfTheRegionEndsTheGame() {
		// given
		final PlayerMock player = withThings();

		// when
		player.teleport(at(60, 64, 60), PlayerTeleportEvent.TeleportCause.COMMAND);

		// then
		assertThat(games().getSession(player)).isEmpty();
		assertThat(player.getLocation()).isEqualTo(at(60, 64, 60));
		assertHasThingsBack(player);
	}

	@Test
	void anEnderPearlIsAForeignTeleportUnlessTheTrackAllowsIt() {
		// given
		final PlayerMock player = running();

		// when
		player.teleport(at(8.5, 1, 2.5), PlayerTeleportEvent.TeleportCause.ENDER_PEARL);

		// then
		assertThat(player.getLocation()).isEqualTo(at(2.5, 1, 2.5));
	}

	@Test
	void anAllowedEnderPearlMovesAlongTheTrack() {
		// given
		track.getOptions().setEnderPearls(true);
		final PlayerMock player = running();

		// when
		player.teleport(at(8.5, 1, 2.5), PlayerTeleportEvent.TeleportCause.ENDER_PEARL);
		server.getScheduler().performTicks(1);

		// then
		assertThat(player.getLocation()).isEqualTo(at(8.5, 1, 2.5));
		assertThat(session(player).getLastCheckpoint()).isZero();
	}

	@Test
	void switchingToCreativeEndsTheGame() {
		// given
		final PlayerMock player = withThings();

		// when
		player.setGameMode(GameMode.CREATIVE);

		// then
		assertThat(games().getSession(player)).isEmpty();
		assertHasThingsBack(player);
	}

	@Test
	void startingToIgnoreTracksEndsTheGame() {
		// given
		final PlayerMock player = withThings();

		// when
		final boolean ignoring = games().toggleIgnoring(player);
		final boolean again = games().toggleIgnoring(player);

		// then
		assertThat(ignoring).isTrue();
		assertThat(again).isFalse();
		assertThat(games().getSession(player)).isEmpty();
	}

	@Test
	void disconnectingGivesTheStateBackAndComingBackReturnsToTheSpawn() {
		// given
		final PlayerMock player = withThings();
		move(player, at(5.5, 1, 2.5));

		// when
		player.disconnect();
		final boolean inGame = games().getSession(player).isPresent();
		final boolean diamonds = player.getInventory().contains(Material.DIAMOND, 5);
		player.reconnect();

		// then
		assertThat(inGame).isFalse();
		assertThat(diamonds).isTrue();
		assertThat(session(player).getPhase()).isEqualTo(Phase.WAITING);
		assertThat(player.getLocation()).isEqualTo(at(2.5, 1, 2.5));
		assertThat(messages(player)).containsExactly("You joined track tower. Leave the spawn to start the timer.");
	}

	@Test
	void comingBackInsideAnotherTrackIsAnEntryFromTheSide() {
		// given
		final PlayerMock player = player();
		player.teleport(at(5.5, 1, 2.5));
		player.getPersistentDataContainer().set(new NamespacedKey(plugin, "session"), PersistentDataType.STRING,
				"6f1c3f6e-2f4a-4a8e-9c1b-1d2e3f4a5b6c");
		games().leave(player, LeaveReason.DISCONNECT);

		// when
		games().arrive(player);

		// then
		assertThat(session(player).getPhase()).isEqualTo(Phase.WAITING);
		assertThat(messages(player)).contains("Tracks start at their spawn: you were sent to the spawn of tower.");
	}

	@Test
	void aSnapshotLeftByACrashIsGivenBackOnJoining() {
		// given: a player who was on the track when the server crashed, now outside it
		final PlayerMock player = withThings();
		final String snapshot = player.getPersistentDataContainer()
				.get(new NamespacedKey(plugin, "snapshot"), PersistentDataType.STRING);
		games().shutdown();
		player.getPersistentDataContainer().set(new NamespacedKey(plugin, "snapshot"), PersistentDataType.STRING,
				snapshot);
		player.getInventory().clear();
		player.teleport(at(60, 64, 60));

		// when
		games().arrive(player);

		// then
		assertHasThingsBack(player);
		assertThat(player.getPersistentDataContainer().has(new NamespacedKey(plugin, "snapshot"))).isFalse();
	}

	@Test
	void stoppingTheTrackSendsItsPlayersToTheLobby() {
		// given
		final PlayerMock player = withThings();
		final PlayerMock admin = admin(60, 64, 60);

		// when
		admin.performCommand("ptracks stop tower");

		// then
		assertThat(games().getSession(player)).isEmpty();
		assertThat(player.getLocation()).isEqualTo(at(100, 64, 100));
		assertThat(messages(player)).containsExactly("Track tower was stopped.");
		assertHasThingsBack(player);
	}

	@Test
	void disablingThePluginGivesEveryoneTheirStateBack() {
		// given
		final PlayerMock player = withThings();

		// when
		server.getPluginManager().disablePlugin(plugin);

		// then
		assertHasThingsBack(player);
	}

	@Test
	void playersOnlineWhenThePluginStartsAreChecked() {
		// given
		final PlayerMock player = player();
		server.getPluginManager().disablePlugin(plugin);
		player.teleport(at(5.5, 1, 2.5));

		// when
		server.getPluginManager().enablePlugin(plugin);

		// then
		assertThat(plugin.getGames().getSession(player)).isPresent();
		assertThat(player.getLocation()).isEqualTo(at(2.5, 1, 2.5));
	}

	@Test
	void leaveCommandSendsToTheLobby() {
		// given
		final PlayerMock player = withThings();

		// when
		player.performCommand("ptracks leave");
		player.performCommand("ptracks leave");

		// then
		assertThat(player.getLocation()).isEqualTo(at(100, 64, 100));
		assertThat(messages(player)).containsExactly("You left track tower.", "You are not on a track.");
	}

	@Test
	void lobbyCommandLeavesTheGameOrJustTeleports() {
		// given
		final PlayerMock player = withThings();
		final PlayerMock other = player();

		// when
		player.performCommand("ptracks lobby");
		other.performCommand("ptracks lobby");

		// then
		assertThat(games().getSession(player)).isEmpty();
		assertThat(player.getLocation()).isEqualTo(at(100, 64, 100));
		assertThat(other.getLocation()).isEqualTo(at(100, 64, 100));
	}

	@Test
	void lobbyCommandWithoutALobby() {
		// given
		plugin.getTrackRegistry().setLobby(null);
		final PlayerMock player = player();

		// when
		player.performCommand("ptracks lobby");

		// then
		assertThat(messages(player)).containsExactly("No lobby is set yet.");
	}

	@Test
	void ignoreCommandSwitches() {
		// given
		final PlayerMock player = player();
		player.setOp(true);

		// when
		player.performCommand("ptracks ignore");
		move(player, at(10, 1, 2.5));
		player.performCommand("ptracks ignore");

		// then
		assertThat(games().getSessions()).isEmpty();
		assertThat(messages(player)).containsExactly(
				"You ignore tracks now: walking into one does not start a game.",
				"You take part in tracks again.");
	}

	@Test
	void theTracksOwnLobbyIsUsedWhenSet() {
		// given
		track.setLobby(new WorldSpot("world", new Spot(30, 64, 30, 0, 0)));
		final PlayerMock player = withThings();

		// when
		games().leave(player, LeaveReason.EXIT);

		// then
		assertThat(player.getLocation()).isEqualTo(at(30, 64, 30));
	}
}
