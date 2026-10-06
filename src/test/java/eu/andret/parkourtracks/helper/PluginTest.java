package eu.andret.parkourtracks.helper;

import eu.andret.parkourtracks.ParkourTracksPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

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
}
