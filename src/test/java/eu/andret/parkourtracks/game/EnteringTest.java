package eu.andret.parkourtracks.game;

import eu.andret.parkourtracks.helper.FakeBank;
import eu.andret.parkourtracks.menu.Menu;
import eu.andret.parkourtracks.track.TrackEffect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.assertj.core.api.Assertions.assertThat;

class EnteringTest extends GameTest {
	@Test
	void walkingIntoTheSpawnJoinsTheGame() {
		// given
		final PlayerMock player = player();
		player.teleport(at(-5, 1, 2.5));

		// when
		move(player, at(2.5, 1, 2.5));

		// then
		assertThat(session(player).getPhase()).isEqualTo(Phase.WAITING);
		assertThat(session(player).getTrack()).isEqualTo(track);
		assertThat(messages(player)).containsExactly("You joined track tower. Leave the spawn to start the timer.");
		assertThat(player.getLocation()).isEqualTo(at(2.5, 1, 2.5));
	}

	@Test
	void walkingInFromTheSideSendsToTheSpawn() {
		// given
		final PlayerMock player = player();
		player.teleport(at(10, 1, 25));

		// when
		move(player, at(10, 1, 20));

		// then
		assertThat(session(player).getPhase()).isEqualTo(Phase.WAITING);
		assertThat(player.getLocation()).isEqualTo(at(2.5, 1, 2.5));
		assertThat(messages(player)).containsExactly("Tracks start at their spawn: you were sent to the spawn of tower.");
	}

	@Test
	void spectatorAndIgnoringPlayersAreNotParkourPlayers() {
		// given
		final PlayerMock spectator = player();
		spectator.setGameMode(GameMode.SPECTATOR);
		final PlayerMock ignoring = player();
		games().toggleIgnoring(ignoring);

		// when
		move(spectator, at(10, 1, 2.5));
		move(ignoring, at(10, 1, 2.5));

		// then
		assertThat(games().getSessions()).isEmpty();
		assertThat(spectator.getLocation()).isEqualTo(at(10, 1, 2.5));
	}

	@Test
	void creativePlayersPlayWithoutFlying() {
		// given
		final PlayerMock player = player();
		player.setGameMode(GameMode.CREATIVE);
		player.setAllowFlight(true);
		player.setFlying(true);
		player.teleport(at(-5, 1, 2.5));

		// when
		move(player, at(2.5, 1, 2.5));

		// then
		assertThat(session(player).getTrack()).isEqualTo(track);
		assertThat(player.getGameMode()).isEqualTo(GameMode.CREATIVE);
		assertThat(player.getAllowFlight()).isFalse();
		assertThat(player.isFlying()).isFalse();
	}

	@Test
	void ignoringLastsOverARestart() {
		// given
		final PlayerMock player = player();
		games().toggleIgnoring(player);
		server.getPluginManager().disablePlugin(plugin);
		server.getPluginManager().enablePlugin(plugin);

		// when
		move(player, at(10, 1, 2.5));

		// then
		assertThat(games().isIgnoring(player)).isTrue();
		assertThat(games().getSessions()).isEmpty();
	}

	@Test
	void enterCommandEntersARunningTrack() {
		// given
		final PlayerMock player = player();

		// when
		player.performCommand("ptracks enter tower");

		// then
		assertThat(session(player).getTrack()).isEqualTo(track);
		assertThat(session(player).getPhase()).isEqualTo(Phase.WAITING);
	}

	@Test
	void enterCommandRefusesAStoppedTrack() {
		// given
		track.setRunning(false);
		final PlayerMock player = player();

		// when
		player.performCommand("ptracks enter tower");

		// then
		assertThat(games().getSessions()).isEmpty();
		assertThat(messages(player)).containsExactly("Track tower is not running.");
	}

	@Test
	void enterCommandAsksBeforeAFee() {
		// given
		final FakeBank bank = new FakeBank();
		plugin.setBank(bank);
		final PlayerMock player = player();
		bank.set(player, 100);
		track.getOptions().setFee(30);

		// when
		player.performCommand("ptracks enter tower");

		// then
		assertThat(games().getSessions()).isEmpty();
		assertThat(player.getOpenInventory().getTopInventory().getHolder()).isInstanceOf(Menu.class);
	}

	@Test
	void stoppedTracksAreNotPlayed() {
		// given
		track.setRunning(false);
		final PlayerMock player = player();

		// when
		move(player, at(10, 1, 2.5));

		// then
		assertThat(games().getSessions()).isEmpty();
	}

	@Test
	void sameCoordinatesInAnotherWorldAreNotTheTrack() {
		// given
		final PlayerMock player = player();
		player.teleport(new Location(server.addSimpleWorld("nether"), 50, 1, 2.5));

		// when
		move(player, new Location(player.getWorld(), 10, 1, 2.5));

		// then
		assertThat(games().getSessions()).isEmpty();
	}

	@Test
	void joiningTakesTheStateAndGivesTheTrackEffects() {
		// given
		track.setEffect(new TrackEffect("minecraft:jump_boost", 1));
		final PlayerMock player = player();
		player.getInventory().addItem(new ItemStack(Material.DIAMOND, 5));
		player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 600, 2));
		player.setLevel(30);
		player.setAllowFlight(true);
		player.teleport(at(-5, 1, 2.5));

		// when
		move(player, at(2.5, 1, 2.5));

		// then
		assertThat(player.getInventory().contains(Material.DIAMOND)).isFalse();
		assertThat(player.getInventory().getItem(0)).extracting(ItemStack::getType).isEqualTo(Material.SLIME_BALL);
		assertThat(player.getActivePotionEffects()).extracting(PotionEffect::getType)
				.containsExactly(PotionEffectType.JUMP_BOOST);
		assertThat(player.getPotionEffect(PotionEffectType.JUMP_BOOST).isInfinite()).isTrue();
		assertThat(player.getLevel()).isZero();
		assertThat(player.getAllowFlight()).isFalse();
	}

	@Test
	void teleportIntoTheSpawnJoins() {
		// given
		final PlayerMock player = player();

		// when
		player.teleport(at(2.5, 1, 2.5), PlayerTeleportEvent.TeleportCause.COMMAND);

		// then
		assertThat(session(player).getPhase()).isEqualTo(Phase.WAITING);
		assertThat(player.getLocation()).isEqualTo(at(2.5, 1, 2.5));
	}

	@Test
	void teleportIntoTheMiddleSendsToTheSpawn() {
		// given
		final PlayerMock player = player();

		// when
		player.teleport(at(15.5, 1, 2.5), PlayerTeleportEvent.TeleportCause.COMMAND);

		// then
		assertThat(session(player).getPhase()).isEqualTo(Phase.WAITING);
		assertThat(player.getLocation()).isEqualTo(at(2.5, 1, 2.5));
	}

	@Test
	void enterSendsToTheSpawnFromAnywhere() {
		// given
		final PlayerMock player = player();

		// when
		games().enter(player, track);

		// then
		assertThat(session(player).getPhase()).isEqualTo(Phase.WAITING);
		assertThat(player.getLocation()).isEqualTo(at(2.5, 1, 2.5));
		assertThat(messages(player)).containsExactly("You joined track tower. Leave the spawn to start the timer.");
	}
}
