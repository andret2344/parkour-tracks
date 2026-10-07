package eu.andret.parkourtracks.command;

import eu.andret.parkourtracks.ParkourTracksPlugin;
import eu.andret.parkourtracks.api.TrackPaymentEvent;
import eu.andret.parkourtracks.config.Medal;
import eu.andret.parkourtracks.economy.Bank;
import eu.andret.parkourtracks.economy.MedalPayouts;
import eu.andret.parkourtracks.message.Message;
import eu.andret.parkourtracks.result.ResultStore;
import eu.andret.parkourtracks.track.Track;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Description;
import revxrsal.commands.annotation.Optional;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * {@code /ptracks reconcile <track> [code]}: pays the medal rewards players are owed but were never paid, e.g. after a
 * medal was added or a threshold loosened. Without a code it shows what it would pay and gives a code; the code pays
 * exactly that, within a minute, for the same sender, and only if nothing changed meanwhile.
 */
@Command({"parkourtracks", "ptracks"})
public final class ReconcileCommand {
	private static final int CODE_TICKS = 60 * 20;
	private static final int TOP_PLAYERS = 5;
	private static final String CONSOLE = "console";
	private static final String COUNT = "count";

	@NotNull
	private final CommandSupport support;
	@NotNull
	private final Map<String, Pending> pending = new HashMap<>();
	@NotNull
	private final Random random = new Random();

	/**
	 * A payment of one medal to one player.
	 */
	private record Payment(@NotNull UUID player, @NotNull Medal medal, double amount) {
	}

	/**
	 * A preview waiting for its code: what it showed, to be compared with what is owed when the code comes.
	 */
	private record Pending(@NotNull UUID track, @NotNull String code, @NotNull List<Payment> payments, int expires) {
	}

	public ReconcileCommand(@NotNull final CommandSupport support) {
		this.support = support;
	}

	@Subcommand("reconcile")
	@Description("Shows the medal rewards owed on a track, or pays them with the code the preview gave")
	@CommandPermission(Permissions.RECONCILE)
	public void reconcile(@NotNull final CommandSender sender, @NotNull final Track track,
			@Optional @Nullable final String code) {
		final ParkourTracksPlugin plugin = support.getPlugin();
		if (!plugin.getBank().isAvailable()) {
			throw support.fail(Message.NO_ECONOMY);
		}
		plugin.getResults().fetchStandings(track.getId())
				.thenAcceptAsync(standings -> {
					final List<Payment> payments = computeOwed(track, standings);
					if (code == null) {
						preview(sender, track, payments);
					} else {
						confirm(sender, track, code, payments);
					}
				}, plugin::runOnMainThread);
	}

	@NotNull
	private List<Payment> computeOwed(@NotNull final Track track, @NotNull final List<ResultStore.Standing> standings) {
		final List<Medal> medals = support.getPlugin().getSettings().medals();
		return standings.stream()
				.flatMap(standing -> MedalPayouts.findDue(medals, track.getMedals(), standing.bestTicks(), standing.paidMedals())
						.stream()
						.map(due -> new Payment(standing.player(), due.medal(), due.amount())))
				.toList();
	}

	private void preview(@NotNull final CommandSender sender, @NotNull final Track track,
			@NotNull final List<Payment> payments) {
		if (payments.isEmpty()) {
			support.send(sender, Message.RECONCILE_NOTHING, CommandSupport.createTrackPlaceholder(track));
			return;
		}
		final Bank bank = support.getPlugin().getBank();
		final long players = payments.stream().map(Payment::player).distinct().count();
		support.send(sender, Message.RECONCILE_PREVIEW, CommandSupport.createTrackPlaceholder(track),
				CommandSupport.createPlaceholder(COUNT, players), CommandSupport.createPlaceholder("total", bank.format(computeTotal(payments))));
		final Map<Medal, List<Payment>> byMedal = new LinkedHashMap<>();
		support.getPlugin().getSettings().medals().forEach(medal -> byMedal.put(medal, payments.stream()
				.filter(payment -> payment.medal().equals(medal))
				.toList()));
		byMedal.forEach((medal, ofMedal) -> {
			if (!ofMedal.isEmpty()) {
				support.send(sender, Message.RECONCILE_PREVIEW_MEDAL, Placeholder.parsed("medal", medal.displayName()),
						CommandSupport.createPlaceholder(COUNT, ofMedal.size()), CommandSupport.createPlaceholder("amount", bank.format(computeTotal(ofMedal))));
			}
		});
		final Map<UUID, Double> byPlayer = new HashMap<>();
		payments.forEach(payment -> byPlayer.merge(payment.player(), payment.amount(), Double::sum));
		byPlayer.entrySet()
				.stream()
				.sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
				.limit(TOP_PLAYERS)
				.forEach(entry -> support.send(sender, Message.RECONCILE_PREVIEW_PLAYER,
						CommandSupport.createPlaceholder("player", getPlayerName(entry.getKey())),
						CommandSupport.createPlaceholder("amount", bank.format(entry.getValue()))));
		final String code = String.format(Locale.ROOT, "%04d", random.nextInt(10_000));
		pending.put(getSenderKey(sender), new Pending(track.getId(), code, payments,
				support.getPlugin().getServer().getCurrentTick() + CODE_TICKS));
		support.send(sender, Message.RECONCILE_CONFIRM_HINT, CommandSupport.createTrackPlaceholder(track), CommandSupport.createPlaceholder("code", code));
	}

	private void confirm(@NotNull final CommandSender sender, @NotNull final Track track, @NotNull final String code,
			@NotNull final List<Payment> payments) {
		final Pending waiting = pending.get(getSenderKey(sender));
		final boolean valid = waiting != null && waiting.track().equals(track.getId()) && waiting.code().equals(code)
				&& support.getPlugin().getServer().getCurrentTick() <= waiting.expires();
		// This runs after the database answered, outside the command, so failures are sent, not thrown
		if (!valid) {
			support.send(sender, Message.RECONCILE_EXPIRED, CommandSupport.createTrackPlaceholder(track));
			return;
		}
		pending.remove(getSenderKey(sender));
		if (!waiting.payments().equals(payments)) {
			support.send(sender, Message.RECONCILE_CHANGED, CommandSupport.createTrackPlaceholder(track));
			return;
		}
		final Bank bank = support.getPlugin().getBank();
		int paid = 0;
		double total = 0;
		for (final Payment payment : payments) {
			final OfflinePlayer player = support.getPlugin().getServer().getOfflinePlayer(payment.player());
			if (!bank.deposit(player, payment.amount())) {
				support.getPlugin().getLogger().warning(() -> "Reconciling %s: could not pay %s %s for %s".formatted(track.getName(), getPlayerName(payment.player()), payment.amount(), payment.medal().key()));
				continue;
			}
			support.getPlugin().getResults().recordPayout(track.getId(), payment.player(), payment.medal().key(),
					payment.amount(), Instant.now());
			support.getPlugin().getLogger().info(() -> "Reconciling %s by %s: paid %s %s for %s".formatted(track.getName(), sender.getName(), getPlayerName(payment.player()), payment.amount(), payment.medal().key()));
			support.getPlugin().getServer().getPluginManager().callEvent(new TrackPaymentEvent(player, track.getId(),
					track.getName(), TrackPaymentEvent.Kind.RECONCILIATION, payment.medal().key(), payment.amount()));
			paid++;
			total += payment.amount();
		}
		support.send(sender, Message.RECONCILE_DONE, CommandSupport.createPlaceholder(COUNT, paid),
				CommandSupport.createPlaceholder("total", bank.format(total)));
		if (paid < payments.size()) {
			support.send(sender, Message.RECONCILE_FAILED, CommandSupport.createTrackPlaceholder(track),
					CommandSupport.createPlaceholder(COUNT, payments.size() - paid));
		}
	}

	private static double computeTotal(@NotNull final List<Payment> payments) {
		return payments.stream().mapToDouble(Payment::amount).sum();
	}

	@NotNull
	private String getPlayerName(@NotNull final UUID id) {
		final OfflinePlayer player = support.getPlugin().getServer().getOfflinePlayer(id);
		final String name = player.getName();
		if (name != null) {
			return name;
		}
		return id.toString();
	}

	@NotNull
	private static String getSenderKey(@NotNull final CommandSender sender) {
		return sender instanceof final Player player ? player.getUniqueId().toString() : CONSOLE;
	}
}
