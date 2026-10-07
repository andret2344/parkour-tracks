package eu.andret.parkourtracks.track;

import eu.andret.parkourtracks.util.Amounts;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.ObjDoubleConsumer;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.regex.Pattern;

/**
 * A setting of a track that {@code /ptracks option <track> <option> <value>} changes: how its value is parsed from the
 * command, checked against the other options, stored and shown.
 *
 * @param <T> the type of the value
 */
public final class TrackOption<T> {
	/**
	 * The value that clears an option which may be unset, such as the icon.
	 */
	public static final String NONE = "none";
	@NotNull
	private static final Pattern PERMISSION_NODE = Pattern.compile("[a-z0-9_.-]+");
	@NotNull
	private static final List<String> BOOLEANS = List.of("true", "false");

	@NotNull
	private final String name;
	@NotNull
	private final Function<String, T> parser;
	@NotNull
	private final Function<Track, T> getter;
	@NotNull
	private final BiConsumer<Track, T> setter;
	@NotNull
	private final Function<T, String> formatter;
	@NotNull
	private final List<String> suggestions;
	@NotNull
	private final Function<Track, Optional<String>> conflict;

	private TrackOption(@NotNull final String name, @NotNull final Function<String, T> parser,
			@NotNull final Function<Track, T> getter, @NotNull final BiConsumer<Track, T> setter,
			@NotNull final Function<T, String> formatter, @NotNull final List<String> suggestions,
			@NotNull final Function<Track, Optional<String>> conflict) {
		this.name = name;
		this.parser = parser;
		this.getter = getter;
		this.setter = setter;
		this.formatter = formatter;
		this.suggestions = List.copyOf(suggestions);
		this.conflict = conflict;
	}

	@NotNull
	private static Optional<String> skipConflictCheck(@NotNull final Track track) {
		return Optional.empty();
	}

	@NotNull
	private static TrackOption<Boolean> createFlag(@NotNull final String name, @NotNull final Predicate<TrackOptions> getter,
			@NotNull final BiConsumer<TrackOptions, Boolean> setter,
			@NotNull final Function<Track, Optional<String>> conflict) {
		return new TrackOption<>(name, TrackOption::parseBoolean, track -> getter.test(track.getOptions()),
				(track, value) -> setter.accept(track.getOptions(), value), String::valueOf, BOOLEANS,
				conflict);
	}

	@NotNull
	private static <E extends Enum<E>> TrackOption<E> createChoice(@NotNull final String name, @NotNull final Class<E> type,
			@NotNull final Function<Track, E> getter,
			@NotNull final BiConsumer<Track, E> setter) {
		final List<String> values = Arrays.stream(type.getEnumConstants())
				.map(value -> value.name().toLowerCase(Locale.ROOT))
				.toList();
		return new TrackOption<>(name, text -> parseChoice(text, values, type), getter, setter,
				value -> value.name().toLowerCase(Locale.ROOT), values, TrackOption::skipConflictCheck);
	}

	@NotNull
	private static TrackOption<Double> createAmount(@NotNull final String name, @NotNull final ToDoubleFunction<TrackOptions> getter,
			@NotNull final ObjDoubleConsumer<TrackOptions> setter) {
		return new TrackOption<>(name, TrackOption::parseAmount, track -> getter.applyAsDouble(track.getOptions()),
				(track, value) -> setter.accept(track.getOptions(), value), Amounts::format, List.of("0"),
				TrackOption::skipConflictCheck);
	}

	/**
	 * The boats a track can use: every boat and raft without a chest, which would add a container to the game.
	 */
	@NotNull
	public static List<EntityType> getBoatTypes() {
		return Arrays.stream(EntityType.values())
				.filter(type -> type.name().endsWith("_BOAT") || type.name().endsWith("_RAFT"))
				.filter(type -> !type.name().contains("CHEST"))
				.toList();
	}

	@NotNull
	public static final TrackOption<String> DISPLAY_NAME = new TrackOption<>("displayName", text -> text,
			Track::getDisplayName, Track::setDisplayName, text -> text, List.of(), TrackOption::skipConflictCheck);
	@NotNull
	public static final TrackOption<TrackType> TYPE = createChoice("type", TrackType.class, Track::getType, Track::setType);
	@NotNull
	public static final TrackOption<Boolean> SPRINT_FORCED = createFlag("sprintForced", TrackOptions::isSprintForced,
			TrackOptions::setSprintForced, TrackOption::findBoatConflict);
	@NotNull
	public static final TrackOption<Boolean> HARDCORE = createFlag("hardcore", TrackOptions::isHardcore,
			TrackOptions::setHardcore, TrackOption::skipConflictCheck);
	@NotNull
	public static final TrackOption<Boolean> DAMAGE_ALLOWED = createFlag("damageAllowed", TrackOptions::isDamageAllowed,
			TrackOptions::setDamageAllowed, TrackOption::skipConflictCheck);
	@NotNull
	public static final TrackOption<Boolean> BOAT = createFlag("boat", TrackOptions::isBoat, TrackOptions::setBoat,
			track -> {
				if (track.getOptions().isSprintForced()) {
					return Optional.of("sprintForced");
				}
				return track.getOptions().isEnderPearls() ? Optional.of("enderPearls") : Optional.empty();
			});
	@NotNull
	public static final TrackOption<EntityType> BOAT_TYPE = new TrackOption<>("boatType",
			text -> parseChoice(text, getBoatTypes().stream().map(type -> type.name().toLowerCase(Locale.ROOT)).toList(),
					EntityType.class),
			track -> track.getOptions().getBoatType(), (track, value) -> track.getOptions().setBoatType(value),
			value -> value.name().toLowerCase(Locale.ROOT),
			getBoatTypes().stream().map(type -> type.name().toLowerCase(Locale.ROOT)).toList(), TrackOption::skipConflictCheck);
	@NotNull
	public static final TrackOption<Boolean> PAUSE_ON_CHECKPOINTS = createFlag("pauseOnCheckpoints",
			TrackOptions::isPauseOnCheckpoints, TrackOptions::setPauseOnCheckpoints, TrackOption::skipConflictCheck);
	@NotNull
	public static final TrackOption<SkipMode> SKIP_MODE = createChoice("skipMode", SkipMode.class,
			track -> track.getOptions().getSkipMode(), (track, value) -> track.getOptions().setSkipMode(value));
	@NotNull
	public static final TrackOption<Boolean> ENDER_PEARLS = createFlag("enderPearls", TrackOptions::isEnderPearls,
			TrackOptions::setEnderPearls, TrackOption::findBoatConflict);
	@NotNull
	public static final TrackOption<Double> FEE = createAmount("fee", TrackOptions::getFee, TrackOptions::setFee);
	@NotNull
	public static final TrackOption<Double> REWARD = createAmount("reward", TrackOptions::getReward, TrackOptions::setReward);
	@NotNull
	public static final TrackOption<Integer> DIFFICULTY = new TrackOption<>("difficulty", TrackOption::parseDifficulty,
			track -> track.getOptions().getDifficulty(), (track, value) -> track.getOptions().setDifficulty(value),
			String::valueOf, List.of("1", "2", "3", "4", "5"), TrackOption::skipConflictCheck);
	@NotNull
	public static final TrackOption<Optional<Material>> ICON = new TrackOption<>("icon", TrackOption::parseIcon,
			track -> Optional.ofNullable(track.getOptions().getIcon()),
			(track, value) -> track.getOptions().setIcon(value.orElse(null)),
			value -> value.map(material -> material.name().toLowerCase(Locale.ROOT)).orElse(NONE), List.of(NONE),
			TrackOption::skipConflictCheck);
	@NotNull
	public static final TrackOption<Optional<String>> PERMISSION = new TrackOption<>("permission",
			TrackOption::parsePermission, track -> Optional.ofNullable(track.getOptions().getPermission()),
			(track, value) -> track.getOptions().setPermission(value.orElse(null)), value -> value.orElse(NONE),
			List.of(NONE), TrackOption::skipConflictCheck);
	@NotNull
	public static final TrackOption<AfterFinish> AFTER_FINISH = createChoice("afterFinish", AfterFinish.class,
			track -> track.getOptions().getAfterFinish(), (track, value) -> track.getOptions().setAfterFinish(value));

	/**
	 * Every option, in the order {@code /ptracks info} shows them.
	 */
	@NotNull
	public static final List<TrackOption<?>> ALL = List.of(DISPLAY_NAME, TYPE, DIFFICULTY, ICON, PERMISSION,
			FEE, REWARD, HARDCORE, SKIP_MODE, PAUSE_ON_CHECKPOINTS, SPRINT_FORCED, DAMAGE_ALLOWED, ENDER_PEARLS, BOAT,
			BOAT_TYPE, AFTER_FINISH);

	@NotNull
	public static Optional<TrackOption<?>> find(@NotNull final String name) {
		return ALL.stream()
				.filter(option -> option.name.equalsIgnoreCase(name))
				.findFirst();
	}

	@NotNull
	public String getName() {
		return name;
	}

	/**
	 * Values offered by tab completion; empty for free text.
	 */
	@NotNull
	public List<String> getSuggestions() {
		return suggestions;
	}

	/**
	 * The track's current value, as {@code /ptracks option} takes it.
	 */
	@NotNull
	public String formatValue(@NotNull final Track track) {
		return formatter.apply(getter.apply(track));
	}

	/**
	 * Parses the value and sets it to the track. Turning on an option that cannot go together with one the track has
	 * on is refused; nothing changes then.
	 *
	 * @throws OptionException when the value is invalid or conflicts with another option
	 */
	public void set(@NotNull final Track track, @NotNull final String text) {
		final T value = parser.apply(text.trim());
		if (!Boolean.FALSE.equals(value)) {
			final Optional<String> conflicting = conflict.apply(track);
			if (conflicting.isPresent()) {
				throw new OptionException(OptionException.Problem.CONFLICT, conflicting.get());
			}
		}
		setter.accept(track, value);
	}

	@NotNull
	private static Optional<String> findBoatConflict(@NotNull final Track track) {
		return track.getOptions().isBoat() ? Optional.of("boat") : Optional.empty();
	}

	private static boolean parseBoolean(@NotNull final String text) {
		if (text.equalsIgnoreCase("true")) {
			return true;
		}
		if (text.equalsIgnoreCase("false")) {
			return false;
		}
		throw new OptionException(OptionException.Problem.NOT_A_BOOLEAN, "");
	}

	@NotNull
	private static <E extends Enum<E>> E parseChoice(@NotNull final String text, @NotNull final List<String> values,
			@NotNull final Class<E> type) {
		final String normalized = text.toLowerCase(Locale.ROOT);
		if (!values.contains(normalized)) {
			throw new OptionException(OptionException.Problem.NOT_A_CHOICE, String.join(", ", values));
		}
		return Enum.valueOf(type, normalized.toUpperCase(Locale.ROOT));
	}

	private static double parseAmount(@NotNull final String text) {
		final double amount;
		try {
			amount = Double.parseDouble(text);
		} catch (final NumberFormatException _) {
			throw new OptionException(OptionException.Problem.NOT_A_NUMBER, "");
		}
		if (!Double.isFinite(amount) || amount < 0) {
			throw new OptionException(OptionException.Problem.OUT_OF_RANGE, ">= 0");
		}
		return amount;
	}

	private static int parseDifficulty(@NotNull final String text) {
		final int difficulty;
		try {
			difficulty = Integer.parseInt(text);
		} catch (final NumberFormatException _) {
			throw new OptionException(OptionException.Problem.NOT_A_NUMBER, "");
		}
		if (difficulty < TrackOptions.MIN_DIFFICULTY || difficulty > TrackOptions.MAX_DIFFICULTY) {
			throw new OptionException(OptionException.Problem.OUT_OF_RANGE,
					TrackOptions.MIN_DIFFICULTY + "-" + TrackOptions.MAX_DIFFICULTY);
		}
		return difficulty;
	}

	@NotNull
	private static Optional<Material> parseIcon(@NotNull final String text) {
		if (text.equalsIgnoreCase(NONE)) {
			return Optional.empty();
		}
		final Material material = Material.matchMaterial(text);
		if (material == null || !material.isItem() || material.isAir()) {
			throw new OptionException(OptionException.Problem.NOT_A_CHOICE, NONE + ", an item");
		}
		return Optional.of(material);
	}

	@NotNull
	private static Optional<String> parsePermission(@NotNull final String text) {
		if (text.equalsIgnoreCase(NONE)) {
			return Optional.empty();
		}
		final String permission = text.toLowerCase(Locale.ROOT);
		if (!PERMISSION_NODE.matcher(permission).matches()) {
			throw new OptionException(OptionException.Problem.NOT_A_PERMISSION, "");
		}
		return Optional.of(permission);
	}

	@Override
	@NotNull
	public String toString() {
		return name;
	}
}
