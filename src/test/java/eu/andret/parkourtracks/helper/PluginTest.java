package eu.andret.parkourtracks.helper;

import eu.andret.parkourtracks.ParkourTracksPlugin;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.io.IOException;
import java.nio.file.Files;

/**
 * Starts a mocked server with the plugin loaded before every test.
 */
public abstract class PluginTest {
	protected ServerMock server;
	protected ParkourTracksPlugin plugin;
	protected WorldMock world;

	@BeforeEach
	void setUpServer() {
		server = MockBukkit.mock();
		plugin = MockBukkit.load(ParkourTracksPlugin.class);
		world = server.addSimpleWorld("world");
	}

	@AfterEach
	void tearDownServer() {
		MockBukkit.unmock();
	}

	/**
	 * Replaces the plugin's {@code config.yml} on disk; {@link ParkourTracksPlugin#reload()} reads it.
	 */
	protected void writeConfig(@NotNull final String content) throws IOException {
		Files.writeString(plugin.getDataFolder().toPath().resolve("config.yml"), content);
	}
}
