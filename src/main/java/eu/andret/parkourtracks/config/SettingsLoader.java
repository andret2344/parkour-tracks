package eu.andret.parkourtracks.config;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Turns {@code config.yml} into {@link Settings}. An invalid config throws {@link IllegalArgumentException} with the
 * path of what is wrong.
 */
public final class SettingsLoader {
	private static final String MEDALS = "medals";
	@NotNull
	private static final Pattern MEDAL_KEY = Pattern.compile("[a-z0-9_-]+");

	private SettingsLoader() {
	}

	@NotNull
	public static Settings load(@NotNull final ConfigurationSection config) {
		return new Settings(loadMedals(config));
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
