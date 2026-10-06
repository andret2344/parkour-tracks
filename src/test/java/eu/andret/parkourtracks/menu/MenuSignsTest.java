package eu.andret.parkourtracks.menu;

import eu.andret.parkourtracks.helper.FakeBank;
import eu.andret.parkourtracks.helper.PluginTest;
import eu.andret.parkourtracks.helper.TestTracks;
import eu.andret.parkourtracks.track.Track;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.sign.Side;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MenuSignsTest extends PluginTest {
	private Track track;
	private PlayerMock admin;
	private PlayerMock player;

	@BeforeEach
	void setUpTrack() {
		track = TestTracks.tower(plugin, world);
		admin = admin(30, 1, 30);
		player = server.addPlayer();
		player.setGameMode(GameMode.SURVIVAL);
		player.teleport(new Location(world, 30, 1, 30));
	}

	@NotNull
	Block sign() {
		final Block block = world.getBlockAt(30, 1, 31);
		block.setType(Material.OAK_SIGN);
		block.getChunk().load();
		return block;
	}

	@NotNull
	SignChangeEvent write(@NotNull final PlayerMock writer, @NotNull final Block block, @NotNull final String... lines) {
		final List<Component> components = new ArrayList<>();
		for (final String line : lines) {
			components.add(Component.text(line));
		}
		final SignChangeEvent event = new SignChangeEvent(block, writer, components, Side.FRONT);
		server.getPluginManager().callEvent(event);
		return event;
	}

	@NotNull
	PlayerInteractEvent click(@NotNull final PlayerMock clicker, @NotNull final Block block) {
		final PlayerInteractEvent event = new PlayerInteractEvent(clicker, Action.RIGHT_CLICK_BLOCK, null, block,
				BlockFace.NORTH, EquipmentSlot.HAND);
		server.getPluginManager().callEvent(event);
		plugin.getResults().flush();
		server.getScheduler().performTicks(1);
		return event;
	}

	@NotNull
	static List<String> plain(@NotNull final List<Component> lines) {
		return lines.stream().map(PlainTextComponentSerializer.plainText()::serialize).toList();
	}

	@Test
	void aMenuSignOpensTheMenu() {
		// given
		final Block block = sign();
		final SignChangeEvent written = write(admin, block, "[PTMenu]", "", "", "");

		// when
		final PlayerInteractEvent event = click(player, block);

		// then
		assertThat(plain(written.lines())).containsExactly("Tracks", "", "Click to choose", "");
		assertThat(event.isCancelled()).isTrue();
		assertThat(player.getOpenInventory().getTopInventory().getHolder()).isInstanceOf(Menu.class);
	}

	@Test
	void aJoinSignEntersItsTrack() {
		// given
		final Block block = sign();
		final SignChangeEvent written = write(admin, block, "[ptjoin]", "tower", "", "");

		// when
		click(player, block);

		// then
		assertThat(plain(written.lines())).containsExactly("Play", "tower", "Difficulty 1", "free");
		assertThat(plugin.getGames().getSession(player)).isPresent();
	}

	@Test
	void aJoinSignAsksBeforeAFee() {
		// given
		final FakeBank bank = new FakeBank();
		plugin.setBank(bank);
		bank.set(player, 100);
		track.getOptions().setFee(30);
		final Block block = sign();
		write(admin, block, "[ptjoin]", "tower", "", "");

		// when
		click(player, block);

		// then
		assertThat(plugin.getGames().getSession(player)).isEmpty();
		assertThat(player.getOpenInventory().getTopInventory().getHolder()).isInstanceOf(Menu.class);
	}

	@Test
	void aJoinSignOfAStoppedOrRemovedTrackSaysSo() {
		// given
		final Block block = sign();
		write(admin, block, "[ptjoin]", "tower", "", "");
		track.setRunning(false);

		// when
		click(player, block);
		plugin.getTrackRegistry().remove(track);
		click(player, block);

		// then
		assertThat(messages(player)).containsExactly("Track tower is not running.", "Removed track");
		assertThat(plugin.getGames().getSession(player)).isEmpty();
	}

	@Test
	void signsNeedThePermissionAndAnExistingTrack() {
		// given
		final Block block = sign();

		// when
		final SignChangeEvent noPermission = write(player, block, "[ptmenu]", "", "", "");
		write(admin, block, "[ptjoin]", "cave", "", "");
		final PlayerInteractEvent click = click(player, block);

		// then
		assertThat(messages(player)).containsExactly("You cannot make ParkourTracks signs.");
		assertThat(messages(admin)).containsExactly("There is no track named cave.");
		assertThat(PlainTextComponentSerializer.plainText().serialize(noPermission.line(0))).isEqualTo("[ptmenu]");
		assertThat(click.isCancelled()).isFalse();
	}
}
