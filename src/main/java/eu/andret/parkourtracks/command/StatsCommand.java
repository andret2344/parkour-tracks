package eu.andret.parkourtracks.command;

import eu.andret.parkourtracks.ParkourTracksPlugin;
import eu.andret.parkourtracks.config.Medal;
import eu.andret.parkourtracks.message.Message;
import eu.andret.parkourtracks.result.ResultStore;
import eu.andret.parkourtracks.track.Track;
import eu.andret.parkourtracks.track.TrackRules;
import eu.andret.parkourtracks.util.Ticks;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Description;
import revxrsal.commands.annotation.Optional;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;
import revxrsal.commands.bukkit.exception.SenderNotPlayerException;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * {@code /ptracks stats}: a player's results, the sender's own unless another player is named.
 */
@Command({"parkourtracks", "ptracks"})
public final class StatsCommand {
	@NotNull
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ROOT)
			.withZone(ZoneId.systemDefault());

	@NotNull
	private final CommandSupport support;

	public StatsCommand(@NotNull final CommandSupport support) {
		this.support = support;
	}

	@Subcommand("stats")
	@Description("Shows your results on all tracks or on one, or those of another player")
	@CommandPermission(Permissions.PLAY)
	public void stats(@NotNull final CommandSender sender, @Optional @Nullable final Track track,
					  @Optional @Nullable final String player) {
		final OfflinePlayer target = target(sender, player);
		final String name = target.getName() == null ? String.valueOf(player) : target.getName();
		final ParkourTracksPlugin plugin = support.plugin();
		if (track == null) {
			plugin.getResults().playerSummary(target.getUniqueId())
					.thenAcceptAsync(results -> sendSummary(sender, name, results), plugin::runOnMainThread);
			return;
		}
		plugin.getResults().playerResult(track.getId(), target.getUniqueId())
				.thenCombine(plugin.getResults().ranked(track.getId(), 1), (result, record) -> {
					plugin.runOnMainThread(() -> sendTrack(sender, name, track, result, record));
					return null;
				});
	}

	@NotNull
	private OfflinePlayer target(@NotNull final CommandSender sender, @Nullable final String name) {
		if (name == null) {
			if (sender instanceof final OfflinePlayer player) {
				return player;
			}
			throw new SenderNotPlayerException();
		}
		if (!sender.hasPermission(Permissions.STATS_OTHERS)) {
			throw support.fail(Message.STATS_OTHERS_DENIED);
		}
		final OfflinePlayer target = support.plugin().getServer().getOfflinePlayerIfCached(name);
		if (target == null) {
			throw support.fail(Message.PLAYER_NOT_FOUND, CommandSupport.text("player", name));
		}
		return target;
	}

	private void sendSummary(@NotNull final CommandSender sender, @NotNull final String name,
							 @NotNull final List<ResultStore.TrackResult> results) {
		final TagResolver player = CommandSupport.text("player", name);
		// Results of removed tracks stay in the database, but there is nothing to show them by
		final List<ResultStore.TrackResult> shown = results.stream()
				.filter(result -> support.registry().find(result.track()).isPresent())
				.sorted(Comparator.comparing(result -> support.registry().find(result.track()).orElseThrow().getName()))
				.toList();
		if (shown.isEmpty()) {
			support.send(sender, Message.STATS_NONE, player);
			return;
		}
		support.send(sender, Message.STATS_SUMMARY_HEADER, player);
		shown.forEach(result -> support.send(sender, Message.STATS_SUMMARY_ENTRY,
				CommandSupport.track(support.registry().find(result.track()).orElseThrow()),
				CommandSupport.text("time", Ticks.format(result.bestTicks())),
				CommandSupport.text("count", result.completions())));
	}

	private void sendTrack(@NotNull final CommandSender sender, @NotNull final String name, @NotNull final Track track,
						   @NotNull final java.util.Optional<ResultStore.PlayerResult> result,
						   @NotNull final java.util.Optional<ResultStore.Ranked> record) {
		final TagResolver player = CommandSupport.text("player", name);
		if (result.isEmpty()) {
			support.send(sender, Message.STATS_NO_RESULT, player, CommandSupport.track(track));
		} else {
			final ResultStore.PlayerResult found = result.get();
			support.send(sender, Message.STATS_TRACK_HEADER, player, CommandSupport.displayName(track));
			support.send(sender, Message.STATS_BEST, CommandSupport.text("time", Ticks.format(found.bestTicks())));
			support.send(sender, Message.STATS_COMPLETIONS, CommandSupport.text("count", found.completions()));
			support.send(sender, Message.STATS_LAST, CommandSupport.text("date", DATE.format(found.lastFinished())));
			final List<Medal> medals = support.plugin().getSettings().medals();
			final Component medal = TrackRules.bestMedal(medals, track.getMedals(), found.bestTicks())
					.map(earned -> MiniMessage.miniMessage().deserialize(earned.displayName()))
					.orElseGet(() -> support.plugin().getMessages().get(Message.VALUE_NONE));
			support.send(sender, Message.STATS_MEDAL, Placeholder.component("medal", medal));
		}
		if (record.isEmpty()) {
			support.send(sender, Message.STATS_NO_RECORD);
			return;
		}
		support.send(sender, Message.STATS_RECORD, CommandSupport.text("time", Ticks.format(record.get().ticks())),
				CommandSupport.text("holder", holderName(record.get().player())));
	}

	@NotNull
	private String holderName(@NotNull final UUID id) {
		final OfflinePlayer holder = support.plugin().getServer().getOfflinePlayer(id);
		return holder.getName() == null ? id.toString() : holder.getName();
	}
}
