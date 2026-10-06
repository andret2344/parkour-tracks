package eu.andret.parkourtracks;

import eu.andret.parkourtracks.config.Settings;
import eu.andret.parkourtracks.config.SettingsLoader;
import eu.andret.parkourtracks.track.TrackRegistry;
import eu.andret.parkourtracks.track.TrackStore;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

public class ParkourTracksPlugin extends JavaPlugin {
	private static final String CONFIG_FILE = "config.yml";
	private static final String TRACKS_FILE = "tracks.json";

	private Settings settings;
	private TrackRegistry trackRegistry;

	/**
	 * An invalid config or tracks file throws, which stops the plugin: starting on defaults, or with no tracks, would
	 * hide the problem, and the first save would overwrite the tracks file.
	 */
	@Override
	public void onEnable() {
		saveDefaultConfig();
		settings = readSettings();
		trackRegistry = new TrackRegistry(new TrackStore(getDataFolder().toPath().resolve(TRACKS_FILE)));
		trackRegistry.load();
	}

	/**
	 * Reads the config again. When it is invalid, nothing changes and the exception says why.
	 *
	 * @throws IllegalArgumentException when the config cannot be read or its content is invalid
	 */
	public void reload() {
		settings = readSettings();
	}

	@NotNull
	private Settings readSettings() {
		// Bukkit's getConfig() only logs a broken file and goes on with an empty config, so it is read by hand
		final YamlConfiguration config = new YamlConfiguration();
		try {
			config.load(new File(getDataFolder(), CONFIG_FILE));
		} catch (final IOException | InvalidConfigurationException ex) {
			throw new IllegalArgumentException("Could not read " + CONFIG_FILE + ": " + ex.getMessage(), ex);
		}
		return SettingsLoader.load(config);
	}

	@NotNull
	public Settings getSettings() {
		return settings;
	}

	@NotNull
	public TrackRegistry getTrackRegistry() {
		return trackRegistry;
	}
}
