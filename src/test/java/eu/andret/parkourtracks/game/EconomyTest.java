package eu.andret.parkourtracks.game;

import eu.andret.parkourtracks.economy.NoBank;
import eu.andret.parkourtracks.helper.FakeBank;
import eu.andret.parkourtracks.track.MedalThreshold;
import eu.andret.parkourtracks.track.TrackType;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.permissions.PermissionAttachment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EconomyTest extends GameTest {
	private final FakeBank bank = new FakeBank();

	@BeforeEach
	void setUpBank() {
		plugin.setBank(bank);
	}

	@Override
	protected PlayerMock player() {
		final PlayerMock player = super.player();
		bank.set(player, 100);
		return player;
	}

	void waitForResults() {
		plugin.getResults().flush();
		server.getScheduler().performTicks(1);
	}

	void finish(final PlayerMock player, final int ticks) {
		player.teleport(at(-5, 1, 2.5));
		move(player, at(2.5, 1, 2.5));
		move(player, at(4.5, 1, 2.5));
		server.getScheduler().performTicks(ticks);
		walkTo(player, 8.5);
		walkTo(player, 12.5);
		walkTo(player, 17.5);
		waitForResults();
		games().leave(player, LeaveReason.EXIT);
	}

	@Test
	void walkingInPaysTheFee() {
		// given
		track.getOptions().setFee(30);

		// when
		final PlayerMock player = onSpawn();

		// then
		assertThat(bank.balance(player)).isEqualTo(70);
		assertThat(games().getSession(player)).isPresent();
	}

	@Test
	void everyWayInPaysTheFee() {
		// given
		track.getOptions().setFee(10);
		final PlayerMock side = player();
		final PlayerMock teleported = player();
		final PlayerMock entered = player();

		// when
		move(side, at(10, 1, 20));
		teleported.teleport(at(10, 1, 2.5), PlayerTeleportEvent.TeleportCause.COMMAND);
		games().enter(entered, track);

		// then
		assertThat(List.of(bank.balance(side), bank.balance(teleported), bank.balance(entered))).containsOnly(90.0);
		assertThat(messages(entered)).contains("Paid $10.00 to enter track tower.");
	}

	@Test
	void aPlayerWhoCannotPayIsSentToTheLobby() {
		// given
		track.getOptions().setFee(150);
		final PlayerMock walker = player();
		final PlayerMock teleported = player();

		// when
		move(walker, at(10, 1, 20));
		teleported.teleport(at(10, 1, 2.5), PlayerTeleportEvent.TeleportCause.COMMAND);

		// then
		assertThat(games().getSessions()).isEmpty();
		assertThat(walker.getLocation()).isEqualTo(at(100, 64, 100));
		assertThat(teleported.getLocation()).isEqualTo(at(100, 64, 100));
		assertThat(bank.balance(walker)).isEqualTo(100);
		assertThat(messages(walker)).containsExactly("Entering track tower costs $150.00, which you cannot afford.");
	}

	@Test
	void comingBackAfterADisconnectIsFree() {
		// given
		track.getOptions().setFee(30);
		final PlayerMock player = onSpawn();

		// when
		player.disconnect();
		player.reconnect();

		// then
		assertThat(games().getSession(player)).isPresent();
		assertThat(bank.balance(player)).isEqualTo(70);
	}

	@Test
	void withoutAnEconomyTracksAreFree() {
		// given
		plugin.setBank(new NoBank());
		track.getOptions().setFee(30);

		// when
		final PlayerMock player = onSpawn();

		// then
		assertThat(games().getSession(player)).isPresent();
	}

	@Test
	void theTrackPermissionIsNeeded() {
		// given
		track.getOptions().setPermission("tracks.vip");
		track.getOptions().setFee(10);
		final PlayerMock refused = player();
		final PlayerMock vip = player();
		final PermissionAttachment attachment = vip.addAttachment(plugin);
		attachment.setPermission("tracks.vip", true);

		// when
		move(refused, at(10, 1, 20));
		move(vip, at(10, 1, 20));

		// then
		assertThat(games().getSession(refused)).isEmpty();
		assertThat(refused.getLocation()).isEqualTo(at(100, 64, 100));
		assertThat(bank.balance(refused)).isEqualTo(100);
		assertThat(messages(refused)).containsExactly("You may not enter track tower.");
		assertThat(games().getSession(vip)).isPresent();
	}

	@Test
	void stoppingTheTrackGivesTheFeeBack() {
		// given
		track.getOptions().setFee(30);
		final PlayerMock player = onSpawn();

		// when
		admin(60, 64, 60).performCommand("ptracks stop tower");

		// then
		assertThat(bank.balance(player)).isEqualTo(100);
		assertThat(messages(player)).contains("You got back the $30.00 you paid for track tower.");
	}

	@Test
	void theRefundCanBeTurnedOff() throws IOException {
		// given
		writeConfig("refund-on-stop: false\n");
		plugin.reload();
		track.getOptions().setFee(30);
		final PlayerMock player = onSpawn();

		// when
		admin(60, 64, 60).performCommand("ptracks stop tower");

		// then
		assertThat(bank.balance(player)).isEqualTo(70);
	}

	@Test
	void leavingOtherwiseGivesNoFeeBack() {
		// given
		track.getOptions().setFee(30);
		final PlayerMock player = onSpawn();

		// when
		games().leave(player, LeaveReason.EXIT);

		// then
		assertThat(bank.balance(player)).isEqualTo(70);
	}

	@Test
	void everyCompletionPaysTheReward() {
		// given
		track.getOptions().setReward(5);
		final PlayerMock player = player();

		// when
		finish(player, 100);
		finish(player, 100);

		// then
		assertThat(bank.balance(player)).isEqualTo(110);
		assertThat(messages(player)).contains("You got $5.00 for finishing track tower.");
	}

	@Test
	void trainingPaysNoReward() {
		// given
		track.setType(TrackType.TRAINING);
		track.getOptions().setReward(5);
		final PlayerMock player = player();

		// when
		finish(player, 100);

		// then
		assertThat(bank.balance(player)).isEqualTo(100);
	}

	@Test
	void medalsArePaidOnceEachAndAGoodRunEarnsTheWorseOnesToo() {
		// given
		track.setMedal("gold", new MedalThreshold(100, 50));
		track.setMedal("silver", new MedalThreshold(200, 20));
		track.setMedal("bronze", new MedalThreshold(300, 0));
		final PlayerMock player = player();

		// when
		finish(player, 150);
		final double afterSilver = bank.balance(player);
		finish(player, 50);
		final double afterGold = bank.balance(player);
		finish(player, 50);

		// then
		assertThat(afterSilver).isEqualTo(120);
		assertThat(afterGold).isEqualTo(170);
		assertThat(bank.balance(player)).isEqualTo(170);
	}

	@Test
	void tighterThresholdsPayNothingAgain() {
		// given
		track.setMedal("gold", new MedalThreshold(100, 50));
		final PlayerMock player = player();
		finish(player, 90);

		// when
		track.setMedal("gold", new MedalThreshold(80, 50));
		finish(player, 70);

		// then
		assertThat(bank.balance(player)).isEqualTo(150);
	}

	@Test
	void aFailedPaymentStaysOwed() {
		// given
		track.setMedal("gold", new MedalThreshold(100, 50));
		final PlayerMock player = player();
		bank.refuse(player, true);
		finish(player, 90);

		// when
		bank.refuse(player, false);
		finish(player, 90);

		// then
		assertThat(bank.balance(player)).isEqualTo(150);
	}
}
