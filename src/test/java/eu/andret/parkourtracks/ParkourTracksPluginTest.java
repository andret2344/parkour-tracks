package eu.andret.parkourtracks;

import eu.andret.parkourtracks.config.Medal;
import eu.andret.parkourtracks.helper.PluginTest;
import eu.andret.parkourtracks.track.Cuboid;
import eu.andret.parkourtracks.track.Track;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ParkourTracksPluginTest extends PluginTest {
	@Test
	void enablesWithShippedConfig() {
		// when
		final boolean result = plugin.isEnabled();

		// then
		assertThat(result).isTrue();
		assertThat(plugin.getSettings().medals()).extracting(Medal::key)
				.containsExactly("platinum", "gold", "silver", "bronze");
		assertThat(plugin.getTrackRegistry().getTracks()).isEmpty();
	}

	@Test
	void loadsSavedTracksOnEnable() {
		// given
		final Track track = plugin.getTrackRegistry().create("tower", "world", new Cuboid(0, 0, 0, 5, 5, 5));
		plugin.getTrackRegistry().save();
		server.getPluginManager().disablePlugin(plugin);

		// when
		server.getPluginManager().enablePlugin(plugin);

		// then
		assertThat(plugin.getTrackRegistry().getTracks()).containsExactly(track);
	}

	@Test
	void reloadReadsTheConfigAgain() throws IOException {
		// given
		writeConfig("medals:\n  gold: '<gold>Gold'\n");

		// when
		plugin.reload();

		// then
		assertThat(plugin.getSettings().medals()).extracting(Medal::key).containsExactly("gold");
	}

	@Test
	void reloadOfInvalidContentKeepsTheSettings() throws IOException {
		// given
		writeConfig("medals:\n  Gold: '<gold>Gold'\n");

		// when / then
		assertThatThrownBy(plugin::reload).isInstanceOf(IllegalArgumentException.class);
		assertThat(plugin.getSettings().medals()).hasSize(4);
	}

	@Test
	void reloadOfBrokenYamlKeepsTheSettings() throws IOException {
		// given
		writeConfig("medals: [");

		// when / then
		assertThatThrownBy(plugin::reload)
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageStartingWith("Could not read config.yml");
		assertThat(plugin.getSettings().medals()).hasSize(4);
	}

	@Test
	void doesNotEnableWithBrokenTracksFile() throws IOException {
		// given
		server.getPluginManager().disablePlugin(plugin);
		Files.writeString(plugin.getDataFolder().toPath().resolve("tracks.json"), "{ not json");

		// when / then: the server stops a plugin whose onEnable throws
		assertThatThrownBy(() -> server.getPluginManager().enablePlugin(plugin))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageStartingWith("Could not read tracks.json");
		// The broken file is left for the admin to fix, never overwritten
		assertThat(Files.readString(plugin.getDataFolder().toPath().resolve("tracks.json"))).isEqualTo("{ not json");
	}

	@Test
	void doesNotEnableWithInvalidConfig() throws IOException {
		// given
		server.getPluginManager().disablePlugin(plugin);
		writeConfig("medals: [");

		// when / then: the server stops a plugin whose onEnable throws
		assertThatThrownBy(() -> server.getPluginManager().enablePlugin(plugin))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageStartingWith("Could not read config.yml");
	}
}
