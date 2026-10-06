package eu.andret.parkourtracks.config;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
	@NotNull
	private static final Pattern MEDAL_KEY = Pattern.compile("[a-z0-9_-]+");

	private SettingsLoader() {
	}

	@NotNull
	public static Settings load(@NotNull final ConfigurationSection config) {
		return new Settings(loadMedals(config), loadFinishDelay(config), loadTimerDisplay(config));
	}

	private static int loadFinishDelay(@NotNull final ConfigurationSection config) {
		if (!config.contains(FINISH_DELAY)) {
			return DEFAULT_FINISH_DELAY;
		}
		if (!config.isInt(FINISH_DELAY) || config.getInt(FINISH_DELAY) < 0) {
			throw new IllegalArgumentException("'" + FINISH_DELAY + "' in config.yml has to be a whole number of seconds, 0 or more");
		}
		return config.getInt(FINISH_DELAY);
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
