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
import java.util.OptionalInt;
import java.util.UUID;

/**
 * The options, medals, effects and authors of a track.
 */
@Command({"parkourtracks", "ptracks"})
public final class TrackSettingsCommand {
	private static final int MAX_AMPLIFIER = 255;

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
			throw optionFailure(option, ex);
		}
		support.save();
		if (option == TrackOption.DISPLAY_NAME) {
			support.plugin().getRecordSigns().refresh(track.getId());
		}
		support.send(sender, Message.OPTION_SET, CommandSupport.track(track),
				CommandSupport.text("option", option.getName()), CommandSupport.text("value", option.display(track)));
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
		TrackRules.findMedalOrderClash(support.plugin().getSettings().medals(), track.getMedals(), medal, ticks.getAsInt())
				.ifPresent(other -> {
					throw support.fail(Message.MEDAL_ORDER, medal(other));
				});
		track.setMedal(medal.key(), new MedalThreshold(ticks.getAsInt(), threshold(track, medal).reward()));
		support.save();
		support.send(sender, Message.MEDAL_TIME_SET, CommandSupport.track(track), medal(medal),
				CommandSupport.text("time", Ticks.format(ticks.getAsInt())));
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
		} catch (final NumberFormatException ex) {
			throw support.fail(Message.MEDAL_REWARD_INVALID);
		}
		if (!Double.isFinite(amount) || amount < 0) {
			throw support.fail(Message.MEDAL_REWARD_INVALID);
		}
		track.setMedal(medal.key(), new MedalThreshold(threshold(track, medal).ticks(), amount));
		support.save();
		support.send(sender, Message.MEDAL_REWARD_SET, CommandSupport.track(track), medal(medal),
				CommandSupport.text("reward", Amounts.format(amount)));
	}

	@Subcommand("medal remove")
	@Description("Removes the time and the reward of a medal from a track, which then does not award it")
	@CommandPermission(Permissions.EDIT)
	public void medalRemove(@NotNull final CommandSender sender, @NotNull final Track track, @NotNull final Medal medal) {
		support.requireStopped(track);
		track.removeMedal(medal.key());
		support.save();
		support.send(sender, Message.MEDAL_REMOVED, CommandSupport.track(track), medal(medal));
	}

	@Subcommand("effect set")
	@Description("Gives players on a track a potion effect for the whole game; level 1 is the weakest")
	@CommandPermission(Permissions.EDIT)
	public void effectSet(@NotNull final CommandSender sender, @NotNull final Track track,
						  @EffectType @NotNull final String effect, @Range(min = 1, max = MAX_AMPLIFIER + 1) final int level) {
		support.requireStopped(track);
		final PotionEffectType type = effectType(effect);
		final String key = type.getKey().asString();
		track.setEffect(new TrackEffect(key, level - 1));
		support.save();
		support.send(sender, Message.EFFECT_SET, CommandSupport.track(track), CommandSupport.text("effect", key),
				CommandSupport.text("level", level));
	}

	@Subcommand("effect remove")
	@Description("Takes a potion effect off a track")
	@CommandPermission(Permissions.EDIT)
	public void effectRemove(@NotNull final CommandSender sender, @NotNull final Track track,
							 @EffectType @NotNull final String effect) {
		support.requireStopped(track);
		final String key = effectType(effect).getKey().asString();
		if (track.getEffects().stream().noneMatch(trackEffect -> trackEffect.type().equals(key))) {
			throw support.fail(Message.EFFECT_NOT_ON_TRACK, CommandSupport.track(track), CommandSupport.text("effect", key));
		}
		track.removeEffect(key);
		support.save();
		support.send(sender, Message.EFFECT_REMOVED, CommandSupport.track(track), CommandSupport.text("effect", key));
	}

	@Subcommand("author add")
	@Description("Adds an author to a track of type players")
	@CommandPermission(Permissions.EDIT)
	public void authorAdd(@NotNull final CommandSender sender, @NotNull final Track track, @NotNull final String player) {
		support.requireStopped(track);
		final OfflinePlayer author = author(track, player);
		if (track.getAuthors().contains(author.getUniqueId())) {
			throw support.fail(Message.AUTHOR_ALREADY, CommandSupport.track(track), playerName(author, player));
		}
		final List<UUID> authors = new ArrayList<>(track.getAuthors());
		authors.add(author.getUniqueId());
		track.setAuthors(authors);
		support.save();
		support.send(sender, Message.AUTHOR_ADDED, CommandSupport.track(track), playerName(author, player));
	}

	@Subcommand("author remove")
	@Description("Removes an author from a track")
	@CommandPermission(Permissions.EDIT)
	public void authorRemove(@NotNull final CommandSender sender, @NotNull final Track track,
							 @NotNull final String player) {
		support.requireStopped(track);
		final OfflinePlayer author = author(track, player);
		final List<UUID> authors = new ArrayList<>(track.getAuthors());
		if (!authors.remove(author.getUniqueId())) {
			throw support.fail(Message.AUTHOR_NOT_AUTHOR, CommandSupport.track(track), playerName(author, player));
		}
		track.setAuthors(authors);
		support.save();
		support.send(sender, Message.AUTHOR_REMOVED, CommandSupport.track(track), playerName(author, player));
	}

	/**
	 * Every potion effect type, by key, for completion.
	 */
	@NotNull
	public static List<String> effectKeys() {
		return Registry.EFFECT.stream()
				.map(type -> type.getKey().getKey())
				.sorted()
				.toList();
	}

	@NotNull
	private MessageException optionFailure(@NotNull final TrackOption<?> option, @NotNull final OptionException ex) {
		final TagResolver name = CommandSupport.text("option", option.getName());
		return switch (ex.getProblem()) {
			case NOT_A_BOOLEAN -> support.fail(Message.OPTION_NOT_A_BOOLEAN, name);
			case NOT_A_NUMBER -> support.fail(Message.OPTION_NOT_A_NUMBER, name);
			case OUT_OF_RANGE -> support.fail(Message.OPTION_OUT_OF_RANGE, name, CommandSupport.text("range", ex.getDetail()));
			case NOT_A_CHOICE -> support.fail(Message.OPTION_NOT_A_CHOICE, name, CommandSupport.text("choices", ex.getDetail()));
			case NOT_A_PERMISSION -> support.fail(Message.OPTION_NOT_A_PERMISSION, name);
			case CONFLICT -> support.fail(Message.OPTION_CONFLICT, name, CommandSupport.text("other", ex.getDetail()));
		};
	}

	@NotNull
	private static MedalThreshold threshold(@NotNull final Track track, @NotNull final Medal medal) {
		final MedalThreshold threshold = track.getMedals().get(medal.key());
		return threshold == null ? new MedalThreshold(0, 0) : threshold;
	}

	@NotNull
	private static TagResolver medal(@NotNull final Medal medal) {
		return Placeholder.parsed("medal", medal.displayName());
	}

	@NotNull
	private PotionEffectType effectType(@NotNull final String name) {
		final NamespacedKey key = NamespacedKey.fromString(name.toLowerCase(Locale.ROOT));
		final PotionEffectType type = key == null ? null : Registry.EFFECT.get(key);
		if (type == null) {
			throw support.fail(Message.EFFECT_NOT_FOUND, CommandSupport.text("effect", name));
		}
		return type;
	}

	/**
	 * Finds a player who has played on the server; only tracks of type players have authors.
	 */
	@NotNull
	private OfflinePlayer author(@NotNull final Track track, @NotNull final String name) {
		if (track.getType() != TrackType.PLAYERS) {
			throw support.fail(Message.AUTHOR_NOT_PLAYERS_TYPE, CommandSupport.track(track));
		}
		final OfflinePlayer player = support.plugin().getServer().getOfflinePlayerIfCached(name);
		if (player == null) {
			throw support.fail(Message.AUTHOR_UNKNOWN_PLAYER, CommandSupport.text("player", name));
		}
		return player;
	}

	@NotNull
	private static TagResolver playerName(@NotNull final OfflinePlayer player, @NotNull final String typed) {
		return CommandSupport.text("player", player.getName() == null ? typed : player.getName());
	}
}
