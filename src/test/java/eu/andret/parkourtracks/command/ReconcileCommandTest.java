package eu.andret.parkourtracks.command;

import eu.andret.parkourtracks.economy.NoBank;
import eu.andret.parkourtracks.helper.FakeBank;
import eu.andret.parkourtracks.helper.PluginTest;
import eu.andret.parkourtracks.track.MedalThreshold;
import eu.andret.parkourtracks.track.Track;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class ReconcileCommandTest extends PluginTest {
	private static final Pattern CODE = Pattern.compile("reconcile tower (\\d{4})");

	private final FakeBank bank = new FakeBank();
	private Track track;
	private PlayerMock admin;
	private PlayerMock alice;
	private PlayerMock bob;

	@BeforeEach
	void setUpResults() {
		plugin.setBank(bank);
		track = tower();
		admin = admin(60, 64, 60);
		alice = server.addPlayer("Alice");
		bob = server.addPlayer("Bob");
		run(alice.getUniqueId(), 90);
		run(bob.getUniqueId(), 150);
	}

	void run(final UUID player, final int ticks) {
		plugin.getResults().recordRun(track.getId(), player, ticks, Instant.now()).join();
	}

	List<String> reconcile(final PlayerMock sender, final String arguments) {
		sender.performCommand("ptracks reconcile " + arguments);
		plugin.getResults().flush();
		server.getScheduler().performTicks(1);
		plugin.getResults().flush();
		return messages(sender);
	}

	static String code(final List<String> messages) {
		final Matcher matcher = CODE.matcher(String.join("\n", messages));
		assertThat(matcher.find()).isTrue();
		return matcher.group(1);
	}

	@Test
	void thePreviewShowsWhatIsOwedAndPaysNothing() {
		// given: medals added after the runs
		track.setMedal("gold", new MedalThreshold(100, 50));
		track.setMedal("silver", new MedalThreshold(200, 20));

		// when
		final List<String> messages = reconcile(admin, "tower");

		// then
		assertThat(messages).startsWith("Medal rewards not paid yet on track tower: 2 players, $90.00 in all.",
				"- Gold: 1 players, $50.00", "- Silver: 2 players, $40.00", "  Alice: $70.00", "  Bob: $20.00");
		assertThat(messages.getLast()).startsWith("To pay it, run /ptracks reconcile tower ");
		assertThat(bank.balance(alice)).isZero();
	}

	@Test
	void theCodePaysWhatThePreviewShowedOnce() {
		// given
		track.setMedal("gold", new MedalThreshold(100, 50));
		track.setMedal("silver", new MedalThreshold(200, 20));
		final String code = code(reconcile(admin, "tower"));

		// when
		final List<String> paid = reconcile(admin, "tower " + code);
		final List<String> again = reconcile(admin, "tower");

		// then
		assertThat(paid).containsExactly("Paid 3 medal rewards, $90.00 in all.");
		assertThat(bank.balance(alice)).isEqualTo(70);
		assertThat(bank.balance(bob)).isEqualTo(20);
		assertThat(again).containsExactly("Every medal earned on track tower is paid already.");
	}

	@Test
	void aChangeBetweenThePreviewAndTheCodePaysNothing() {
		// given
		track.setMedal("gold", new MedalThreshold(100, 50));
		final String code = code(reconcile(admin, "tower"));
		track.setMedal("gold", new MedalThreshold(100, 5000));

		// when
		final List<String> messages = reconcile(admin, "tower " + code);

		// then
		assertThat(messages).containsExactly("The thresholds or the results changed since the preview; nothing was "
				+ "paid. Run /ptracks reconcile tower again.");
		assertThat(bank.balance(alice)).isZero();
	}

	@Test
	void theCodeExpiresAfterAMinuteAndBelongsToItsSender() {
		// given
		track.setMedal("gold", new MedalThreshold(100, 50));
		final String code = code(reconcile(admin, "tower"));
		final PlayerMock other = admin(60, 64, 60);

		// when
		final List<String> stranger = reconcile(other, "tower " + code);
		final List<String> wrong = reconcile(admin, "tower 99999");
		server.getScheduler().performTicks(60 * 20);
		final List<String> late = reconcile(admin, "tower " + code);

		// then
		assertThat(List.of(stranger, wrong, late)).allSatisfy(messages -> assertThat(messages).containsExactly(
				"There is no such preview, or it expired. Run /ptracks reconcile tower to see it again."));
		assertThat(bank.balance(alice)).isZero();
	}

	@Test
	void failedPaymentsStayOwed() {
		// given
		track.setMedal("silver", new MedalThreshold(200, 20));
		bank.refuse(bob, true);
		final String code = code(reconcile(admin, "tower"));

		// when
		final List<String> messages = reconcile(admin, "tower " + code);
		bank.refuse(bob, false);
		final List<String> retry = reconcile(admin, "tower");

		// then
		assertThat(messages).containsExactly("Paid 1 medal rewards, $20.00 in all.",
				"1 payments failed and stay owed; run /ptracks reconcile tower again to retry them.");
		assertThat(retry).first().isEqualTo("Medal rewards not paid yet on track tower: 1 players, $20.00 in all.");
	}

	@Test
	void medalsWithoutARewardAreNotOwed() {
		// given
		track.setMedal("gold", new MedalThreshold(100, 0));

		// when
		final List<String> messages = reconcile(admin, "tower");

		// then
		assertThat(messages).containsExactly("Every medal earned on track tower is paid already.");
	}

	@Test
	void needsAnEconomy() {
		// given
		plugin.setBank(new NoBank());

		// when
		final List<String> messages = reconcile(admin, "tower");

		// then
		assertThat(messages).containsExactly("There is no economy plugin, so there is nothing to pay.");
	}
}
