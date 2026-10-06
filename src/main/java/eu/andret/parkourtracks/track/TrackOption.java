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
import java.util.regex.Pattern;

/**
 * A setting of a track that {@code /ptracks set <track> <option> <value>} changes: how its value is parsed from the
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
	private static Optional<String> noConflict(@NotNull final Track track) {
		return Optional.empty();
	}

	@NotNull
	private static TrackOption<Boolean> flag(@NotNull final String name, @NotNull final Function<TrackOptions, Boolean> getter,
											 @NotNull final BiConsumer<TrackOptions, Boolean> setter,
											 @NotNull final Function<Track, Optional<String>> conflict) {
		return new TrackOption<>(name, TrackOption::parseBoolean, track -> getter.apply(track.getOptions()),
				(track, value) -> setter.accept(track.getOptions(), value), String::valueOf, BOOLEANS,
				conflict);
	}

	@NotNull
	private static <E extends Enum<E>> TrackOption<E> choice(@NotNull final String name, @NotNull final Class<E> type,
															  @NotNull final Function<Track, E> getter,
															  @NotNull final BiConsumer<Track, E> setter) {
		final List<String> values = Arrays.stream(type.getEnumConstants())
				.map(value -> value.name().toLowerCase(Locale.ROOT))
				.toList();
		return new TrackOption<>(name, text -> parseChoice(text, values, type), getter, setter,
				value -> value.name().toLowerCase(Locale.ROOT), values, TrackOption::noConflict);
	}

	@NotNull
	private static TrackOption<Double> amount(@NotNull final String name, @NotNull final Function<TrackOptions, Double> getter,
											  @NotNull final BiConsumer<TrackOptions, Double> setter) {
		return new TrackOption<>(name, TrackOption::parseAmount, track -> getter.apply(track.getOptions()),
				(track, value) -> setter.accept(track.getOptions(), value), Amounts::format, List.of("0"),
				TrackOption::noConflict);
	}

	/**
	 * The boats a track can use: every boat and raft without a chest, which would add a container to the game.
	 */
	@NotNull
	public static List<EntityType> boatTypes() {
		return Arrays.stream(EntityType.values())
				.filter(type -> type.name().endsWith("_BOAT") || type.name().endsWith("_RAFT"))
				.filter(type -> !type.name().contains("CHEST"))
				.toList();
	}

	@NotNull
	public static final TrackOption<String> DISPLAY_NAME = new TrackOption<>("displayName", text -> text,
			Track::getDisplayName, Track::setDisplayName, text -> text, List.of(), TrackOption::noConflict);
	@NotNull
	public static final TrackOption<TrackType> TYPE = choice("type", TrackType.class, Track::getType, Track::setType);
	@NotNull
	public static final TrackOption<Boolean> SPRINT_FORCED = flag("sprintForced", TrackOptions::isSprintForced,
			TrackOptions::setSprintForced, track -> conflictWithBoat(track));
	@NotNull
	public static final TrackOption<Boolean> HARDCORE = flag("hardcore", TrackOptions::isHardcore,
			TrackOptions::setHardcore, TrackOption::noConflict);
	@NotNull
	public static final TrackOption<Boolean> DAMAGE_ALLOWED = flag("damageAllowed", TrackOptions::isDamageAllowed,
			TrackOptions::setDamageAllowed, TrackOption::noConflict);
	@NotNull
	public static final TrackOption<Boolean> BOAT = flag("boat", TrackOptions::isBoat, TrackOptions::setBoat,
			track -> {
				if (track.getOptions().isSprintForced()) {
					return Optional.of("sprintForced");
				}
				return track.getOptions().isEnderPearls() ? Optional.of("enderPearls") : Optional.empty();
			});
	@NotNull
	public static final TrackOption<EntityType> BOAT_TYPE = new TrackOption<>("boatType",
			text -> parseChoice(text, boatTypes().stream().map(type -> type.name().toLowerCase(Locale.ROOT)).toList(),
					EntityType.class),
			track -> track.getOptions().getBoatType(), (track, value) -> track.getOptions().setBoatType(value),
			value -> value.name().toLowerCase(Locale.ROOT),
			boatTypes().stream().map(type -> type.name().toLowerCase(Locale.ROOT)).toList(), TrackOption::noConflict);
	@NotNull
	public static final TrackOption<Boolean> PAUSE_ON_CHECKPOINTS = flag("pauseOnCheckpoints",
			TrackOptions::isPauseOnCheckpoints, TrackOptions::setPauseOnCheckpoints, TrackOption::noConflict);
	@NotNull
	public static final TrackOption<SkipMode> SKIP_MODE = choice("skipMode", SkipMode.class,
			track -> track.getOptions().getSkipMode(), (track, value) -> track.getOptions().setSkipMode(value));
	@NotNull
	public static final TrackOption<Boolean> ENDER_PEARLS = flag("enderPearls", TrackOptions::isEnderPearls,
			TrackOptions::setEnderPearls, track -> conflictWithBoat(track));
	@NotNull
	public static final TrackOption<Double> FEE = amount("fee", TrackOptions::getFee, TrackOptions::setFee);
	@NotNull
	public static final TrackOption<Double> REWARD = amount("reward", TrackOptions::getReward, TrackOptions::setReward);
	@NotNull
	public static final TrackOption<Integer> DIFFICULTY = new TrackOption<>("difficulty", TrackOption::parseDifficulty,
			track -> track.getOptions().getDifficulty(), (track, value) -> track.getOptions().setDifficulty(value),
			String::valueOf, List.of("1", "2", "3", "4", "5"), TrackOption::noConflict);
	@NotNull
	public static final TrackOption<Optional<Material>> ICON = new TrackOption<>("icon", TrackOption::parseIcon,
			track -> Optional.ofNullable(track.getOptions().getIcon()),
			(track, value) -> track.getOptions().setIcon(value.orElse(null)),
			value -> value.map(material -> material.name().toLowerCase(Locale.ROOT)).orElse(NONE), List.of(NONE),
			TrackOption::noConflict);
	@NotNull
	public static final TrackOption<Optional<String>> PERMISSION = new TrackOption<>("permission",
			TrackOption::parsePermission, track -> Optional.ofNullable(track.getOptions().getPermission()),
			(track, value) -> track.getOptions().setPermission(value.orElse(null)), value -> value.orElse(NONE),
			List.of(NONE), TrackOption::noConflict);
	@NotNull
	public static final TrackOption<AfterFinish> AFTER_FINISH = choice("afterFinish", AfterFinish.class,
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
	 * The track's current value, as {@code /ptracks set} takes it.
	 */
	@NotNull
	public String display(@NotNull final Track track) {
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
	private static Optional<String> conflictWithBoat(@NotNull final Track track) {
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
		} catch (final NumberFormatException ex) {
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
		} catch (final NumberFormatException ex) {
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
