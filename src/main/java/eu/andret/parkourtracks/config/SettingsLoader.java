package eu.andret.parkourtracks.config;

import eu.andret.parkourtracks.game.GameItem;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Turns {@code config.yml} into {@link Settings}. An invalid config throws {@link IllegalArgumentException} with the
 * path of what is wrong.
 */
public final class SettingsLoader {
	private static final String MEDALS = "medals";
	private static final String FINISH_DELAY = "finish-delay";
	private static final String TIMER_DISPLAY = "timer-display";
	private static final int DEFAULT_FINISH_DELAY = 5;
	private static final String SPRINT_GRACE = "sprint-grace-ticks";
	private static final int DEFAULT_SPRINT_GRACE = 5;
	private static final String GAME_ITEMS = "game-items";
	private static final String SCOREBOARD = "scoreboard";
	private static final String BACKUP_FREQUENCY = "backup-frequency";
	private static final int DEFAULT_BACKUP_FREQUENCY = 1440;
	private static final String BACKUP_KEEP = "backup-keep";
	private static final int DEFAULT_BACKUP_KEEP = 10;
	private static final int HOTBAR_SIZE = 9;
	@NotNull
	private static final Map<GameItem, GameItemSlot> DEFAULT_GAME_ITEMS = Map.of(
			GameItem.BACK, new GameItemSlot(GameItem.BACK, 0, Material.SLIME_BALL),
			GameItem.RESTART, new GameItemSlot(GameItem.RESTART, 1, Material.CLOCK),
			GameItem.HIDE, new GameItemSlot(GameItem.HIDE, 7, Material.ENDER_EYE),
			GameItem.EXIT, new GameItemSlot(GameItem.EXIT, 8, Material.RED_BED));
	@NotNull
	private static final Pattern MEDAL_KEY = Pattern.compile("[a-z0-9_-]+");

	private SettingsLoader() {
	}

	@NotNull
	public static Settings load(@NotNull final ConfigurationSection config) {
		return new Settings(loadMedals(config),
				loadNonNegative(config, FINISH_DELAY, DEFAULT_FINISH_DELAY, "a whole number of seconds"),
				loadTimerDisplay(config), loadNonNegative(config, SPRINT_GRACE, DEFAULT_SPRINT_GRACE, "a whole number of ticks"),
				loadGameItems(config), config.getBoolean(SCOREBOARD, true),
				loadNonNegative(config, BACKUP_FREQUENCY, DEFAULT_BACKUP_FREQUENCY, "a whole number of minutes"),
				loadNonNegative(config, BACKUP_KEEP, DEFAULT_BACKUP_KEEP, "a whole number"),
				config.getBoolean("refund-on-stop", true));
	}

	private static int loadNonNegative(@NotNull final ConfigurationSection config, @NotNull final String path,
									   final int defaultValue, @NotNull final String what) {
		if (!config.contains(path)) {
			return defaultValue;
		}
		if (!config.isInt(path) || config.getInt(path) < 0) {
			throw new IllegalArgumentException("'" + path + "' in config.yml has to be " + what + ", 0 or more");
		}
		return config.getInt(path);
	}

	/**
	 * Every game item, with its default slot and material for what the config leaves out; turned off ones are left
	 * out of the list.
	 */
	@NotNull
	private static List<GameItemSlot> loadGameItems(@NotNull final ConfigurationSection config) {
		final List<GameItemSlot> items = new ArrayList<>();
		final Set<Integer> slots = new HashSet<>();
		for (final GameItem item : GameItem.values()) {
			final String path = GAME_ITEMS + "." + item.key();
			final GameItemSlot defaults = DEFAULT_GAME_ITEMS.get(item);
			if (!config.getBoolean(path + ".enabled", true)) {
				continue;
			}
			final int slot = config.getInt(path + ".slot", defaults.slot());
			if (slot < 0 || slot >= HOTBAR_SIZE) {
				throw new IllegalArgumentException("'" + path + ".slot' in config.yml has to be a hotbar slot, 0-8");
			}
			if (!slots.add(slot)) {
				throw new IllegalArgumentException("'" + path + ".slot' in config.yml is the slot of another game item");
			}
			final String materialName = config.getString(path + ".material", defaults.material().name());
			final Material material = Material.matchMaterial(materialName);
			if (material == null || !material.isItem() || material.isAir()) {
				throw new IllegalArgumentException("'" + path + ".material' in config.yml has to be an item, not " + materialName);
			}
			items.add(new GameItemSlot(item, slot, material));
		}
		return items;
	}

	@NotNull
	private static TimerDisplay loadTimerDisplay(@NotNull final ConfigurationSection config) {
		final String value = config.getString(TIMER_DISPLAY, TimerDisplay.ACTION_BAR.name());
		try {
			return TimerDisplay.valueOf(value.toUpperCase(Locale.ROOT).replace('-', '_'));
		} catch (final IllegalArgumentException ex) {
			throw new IllegalArgumentException("'" + TIMER_DISPLAY + "' in config.yml has to be action-bar, xp-bar or both", ex);
		}
	}

	@NotNull
	private static List<Medal> loadMedals(@NotNull final ConfigurationSection config) {
		if (!config.contains(MEDALS)) {
			return List.of();
		}
		final ConfigurationSection section = config.getConfigurationSection(MEDALS);
		if (section == null) {
			throw new IllegalArgumentException("'" + MEDALS + "' in config.yml has to map medal keys to display names");
		}
		final List<Medal> medals = new ArrayList<>();
		// The keys keep the order of the file, which is the order of the medals, best first
		for (final String key : section.getKeys(false)) {
			final String path = MEDALS + "." + key;
			if (!MEDAL_KEY.matcher(key).matches()) {
				throw new IllegalArgumentException("'" + path + "' in config.yml: a medal key can contain only lowercase letters, digits, '_' and '-'");
			}
			final String displayName = section.getString(key);
			if (displayName == null || displayName.isBlank() || section.isConfigurationSection(key)) {
				throw new IllegalArgumentException("'" + path + "' in config.yml has to be the display name of the medal");
			}
			medals.add(new Medal(key, displayName));
		}
		return medals;
	}
}
