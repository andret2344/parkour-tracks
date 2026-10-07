package eu.andret.parkourtracks.command;

import eu.andret.parkourtracks.config.Medal;
import eu.andret.parkourtracks.message.Message;
import eu.andret.parkourtracks.track.MedalThreshold;
import eu.andret.parkourtracks.track.OptionException;
import eu.andret.parkourtracks.track.Track;
import eu.andret.parkourtracks.track.TrackEffect;
import eu.andret.parkourtracks.track.TrackOption;
import eu.andret.parkourtracks.track.TrackRules;
import eu.andret.parkourtracks.track.TrackType;
import eu.andret.parkourtracks.util.Amounts;
import eu.andret.parkourtracks.util.Ticks;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Registry;
import org.bukkit.command.CommandSender;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Description;
import revxrsal.commands.annotation.Range;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

/**
 * The options, medals, effects and authors of a track.
 */
@Command({"parkourtracks", "ptracks"})
public final class TrackSettingsCommand {
	private static final int MAX_AMPLIFIER = 255;
	private static final String EFFECT = "effect";

	@NotNull
	private final CommandSupport support;

	public TrackSettingsCommand(@NotNull final CommandSupport support) {
		this.support = support;
	}

	@Subcommand("set")
	@Description("Sets an option of a track; /ptracks info lists them")
	@CommandPermission(Permissions.EDIT)
	public void set(@NotNull final CommandSender sender, @NotNull final Track track, @NotNull final TrackOption<?> option,
			@OptionValue @NotNull final String value) {
		support.requireStopped(track);
		try {
			option.set(track, value);
		} catch (final OptionException ex) {
			throw createOptionFailure(option, ex);
		}
		support.save();
		if (option == TrackOption.DISPLAY_NAME) {
			support.getPlugin().getRecordSigns().refresh(track.getId());
		}
		support.send(sender, Message.OPTION_SET, CommandSupport.createTrackPlaceholder(track),
				CommandSupport.createPlaceholder("option", option.getName()), CommandSupport.createPlaceholder("value", option.formatValue(track)));
	}

	@Subcommand("medal time")
	@Description("Sets the time in seconds a run must beat for a medal on a track")
	@CommandPermission(Permissions.EDIT)
	public void medalTime(@NotNull final CommandSender sender, @NotNull final Track track, @NotNull final Medal medal,
			@NotNull final String seconds) {
		support.requireStopped(track);
		final OptionalInt ticks = Ticks.parseSeconds(seconds);
		if (ticks.isEmpty()) {
			throw support.fail(Message.MEDAL_TIME_INVALID);
		}
		TrackRules.findMedalOrderClash(support.getPlugin().getSettings().medals(), track.getMedals(), medal, ticks.getAsInt())
				.ifPresent(other -> {
					throw support.fail(Message.MEDAL_ORDER, createMedalPlaceholder(other));
				});
		track.setMedal(medal.key(), new MedalThreshold(ticks.getAsInt(), findThreshold(track, medal).reward()));
		support.save();
		support.send(sender, Message.MEDAL_TIME_SET, CommandSupport.createTrackPlaceholder(track), createMedalPlaceholder(medal),
				CommandSupport.createPlaceholder("time", Ticks.format(ticks.getAsInt())));
	}

	@Subcommand("medal reward")
	@Description("Sets the reward paid once for a medal on a track")
	@CommandPermission(Permissions.EDIT)
	public void medalReward(@NotNull final CommandSender sender, @NotNull final Track track, @NotNull final Medal medal,
			@NotNull final String reward) {
		support.requireStopped(track);
		final double amount;
		try {
			amount = Double.parseDouble(reward);
		} catch (final NumberFormatException _) {
			throw support.fail(Message.MEDAL_REWARD_INVALID);
		}
		if (!Double.isFinite(amount) || amount < 0) {
			throw support.fail(Message.MEDAL_REWARD_INVALID);
		}
		track.setMedal(medal.key(), new MedalThreshold(findThreshold(track, medal).ticks(), amount));
		support.save();
		support.send(sender, Message.MEDAL_REWARD_SET, CommandSupport.createTrackPlaceholder(track), createMedalPlaceholder(medal),
				CommandSupport.createPlaceholder("reward", Amounts.format(amount)));
	}

	@Subcommand("medal remove")
	@Description("Removes the time and the reward of a medal from a track, which then does not award it")
	@CommandPermission(Permissions.EDIT)
	public void medalRemove(@NotNull final CommandSender sender, @NotNull final Track track, @NotNull final Medal medal) {
		support.requireStopped(track);
		track.removeMedal(medal.key());
		support.save();
		support.send(sender, Message.MEDAL_REMOVED, CommandSupport.createTrackPlaceholder(track), createMedalPlaceholder(medal));
	}

	@Subcommand("effect set")
	@Description("Gives players on a track a potion effect for the whole game; level 1 is the weakest")
	@CommandPermission(Permissions.EDIT)
	public void effectSet(@NotNull final CommandSender sender, @NotNull final Track track,
			@EffectType @NotNull final String effect, @Range(min = 1, max = MAX_AMPLIFIER + 1) final int level) {
		support.requireStopped(track);
		final PotionEffectType type = findEffectType(effect);
		final String key = type.getKey().asString();
		track.setEffect(new TrackEffect(key, level - 1));
		support.save();
		support.send(sender, Message.EFFECT_SET, CommandSupport.createTrackPlaceholder(track), CommandSupport.createPlaceholder(EFFECT, key),
				CommandSupport.createPlaceholder("level", level));
	}

	@Subcommand("effect remove")
	@Description("Takes a potion effect off a track")
	@CommandPermission(Permissions.EDIT)
	public void effectRemove(@NotNull final CommandSender sender, @NotNull final Track track,
			@EffectType @NotNull final String effect) {
		support.requireStopped(track);
		final String key = findEffectType(effect).getKey().asString();
		if (track.getEffects().stream().noneMatch(trackEffect -> trackEffect.type().equals(key))) {
			throw support.fail(Message.EFFECT_NOT_ON_TRACK, CommandSupport.createTrackPlaceholder(track), CommandSupport.createPlaceholder(EFFECT, key));
		}
		track.removeEffect(key);
		support.save();
		support.send(sender, Message.EFFECT_REMOVED, CommandSupport.createTrackPlaceholder(track), CommandSupport.createPlaceholder(EFFECT, key));
	}

	@Subcommand("author add")
	@Description("Adds an author to a track of type players")
	@CommandPermission(Permissions.EDIT)
	public void authorAdd(@NotNull final CommandSender sender, @NotNull final Track track, @NotNull final String player) {
		support.requireStopped(track);
		final OfflinePlayer author = findAuthor(track, player);
		if (track.getAuthors().contains(author.getUniqueId())) {
			throw support.fail(Message.AUTHOR_ALREADY, CommandSupport.createTrackPlaceholder(track), createPlayerPlaceholder(author, player));
		}
		final List<UUID> authors = new ArrayList<>(track.getAuthors());
		authors.add(author.getUniqueId());
		track.setAuthors(authors);
		support.save();
		support.send(sender, Message.AUTHOR_ADDED, CommandSupport.createTrackPlaceholder(track), createPlayerPlaceholder(author, player));
	}

	@Subcommand("author remove")
	@Description("Removes an author from a track")
	@CommandPermission(Permissions.EDIT)
	public void authorRemove(@NotNull final CommandSender sender, @NotNull final Track track,
			@NotNull final String player) {
		support.requireStopped(track);
		final OfflinePlayer author = findAuthor(track, player);
		final List<UUID> authors = new ArrayList<>(track.getAuthors());
		if (!authors.remove(author.getUniqueId())) {
			throw support.fail(Message.AUTHOR_NOT_AUTHOR, CommandSupport.createTrackPlaceholder(track), createPlayerPlaceholder(author, player));
		}
		track.setAuthors(authors);
		support.save();
		support.send(sender, Message.AUTHOR_REMOVED, CommandSupport.createTrackPlaceholder(track), createPlayerPlaceholder(author, player));
	}

	/**
	 * Every potion effect type, by key, for completion.
	 */
	@NotNull
	public static List<String> getEffectKeys() {
		return Registry.EFFECT.stream()
				.map(type -> type.getKey().getKey())
				.sorted()
				.toList();
	}

	@NotNull
	private MessageException createOptionFailure(@NotNull final TrackOption<?> option, @NotNull final OptionException ex) {
		final TagResolver name = CommandSupport.createPlaceholder("option", option.getName());
		return switch (ex.getProblem()) {
			case NOT_A_BOOLEAN -> support.fail(Message.OPTION_NOT_A_BOOLEAN, name);
			case NOT_A_NUMBER -> support.fail(Message.OPTION_NOT_A_NUMBER, name);
			case OUT_OF_RANGE -> support.fail(Message.OPTION_OUT_OF_RANGE, name, CommandSupport.createPlaceholder("range", ex.getDetail()));
			case NOT_A_CHOICE -> support.fail(Message.OPTION_NOT_A_CHOICE, name, CommandSupport.createPlaceholder("choices", ex.getDetail()));
			case NOT_A_PERMISSION -> support.fail(Message.OPTION_NOT_A_PERMISSION, name);
			case CONFLICT -> support.fail(Message.OPTION_CONFLICT, name, CommandSupport.createPlaceholder("other", ex.getDetail()));
		};
	}

	@NotNull
	private static MedalThreshold findThreshold(@NotNull final Track track, @NotNull final Medal medal) {
		final MedalThreshold threshold = track.getMedals().get(medal.key());
		return Optional.ofNullable(threshold)
				.orElseGet(() -> new MedalThreshold(0, 0));
	}

	@NotNull
	private static TagResolver createMedalPlaceholder(@NotNull final Medal medal) {
		return Placeholder.parsed("medal", medal.displayName());
	}

	@NotNull
	private PotionEffectType findEffectType(@NotNull final String name) {
		final NamespacedKey key = NamespacedKey.fromString(name.toLowerCase(Locale.ROOT));
		final PotionEffectType type = key == null ? null : Registry.EFFECT.get(key);
		if (type == null) {
			throw support.fail(Message.EFFECT_NOT_FOUND, CommandSupport.createPlaceholder(EFFECT, name));
		}
		return type;
	}

	/**
	 * Finds a player who has played on the server; only tracks of type players have authors.
	 */
	@NotNull
	private OfflinePlayer findAuthor(@NotNull final Track track, @NotNull final String name) {
		if (track.getType() != TrackType.PLAYERS) {
			throw support.fail(Message.AUTHOR_NOT_PLAYERS_TYPE, CommandSupport.createTrackPlaceholder(track));
		}
		final OfflinePlayer player = support.getPlugin().getServer().getOfflinePlayerIfCached(name);
		if (player == null) {
			throw support.fail(Message.AUTHOR_UNKNOWN_PLAYER, CommandSupport.createPlaceholder("player", name));
		}
		return player;
	}

	@NotNull
	private static TagResolver createPlayerPlaceholder(@NotNull final OfflinePlayer player, @NotNull final String typed) {
		final String name = player.getName();
		return CommandSupport.createPlaceholder("player", Optional.ofNullable(name).orElse(typed));
	}
}
