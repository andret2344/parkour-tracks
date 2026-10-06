package eu.andret.parkourtracks.config;

import eu.andret.parkourtracks.game.GameItem;
import org.bukkit.Material;
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
import static org.assertj.core.api.Assertions.tuple;

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

	@Test
	void defaultsForTheGameSettings() throws InvalidConfigurationException {
		// when
		final Settings settings = SettingsLoader.load(yaml(""));

		// then
		assertThat(settings.finishDelaySeconds()).isEqualTo(5);
		assertThat(settings.sprintGraceTicks()).isEqualTo(5);
		assertThat(settings.timerDisplay()).isEqualTo(TimerDisplay.ACTION_BAR);
		assertThat(settings.gameItems()).extracting(GameItemSlot::item, GameItemSlot::slot)
				.containsExactly(tuple(GameItem.BACK, 0), tuple(GameItem.RESTART, 1), tuple(GameItem.HIDE, 7),
						tuple(GameItem.EXIT, 8));
	}

	@Test
	void readsTheGameSettings() throws InvalidConfigurationException {
		// when
		final Settings settings = SettingsLoader.load(yaml("""
				finish-delay: 0
				sprint-grace-ticks: 10
				timer-display: Xp-Bar
				game-items:
				  back:
				    enabled: false
				  exit:
				    slot: 4
				    material: barrier
				"""));

		// then
		assertThat(settings.finishDelaySeconds()).isZero();
		assertThat(settings.sprintGraceTicks()).isEqualTo(10);
		assertThat(settings.timerDisplay()).isEqualTo(TimerDisplay.XP_BAR);
		assertThat(settings.gameItems()).extracting(GameItemSlot::item).doesNotContain(GameItem.BACK);
		assertThat(settings.gameItems()).contains(new GameItemSlot(GameItem.EXIT, 4, Material.BARRIER));
	}

	@Test
	void rejectsInvalidGameSettings() {
		// when / then
		assertThatThrownBy(() -> SettingsLoader.load(yaml("finish-delay: -1")))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("'finish-delay'");
		assertThatThrownBy(() -> SettingsLoader.load(yaml("sprint-grace-ticks: soon")))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("'sprint-grace-ticks'");
		assertThatThrownBy(() -> SettingsLoader.load(yaml("timer-display: title")))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("'timer-display'");
		assertThatThrownBy(() -> SettingsLoader.load(yaml(exitItem("slot: 9"))))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("'game-items.exit.slot'");
		assertThatThrownBy(() -> SettingsLoader.load(yaml(exitItem("slot: 0"))))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("another game item");
		assertThatThrownBy(() -> SettingsLoader.load(yaml(exitItem("material: water"))))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("'game-items.exit.material'");
	}

	@NotNull
	static String exitItem(@NotNull final String setting) {
		return """
				game-items:
				  exit:
				    %s
				""".formatted(setting);
	}
}
