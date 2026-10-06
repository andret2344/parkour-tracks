package eu.andret.parkourtracks.display;

import eu.andret.parkourtracks.helper.PluginTest;
import eu.andret.parkourtracks.track.Track;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.event.block.SignChangeEvent;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RecordSignsTest extends PluginTest {
	private Track track;
	private PlayerMock admin;

	@BeforeEach
	void setUpTrack() {
		track = tower();
		admin = admin(30, 1, 30);
	}

	@NotNull
	Block sign(final int x) {
		final Block block = world.getBlockAt(x, 1, 30);
		block.setType(Material.OAK_SIGN);
		// MockBukkit counts a chunk as loaded only once it was loaded on purpose
		block.getChunk().load();
		return block;
	}

	@NotNull
	SignChangeEvent write(@NotNull final PlayerMock writer, @NotNull final Block block, @NotNull final String... lines) {
		// The server hands the event lines it may change
		final List<Component> components = new ArrayList<>();
		for (final String line : lines) {
			components.add(Component.text(line));
		}
		final SignChangeEvent event = new SignChangeEvent(block, writer, components, Side.FRONT);
		server.getPluginManager().callEvent(event);
		waitForResults();
		return event;
	}

	void waitForResults() {
		server.getScheduler().performTicks(1);
		plugin.getResults().flush();
		server.getScheduler().performTicks(1);
	}

	@NotNull
	static List<String> lines(@NotNull final Block block) {
		final Sign sign = (Sign) block.getState();
		return sign.getSide(Side.FRONT).lines().stream()
				.map(PlainTextComponentSerializer.plainText()::serialize)
				.toList();
	}

	void run(@NotNull final UUID player, final int ticks) {
		plugin.getResults().recordRun(track.getId(), player, ticks, Instant.now()).join();
	}

	@Test
	void writingTheHeaderMakesARecordSign() {
		// given
		final PlayerMock bob = server.addPlayer("Bob");
		run(bob.getUniqueId(), 400);
		final Block block = sign(30);

		// when
		final SignChangeEvent event = write(admin, block, "[PTracks]", "tower", "", "");

		// then
		assertThat(event.lines()).extracting(PlainTextComponentSerializer.plainText()::serialize)
				.containsExactly("#1", "tower", "...", "");
		assertThat(lines(block)).containsExactly("#1", "tower", "Bob", "00:20.00");
		assertThat(messages(admin)).containsExactly("Made a record sign of track tower.");
	}

	@Test
	void theThirdLineIsThePlace() {
		// given
		run(server.addPlayer("Bob").getUniqueId(), 400);
		run(server.addPlayer("Alice").getUniqueId(), 500);
		final Block second = sign(30);
		final Block third = sign(31);

		// when
		write(admin, second, "[ptracks]", "tower", "2", "");
		write(admin, third, "[ptracks]", "tower", "3", "");

		// then
		assertThat(lines(second)).containsExactly("#2", "tower", "Alice", "00:25.00");
		assertThat(lines(third)).containsExactly("#3", "tower", "nobody yet", "");
	}

	@Test
	void invalidSignsAreLeftAsWritten() {
		// given
		final PlayerMock player = server.addPlayer();
		final Block block = sign(30);

		// when
		final SignChangeEvent noPermission = write(player, block, "[ptracks]", "tower", "", "");
		final SignChangeEvent unknown = write(admin, block, "[ptracks]", "cave", "", "");
		final SignChangeEvent badPlace = write(admin, block, "[ptracks]", "tower", "first", "");
		final SignChangeEvent ordinary = write(admin, block, "Hello", "tower", "", "");

		// then
		assertThat(messages(player)).containsExactly("You cannot make record signs.");
		assertThat(messages(admin)).containsExactly("There is no track named cave.",
				"The third line is the place in the ranking: 1-1000, or empty for 1.");
		assertThat(PlainTextComponentSerializer.plainText().serialize(noPermission.line(0))).isEqualTo("[ptracks]");
		assertThat(PlainTextComponentSerializer.plainText().serialize(unknown.line(1))).isEqualTo("cave");
		assertThat(PlainTextComponentSerializer.plainText().serialize(badPlace.line(2))).isEqualTo("first");
		assertThat(PlainTextComponentSerializer.plainText().serialize(ordinary.line(0))).isEqualTo("Hello");
	}

	@Test
	void signsFollowTheRankingAndTheTrack() {
		// given
		final Block block = sign(30);
		write(admin, block, "[ptracks]", "tower", "", "");
		final PlayerMock bob = server.addPlayer("Bob");
		run(bob.getUniqueId(), 400);

		// when
		track.setDisplayName("<gold>The Tower");
		plugin.getRecordSigns().refresh(track.getId());
		waitForResults();

		// then
		assertThat(lines(block)).containsExactly("#1", "The Tower", "Bob", "00:20.00");
	}

	@Test
	void signsOfARemovedTrackSayIt() {
		// given
		final Block block = sign(30);
		write(admin, block, "[ptracks]", "tower", "", "");

		// when
		plugin.getTrackRegistry().remove(track);
		plugin.getRecordSigns().refresh(track.getId());
		waitForResults();

		// then
		assertThat(lines(block)).first().isEqualTo("Removed track");
	}

	@Test
	void signsAreFoundAgainInLoadedChunks() {
		// given
		final Block block = sign(30);
		write(admin, block, "[ptracks]", "tower", "", "");
		final PlayerMock bob = server.addPlayer("Bob");
		run(bob.getUniqueId(), 400);
		final RecordSigns fresh = new RecordSigns(plugin);

		// when
		fresh.loadAll();
		waitForResults();

		// then
		assertThat(lines(block)).containsExactly("#1", "tower", "Bob", "00:20.00");
	}

	@Test
	void aSignBrokenSinceIsForgotten() {
		// given
		final Block block = sign(30);
		write(admin, block, "[ptracks]", "tower", "", "");
		block.setType(Material.AIR);

		// when
		plugin.getRecordSigns().refresh(track.getId());
		waitForResults();

		// then
		assertThat(block.getType()).isEqualTo(Material.AIR);
	}
}
