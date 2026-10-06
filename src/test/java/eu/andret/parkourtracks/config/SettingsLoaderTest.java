package eu.andret.parkourtracks.config;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SettingsLoaderTest {
	@NotNull
	static YamlConfiguration yaml(@NotNull final String content) throws InvalidConfigurationException {
		final YamlConfiguration config = new YamlConfiguration();
		config.loadFromString(content);
		return config;
	}

	@Test
	void loadsShippedConfigWithMedalsInOrder() throws IOException {
		// given
		final YamlConfiguration config;
		try (final InputStream stream = Objects.requireNonNull(getClass().getResourceAsStream("/config.yml"))) {
			config = YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
		}

		// when
		final Settings settings = SettingsLoader.load(config);

		// then
		assertThat(settings.medals()).extracting(Medal::key).containsExactly("platinum", "gold", "silver", "bronze");
		assertThat(settings.findMedal("gold")).map(Medal::displayName).contains("<gold>Gold");
		assertThat(settings.findMedal("wood")).isEmpty();
	}

	@Test
	void loadsNoMedalsWhenTheSectionIsMissing() throws InvalidConfigurationException {
		// when
		final Settings settings = SettingsLoader.load(yaml(""));

		// then
		assertThat(settings.medals()).isEmpty();
	}

	@Test
	void rejectsMedalsThatAreNotAMap() throws InvalidConfigurationException {
		// given
		final YamlConfiguration config = yaml("medals: gold");

		// when / then
		assertThatThrownBy(() -> SettingsLoader.load(config))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("'medals'");
	}

	@Test
	void rejectsInvalidMedalKey() throws InvalidConfigurationException {
		// given
		final YamlConfiguration config = yaml("medals:\n  Gold: '<gold>Gold'");

		// when / then
		assertThatThrownBy(() -> SettingsLoader.load(config))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("'medals.Gold'");
	}

	@Test
	void rejectsMedalWithoutDisplayName() throws InvalidConfigurationException {
		// given
		final YamlConfiguration blank = yaml("medals:\n  gold: ' '");
		final YamlConfiguration section = yaml("medals:\n  gold:\n    name: x");

		// when / then
		assertThatThrownBy(() -> SettingsLoader.load(blank))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("'medals.gold'");
		assertThatThrownBy(() -> SettingsLoader.load(section))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("'medals.gold'");
	}
}
