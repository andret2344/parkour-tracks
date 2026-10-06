package eu.andret.parkourtracks;

import eu.andret.parkourtracks.command.CommandSupport;
import eu.andret.parkourtracks.command.EffectType;
import eu.andret.parkourtracks.command.MedalParameterType;
import eu.andret.parkourtracks.command.OptionParameterType;
import eu.andret.parkourtracks.command.OptionValue;
import eu.andret.parkourtracks.command.PlaceholderCondition;
import eu.andret.parkourtracks.command.PlayerCommand;
import eu.andret.parkourtracks.command.TrackCommand;
import eu.andret.parkourtracks.command.TrackParameterType;
import eu.andret.parkourtracks.command.TrackPartsCommand;
import eu.andret.parkourtracks.command.TrackSettingsCommand;
import eu.andret.parkourtracks.config.Medal;
import eu.andret.parkourtracks.config.Settings;
import eu.andret.parkourtracks.config.SettingsLoader;
import eu.andret.parkourtracks.game.GameListener;
import eu.andret.parkourtracks.game.GameManager;
import eu.andret.parkourtracks.message.Messages;
import eu.andret.parkourtracks.selection.Selections;
import eu.andret.parkourtracks.selection.WorldEditSelections;
import eu.andret.parkourtracks.track.Track;
import eu.andret.parkourtracks.track.TrackOption;
import eu.andret.parkourtracks.track.TrackRegistry;
import eu.andret.parkourtracks.track.TrackStore;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import revxrsal.commands.Lamp;
import revxrsal.commands.bukkit.BukkitLamp;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

public class ParkourTracksPlugin extends JavaPlugin {
	private static final String CONFIG_FILE = "config.yml";
	private static final String MESSAGES_FILE = "messages.yml";
	private static final String TRACKS_FILE = "tracks.json";

	private Settings settings;
	private Messages messages;
	private TrackRegistry trackRegistry;
	private GameManager games;
	@NotNull
	private Selections selections = new WorldEditSelections();

	/**
	 * An invalid config, messages or tracks file throws, which stops the plugin: starting on defaults, or with no
	 * tracks, would hide the problem, and the first save would overwrite the tracks file.
	 */
	@Override
	public void onEnable() {
		saveDefaultConfig();
		if (!new File(getDataFolder(), MESSAGES_FILE).exists()) {
			saveResource(MESSAGES_FILE, false);
		}
		settings = readSettings();
		messages = readMessages();
		trackRegistry = new TrackRegistry(new TrackStore(getDataFolder().toPath().resolve(TRACKS_FILE)));
		trackRegistry.load();
		games = new GameManager(this);
		getServer().getPluginManager().registerEvents(new GameListener(games), this);
		getServer().getScheduler().runTaskTimer(this, games::tick, 1, 1);
		// Players online already never join the server for the plugin, e.g. after a reload
		getServer().getOnlinePlayers().forEach(games::arrive);
		setUpCommands();
	}

	/**
	 * Ends every game as if the players disconnected: they get their own state back now, and return to the spawn
	 * without paying.
	 */
	@Override
	public void onDisable() {
		if (games != null) {
			games.shutdown();
		}
	}

	/**
	 * Reads the config and the messages again. When either is invalid, nothing changes and the exception says why.
	 *
	 * @throws IllegalArgumentException when a file cannot be read or its content is invalid
	 */
	public void reload() {
		final Settings newSettings = readSettings();
		final Messages newMessages = readMessages();
		settings = newSettings;
		messages = newMessages;
	}

	private void setUpCommands() {
		final CommandSupport support = new CommandSupport(this);
		final Lamp<BukkitCommandActor> lamp = BukkitLamp.builder(this)
				.parameterTypes(types -> types
						.addParameterType(Track.class, new TrackParameterType(support))
						.addParameterType(Medal.class, new MedalParameterType(support))
						.addParameterType(optionClass(), new OptionParameterType(support)))
				.suggestionProviders(providers -> providers
						.addProviderForAnnotation(OptionValue.class, _ -> context -> {
							final TrackOption<?> option = context.getResolvedArgumentOrNull("option");
							return option == null ? List.of() : option.getSuggestions();
						})
						.addProviderForAnnotation(EffectType.class, _ -> _ -> TrackSettingsCommand.effectKeys()))
				.commandCondition(new PlaceholderCondition())
				.build();
		lamp.register(new TrackCommand(support), new TrackPartsCommand(support), new TrackSettingsCommand(support),
				new PlayerCommand(support, games));
	}

	/**
	 * {@code TrackOption.class} typed as the class of options of any value, which Java cannot write directly.
	 */
	@NotNull
	@SuppressWarnings("unchecked")
	private static Class<TrackOption<?>> optionClass() {
		return (Class<TrackOption<?>>) (Class<?>) TrackOption.class;
	}

	@NotNull
	private Settings readSettings() {
		return SettingsLoader.load(readYaml(CONFIG_FILE));
	}

	@NotNull
	private Messages readMessages() {
		final YamlConfiguration defaults = new YamlConfiguration();
		try (final InputStream stream = Objects.requireNonNull(getResource(MESSAGES_FILE), MESSAGES_FILE)) {
			defaults.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
		} catch (final IOException | InvalidConfigurationException ex) {
			throw new IllegalStateException("The shipped " + MESSAGES_FILE + " cannot be read", ex);
		}
		return Messages.load(readYaml(MESSAGES_FILE), defaults);
	}

	/**
	 * Bukkit's {@code getConfig()} only logs a broken file and goes on with an empty one, so files are read by hand.
	 */
	@NotNull
	private YamlConfiguration readYaml(@NotNull final String name) {
		final YamlConfiguration config = new YamlConfiguration();
		try {
			config.load(new File(getDataFolder(), name));
		} catch (final IOException | InvalidConfigurationException ex) {
			throw new IllegalArgumentException("Could not read " + name + ": " + ex.getMessage(), ex);
		}
		return config;
	}

	@NotNull
	public Settings getSettings() {
		return settings;
	}

	@NotNull
	public Messages getMessages() {
		return messages;
	}

	@NotNull
	public TrackRegistry getTrackRegistry() {
		return trackRegistry;
	}

	@NotNull
	public GameManager getGames() {
		return games;
	}

	@NotNull
	public Selections getSelections() {
		return selections;
	}

	/**
	 * Replaces where selections come from; for tests, which have no WorldEdit.
	 */
	public void setSelections(@NotNull final Selections selections) {
		this.selections = selections;
	}
}
