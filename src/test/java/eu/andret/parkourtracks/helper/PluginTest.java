package eu.andret.parkourtracks.helper;

import eu.andret.parkourtracks.ParkourTracksPlugin;
import eu.andret.parkourtracks.selection.Selection;
import eu.andret.parkourtracks.selection.SelectionException;
import eu.andret.parkourtracks.track.Cuboid;
import eu.andret.parkourtracks.track.Track;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.simulate.entity.PlayerSimulation;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Starts a mocked server with the plugin loaded before every test. WorldEdit is replaced by selections the test sets
 * with {@link #select}, the scoreboard sidebar by {@link FakeSidebar}, and the world's chunks list their tile
 * entities ({@link TileEntityWorld}).
 */
public abstract class PluginTest {
	protected ServerMock server;
	protected ParkourTracksPlugin plugin;
	protected WorldMock world;
	@NotNull
	protected final FakeSidebar sidebar = new FakeSidebar();
	@NotNull
	private final Map<UUID, Selection> selections = new HashMap<>();

	@BeforeEach
	void setUpServer() {
		server = MockBukkit.mock();
		plugin = MockBukkit.load(ParkourTracksPlugin.class);
		world = new TileEntityWorld("world");
		server.addWorld(world);
		plugin.setSidebar(sidebar);
		plugin.setSelections(player -> {
			final Selection selection = selections.get(player.getUniqueId());
			if (selection == null) {
				throw new SelectionException(SelectionException.Reason.INCOMPLETE);
			}
			return selection;
		});
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

	protected void writeMessages(@NotNull final String content) throws IOException {
		Files.writeString(plugin.getDataFolder().toPath().resolve("messages.yml"), content);
	}

	/**
	 * Makes the given cuboid the player's WorldEdit selection in the given world.
	 */
	protected void select(@NotNull final Player player, @NotNull final World world, @NotNull final Cuboid cuboid) {
		selections.put(player.getUniqueId(), new Selection(world.getName(), cuboid));
	}

	/**
	 * An operator, standing at the given block of {@link #world}.
	 */
	@NotNull
	protected PlayerMock admin(final double x, final double y, final double z) {
		final PlayerMock player = server.addPlayer();
		player.setOp(true);
		player.teleport(new Location(world, x, y, z));
		return player;
	}

	/**
	 * A stopped track named {@code tower} with the region 0,0,0 - 20,20,20 in {@link #world}.
	 */
	@NotNull
	protected Track tower() {
		final Track track = plugin.getTrackRegistry().create("tower", world.getName(), new Cuboid(0, 0, 0, 20, 20, 20));
		plugin.getTrackRegistry().save();
		return track;
	}

	/**
	 * Moves the player as a {@code PlayerMoveEvent} would: the location is set before the event is called and set
	 * back when the event is canceled.
	 */
	protected static void move(@NotNull final PlayerMock player, @NotNull final Location to) {
		new PlayerSimulation(player).simulatePlayerMove(to);
	}

	/**
	 * Takes all messages the player got so far, as plain text.
	 */
	@NotNull
	protected static List<String> messages(@NotNull final PlayerMock player) {
		final List<String> messages = new ArrayList<>();
		Component message = player.nextComponentMessage();
		while (message != null) {
			messages.add(PlainTextComponentSerializer.plainText().serialize(message));
			message = player.nextComponentMessage();
		}
		return messages;
	}
}
