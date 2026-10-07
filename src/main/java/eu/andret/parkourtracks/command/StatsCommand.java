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
	private static final String PLAYER = "player";

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
		final OfflinePlayer target = findTarget(sender, player);
		final String name = target.getName() == null ? String.valueOf(player) : target.getName();
		final ParkourTracksPlugin plugin = support.getPlugin();
		if (track == null) {
			plugin.getResults().fetchPlayerSummary(target.getUniqueId())
					.thenAcceptAsync(results -> sendSummary(sender, name, results), plugin::runOnMainThread);
			return;
		}
		plugin.getResults().fetchPlayerResult(track.getId(), target.getUniqueId())
				.thenCombine(plugin.getResults().fetchRanked(track.getId(), 1), (result, rankedRecord) -> {
					plugin.runOnMainThread(() -> sendTrack(sender, name, track, result.orElse(null), rankedRecord.orElse(null)));
					return null;
				});
	}

	@NotNull
	private OfflinePlayer findTarget(@NotNull final CommandSender sender, @Nullable final String name) {
		if (name == null) {
			if (sender instanceof final OfflinePlayer player) {
				return player;
			}
			throw new SenderNotPlayerException();
		}
		if (!sender.hasPermission(Permissions.STATS_OTHERS)) {
			throw support.fail(Message.STATS_OTHERS_DENIED);
		}
		final OfflinePlayer target = support.getPlugin().getServer().getOfflinePlayerIfCached(name);
		if (target == null) {
			throw support.fail(Message.PLAYER_NOT_FOUND, CommandSupport.createPlaceholder(PLAYER, name));
		}
		return target;
	}

	private void sendSummary(@NotNull final CommandSender sender, @NotNull final String name,
			@NotNull final List<ResultStore.TrackResult> results) {
		final TagResolver player = CommandSupport.createPlaceholder(PLAYER, name);
		// Results of removed tracks stay in the database, but there is nothing to show them by
		final List<ResultStore.TrackResult> shown = results.stream()
				.filter(result -> support.getRegistry().find(result.track()).isPresent())
				.sorted(Comparator.comparing(result -> support.getRegistry().find(result.track()).orElseThrow().getName()))
				.toList();
		if (shown.isEmpty()) {
			support.send(sender, Message.STATS_NONE, player);
			return;
		}
		support.send(sender, Message.STATS_SUMMARY_HEADER, player);
		shown.forEach(result -> support.send(sender, Message.STATS_SUMMARY_ENTRY,
				CommandSupport.createTrackPlaceholder(support.getRegistry().find(result.track()).orElseThrow()),
				CommandSupport.createPlaceholder("time", Ticks.format(result.bestTicks())),
				CommandSupport.createPlaceholder("count", result.completions())));
	}

	private void sendTrack(@NotNull final CommandSender sender, @NotNull final String name, @NotNull final Track track,
			@Nullable final ResultStore.PlayerResult result,
			@Nullable final ResultStore.Ranked rankedRecord) {
		final TagResolver player = CommandSupport.createPlaceholder(PLAYER, name);
		if (result == null) {
			support.send(sender, Message.STATS_NO_RESULT, player, CommandSupport.createTrackPlaceholder(track));
		} else {
			support.send(sender, Message.STATS_TRACK_HEADER, player, CommandSupport.createDisplayNamePlaceholder(track));
			support.send(sender, Message.STATS_BEST, CommandSupport.createPlaceholder("time", Ticks.format(result.bestTicks())));
			support.send(sender, Message.STATS_COMPLETIONS, CommandSupport.createPlaceholder("count", result.completions()));
			support.send(sender, Message.STATS_LAST, CommandSupport.createPlaceholder("date", DATE.format(result.lastFinished())));
			final List<Medal> medals = support.getPlugin().getSettings().medals();
			final Component medal = TrackRules.findBestMedal(medals, track.getMedals(), result.bestTicks())
					.map(earned -> MiniMessage.miniMessage().deserialize(earned.displayName()))
					.orElseGet(() -> support.getPlugin().getMessages().get(Message.VALUE_NONE));
			support.send(sender, Message.STATS_MEDAL, Placeholder.component("medal", medal));
		}
		if (rankedRecord == null) {
			support.send(sender, Message.STATS_NO_RECORD);
			return;
		}
		support.send(sender, Message.STATS_RECORD, CommandSupport.createPlaceholder("time", Ticks.format(rankedRecord.ticks())),
				CommandSupport.createPlaceholder("holder", getHolderName(rankedRecord.player())));
	}

	@NotNull
	private String getHolderName(@NotNull final UUID id) {
		final OfflinePlayer holder = support.getPlugin().getServer().getOfflinePlayer(id);
		final String name = holder.getName();
		if (name != null) {
			return name;
		}
		return id.toString();
	}
}
