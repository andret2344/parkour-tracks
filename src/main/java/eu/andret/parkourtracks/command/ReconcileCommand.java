package eu.andret.parkourtracks.command;

import eu.andret.parkourtracks.ParkourTracksPlugin;
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
		final ParkourTracksPlugin plugin = support.plugin();
		if (!plugin.getBank().isAvailable()) {
			throw support.fail(Message.NO_ECONOMY);
		}
		plugin.getResults().standings(track.getId())
				.thenAcceptAsync(standings -> {
					final List<Payment> payments = owed(track, standings);
					if (code == null) {
						preview(sender, track, payments);
					} else {
						confirm(sender, track, code, payments);
					}
				}, plugin::runOnMainThread);
	}

	@NotNull
	private List<Payment> owed(@NotNull final Track track, @NotNull final List<ResultStore.Standing> standings) {
		final List<Medal> medals = support.plugin().getSettings().medals();
		return standings.stream()
				.flatMap(standing -> MedalPayouts.due(medals, track.getMedals(), standing.bestTicks(), standing.paidMedals())
						.stream()
						.map(due -> new Payment(standing.player(), due.medal(), due.amount())))
				.toList();
	}

	private void preview(@NotNull final CommandSender sender, @NotNull final Track track,
						 @NotNull final List<Payment> payments) {
		if (payments.isEmpty()) {
			support.send(sender, Message.RECONCILE_NOTHING, CommandSupport.track(track));
			return;
		}
		final Bank bank = support.plugin().getBank();
		final long players = payments.stream().map(Payment::player).distinct().count();
		support.send(sender, Message.RECONCILE_PREVIEW, CommandSupport.track(track),
				CommandSupport.text("count", players), CommandSupport.text("total", bank.format(total(payments))));
		final Map<Medal, List<Payment>> byMedal = new LinkedHashMap<>();
		support.plugin().getSettings().medals().forEach(medal -> byMedal.put(medal, payments.stream()
				.filter(payment -> payment.medal().equals(medal))
				.toList()));
		byMedal.forEach((medal, ofMedal) -> {
			if (!ofMedal.isEmpty()) {
				support.send(sender, Message.RECONCILE_PREVIEW_MEDAL, Placeholder.parsed("medal", medal.displayName()),
						CommandSupport.text("count", ofMedal.size()), CommandSupport.text("amount", bank.format(total(ofMedal))));
			}
		});
		final Map<UUID, Double> byPlayer = new HashMap<>();
		payments.forEach(payment -> byPlayer.merge(payment.player(), payment.amount(), Double::sum));
		byPlayer.entrySet()
				.stream()
				.sorted(Map.Entry.<UUID, Double>comparingByValue(Comparator.reverseOrder()))
				.limit(TOP_PLAYERS)
				.forEach(entry -> support.send(sender, Message.RECONCILE_PREVIEW_PLAYER,
						CommandSupport.text("player", name(entry.getKey())),
						CommandSupport.text("amount", bank.format(entry.getValue()))));
		final String code = String.format(Locale.ROOT, "%04d", random.nextInt(10_000));
		pending.put(key(sender), new Pending(track.getId(), code, payments,
				support.plugin().getServer().getCurrentTick() + CODE_TICKS));
		support.send(sender, Message.RECONCILE_CONFIRM_HINT, CommandSupport.track(track), CommandSupport.text("code", code));
	}

	private void confirm(@NotNull final CommandSender sender, @NotNull final Track track, @NotNull final String code,
						 @NotNull final List<Payment> payments) {
		final Pending waiting = pending.get(key(sender));
		final boolean valid = waiting != null && waiting.track().equals(track.getId()) && waiting.code().equals(code)
				&& support.plugin().getServer().getCurrentTick() <= waiting.expires();
		// This runs after the database answered, outside the command, so failures are sent, not thrown
		if (!valid) {
			support.send(sender, Message.RECONCILE_EXPIRED, CommandSupport.track(track));
			return;
		}
		pending.remove(key(sender));
		if (!waiting.payments().equals(payments)) {
			support.send(sender, Message.RECONCILE_CHANGED, CommandSupport.track(track));
			return;
		}
		final Bank bank = support.plugin().getBank();
		int paid = 0;
		double total = 0;
		for (final Payment payment : payments) {
			final OfflinePlayer player = support.plugin().getServer().getOfflinePlayer(payment.player());
			if (!bank.deposit(player, payment.amount())) {
				support.plugin().getLogger().warning("Reconciling " + track.getName() + ": could not pay "
						+ name(payment.player()) + " " + payment.amount() + " for " + payment.medal().key());
				continue;
			}
			support.plugin().getResults().recordPayout(track.getId(), payment.player(), payment.medal().key(),
					payment.amount(), Instant.now());
			support.plugin().getLogger().info("Reconciling " + track.getName() + " by " + sender.getName() + ": paid "
					+ name(payment.player()) + " " + payment.amount() + " for " + payment.medal().key());
			paid++;
			total += payment.amount();
		}
		support.send(sender, Message.RECONCILE_DONE, CommandSupport.text("count", paid),
				CommandSupport.text("total", bank.format(total)));
		if (paid < payments.size()) {
			support.send(sender, Message.RECONCILE_FAILED, CommandSupport.track(track),
					CommandSupport.text("count", payments.size() - paid));
		}
	}

	private static double total(@NotNull final List<Payment> payments) {
		return payments.stream().mapToDouble(Payment::amount).sum();
	}

	@NotNull
	private String name(@NotNull final UUID id) {
		final OfflinePlayer player = support.plugin().getServer().getOfflinePlayer(id);
		return player.getName() == null ? id.toString() : player.getName();
	}

	@NotNull
	private static String key(@NotNull final CommandSender sender) {
		return sender instanceof final Player player ? player.getUniqueId().toString() : CONSOLE;
	}
}
