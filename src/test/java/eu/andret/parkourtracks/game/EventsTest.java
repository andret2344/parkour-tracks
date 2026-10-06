package eu.andret.parkourtracks.game;

import eu.andret.parkourtracks.api.TrackCompleteEvent;
import eu.andret.parkourtracks.api.TrackJoinEvent;
import eu.andret.parkourtracks.api.TrackLeaveEvent;
import eu.andret.parkourtracks.api.TrackPaymentEvent;
import eu.andret.parkourtracks.helper.FakeBank;
import eu.andret.parkourtracks.track.MedalThreshold;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class EventsTest extends GameTest {
	private final List<Event> events = new ArrayList<>();
	private final FakeBank bank = new FakeBank();
	private boolean refuseJoins;

	public class Recorder implements Listener {
		@EventHandler
		public void join(final TrackJoinEvent event) {
			events.add(event);
			event.setCancelled(refuseJoins);
		}

		@EventHandler
		public void leave(final TrackLeaveEvent event) {
			events.add(event);
		}

		@EventHandler
		public void complete(final TrackCompleteEvent event) {
			events.add(event);
		}

		@EventHandler
		public void payment(final TrackPaymentEvent event) {
			events.add(event);
		}
	}

	@BeforeEach
	void setUpRecorder() {
		server.getPluginManager().registerEvents(new Recorder(), plugin);
		plugin.setBank(bank);
	}

	<T extends Event> List<T> of(final Class<T> type) {
		return events.stream().filter(type::isInstance).map(type::cast).toList();
	}

	void finish(final PlayerMock player) {
		walkTo(player, 8.5);
		walkTo(player, 12.5);
		walkTo(player, 17.5);
		plugin.getResults().flush();
		server.getScheduler().performTicks(1);
	}

	@Test
	void joiningAndLeavingAreReported() {
		// given
		final PlayerMock player = onSpawn();

		// when
		games().leave(player, LeaveReason.EXIT);

		// then
		assertThat(of(TrackJoinEvent.class)).singleElement().satisfies(join -> {
			assertThat(join.getPlayer()).isEqualTo(player);
			assertThat(join.getTrackId()).isEqualTo(track.getId());
			assertThat(join.getTrackName()).isEqualTo("tower");
			assertThat(join.getEntry()).isEqualTo(Entry.SPAWN);
		});
		assertThat(of(TrackLeaveEvent.class)).singleElement().satisfies(leave -> {
			assertThat(leave.getReason()).isEqualTo(LeaveReason.EXIT);
			assertThat(leave.getTrackId()).isEqualTo(track.getId());
			assertThat(leave.getTrackName()).isEqualTo("tower");
		});
	}

	@Test
	void anotherPluginCanKeepAPlayerOut() {
		// given
		refuseJoins = true;
		track.getOptions().setFee(10);
		final PlayerMock player = player();
		bank.set(player, 100);

		// when
		player.simulatePlayerMove(at(10, 1, 20));

		// then
		assertThat(games().getSession(player)).isEmpty();
		assertThat(player.getLocation()).isEqualTo(at(100, 64, 100));
		assertThat(bank.balance(player)).isEqualTo(100);
	}

	@Test
	void aCompletionIsReportedWithWhatItAchieved() {
		// given
		track.setMedal("gold", new MedalThreshold(1000, 0));
		final PlayerMock player = running();

		// when
		finish(player);

		// then
		assertThat(of(TrackCompleteEvent.class)).singleElement().satisfies(complete -> {
			assertThat(complete.getPlayer().getUniqueId()).isEqualTo(player.getUniqueId());
			assertThat(complete.getTrackId()).isEqualTo(track.getId());
			assertThat(complete.getTrackName()).isEqualTo("tower");
			assertThat(complete.getTicks()).isZero();
			assertThat(complete.getMedal()).isEqualTo("gold");
			assertThat(complete.isPersonalBest()).isTrue();
			assertThat(complete.isTrackRecord()).isTrue();
		});
	}

	@Test
	void paymentsAreReported() {
		// given
		track.getOptions().setFee(10);
		track.getOptions().setReward(5);
		track.setMedal("gold", new MedalThreshold(1000, 50));
		final PlayerMock player = player();
		bank.set(player, 100);
		player.teleport(at(-5, 1, 2.5));
		player.simulatePlayerMove(at(2.5, 1, 2.5));
		player.simulatePlayerMove(at(4.5, 1, 2.5));

		// when
		finish(player);
		games().leave(player, LeaveReason.EXIT);
		player.simulatePlayerMove(at(2.5, 1, 2.5));
		admin(60, 64, 60).performCommand("ptracks stop tower");

		// then
		assertThat(of(TrackPaymentEvent.class)).extracting(TrackPaymentEvent::getKind, TrackPaymentEvent::getMedal,
				TrackPaymentEvent::getAmount).containsExactly(
				tuple(TrackPaymentEvent.Kind.FEE, null, 10.0),
				tuple(TrackPaymentEvent.Kind.REWARD, null, 5.0),
				tuple(TrackPaymentEvent.Kind.MEDAL, "gold", 50.0),
				tuple(TrackPaymentEvent.Kind.FEE, null, 10.0),
				tuple(TrackPaymentEvent.Kind.REFUND, null, 10.0));
		assertThat(of(TrackPaymentEvent.class)).allSatisfy(payment -> {
			assertThat(payment.getPlayer().getUniqueId()).isEqualTo(player.getUniqueId());
			assertThat(payment.getTrackId()).isEqualTo(track.getId());
			assertThat(payment.getTrackName()).isEqualTo("tower");
		});
	}

	@Test
	void reconciliationsAreReported() {
		// given
		final PlayerMock player = running();
		finish(player);
		track.setMedal("gold", new MedalThreshold(1000, 50));
		final PlayerMock admin = admin(60, 64, 60);
		admin.performCommand("ptracks reconcile tower");
		plugin.getResults().flush();
		server.getScheduler().performTicks(1);
		final Matcher code = Pattern.compile("reconcile tower (\\d{4})").matcher(String.join("\n", messages(admin)));
		assertThat(code.find()).isTrue();

		// when
		admin.performCommand("ptracks reconcile tower " + code.group(1));
		plugin.getResults().flush();
		server.getScheduler().performTicks(1);

		// then
		assertThat(of(TrackPaymentEvent.class)).singleElement().satisfies(payment -> {
			assertThat(payment.getKind()).isEqualTo(TrackPaymentEvent.Kind.RECONCILIATION);
			assertThat(payment.getMedal()).isEqualTo("gold");
			assertThat(payment.getAmount()).isEqualTo(50);
		});
	}
}
