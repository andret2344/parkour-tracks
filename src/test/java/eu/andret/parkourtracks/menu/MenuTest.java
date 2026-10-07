package eu.andret.parkourtracks.menu;

import eu.andret.parkourtracks.game.GameItem;
import eu.andret.parkourtracks.helper.FakeBank;
import eu.andret.parkourtracks.helper.PluginTest;
import eu.andret.parkourtracks.helper.TestTracks;
import eu.andret.parkourtracks.track.Track;
import eu.andret.parkourtracks.track.TrackType;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionAttachment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class MenuTest extends PluginTest {
	private Track track;
	private PlayerMock player;

	@BeforeEach
	void setUpTrack() {
		track = TestTracks.tower(plugin, world);
		player = server.addPlayer();
		player.setGameMode(GameMode.SURVIVAL);
		player.teleport(new Location(world, 50, 1, 2.5));
	}

	void open() {
		plugin.getMenus().open(player);
		waitForResults();
	}

	void waitForResults() {
		plugin.getResults().flush();
		server.getScheduler().performTicks(1);
	}

	@NotNull
	Inventory top() {
		return player.getOpenInventory().getTopInventory();
	}

	void click(final int slot) {
		server.getPluginManager().callEvent(new InventoryClickEvent(player.getOpenInventory(),
				InventoryType.SlotType.CONTAINER, slot, ClickType.LEFT, InventoryAction.PICKUP_ALL));
		waitForResults();
	}

	@NotNull
	static String name(@Nullable final ItemStack item) {
		return PlainTextComponentSerializer.plainText().serialize(Objects.requireNonNull(item).getItemMeta().customName());
	}

	@NotNull
	static List<String> lore(@NotNull final ItemStack item) {
		return Objects.requireNonNull(item.getItemMeta().lore()).stream()
				.map(PlainTextComponentSerializer.plainText()::serialize)
				.toList();
	}

	@NotNull
	Track another(@NotNull final String name, final int x) {
		return TestTracks.small(plugin, world, name, x);
	}

	@Test
	void withOneCategoryItOpensRightAway() {
		// when
		open();

		// then
		assertThat(top().getHolder()).isInstanceOf(Menu.class);
		final ItemStack item = top().getItem(0);
		assertThat(item.getType()).isEqualTo(Material.LIME_WOOL);
		assertThat(name(item)).isEqualTo("tower");
		assertThat(lore(item)).containsExactly("Difficulty: 1", "Fee: free", "Your best: none",
				"Record: none by nobody yet", "Medal: none", "Completions: 0", "Click to play");
	}

	@Test
	void theIconFollowsTheTrackOrItsDifficulty() {
		// given
		track.getOptions().setDifficulty(4);
		another("cave", 50).getOptions().setIcon(Material.LADDER);

		// when
		open();

		// then
		assertThat(top().getItem(0).getType()).isEqualTo(Material.LADDER);
		assertThat(top().getItem(1).getType()).isEqualTo(Material.RED_WOOL);
	}

	@Test
	void stoppedTracksAreNotListed() {
		// given
		track.setRunning(false);

		// when
		open();

		// then
		assertThat(messages(player)).containsExactly("There are no tracks to play yet.");
	}

	@Test
	void severalCategoriesOpenTheirChoiceFirst() {
		// given
		final Track training = another("practice", 50);
		training.setType(TrackType.TRAINING);
		another("built", 70).setType(TrackType.PLAYERS);

		// when
		open();
		final List<String> categories = List.of(name(top().getItem(11)), name(top().getItem(13)), name(top().getItem(15)));
		click(13);

		// then
		assertThat(categories).containsExactly("Server tracks", "Training tracks", "Tracks by players");
		assertThat(name(top().getItem(0))).isEqualTo("practice");
		assertThat(top().getItem(1)).isNull();
	}

	@Test
	void clickingAFreeTrackEntersIt() {
		// given
		open();

		// when
		click(0);

		// then
		assertThat(plugin.getGames().getSession(player)).isPresent();
		assertThat(player.getLocation()).isEqualTo(new Location(world, 2.5, 1, 2.5));
	}

	@Test
	void aTrackWithAFeeAsksFirst() {
		// given
		final FakeBank bank = new FakeBank();
		plugin.setBank(bank);
		bank.set(player, 100);
		track.getOptions().setFee(30);
		open();

		// when
		click(0);
		final String title = PlainTextComponentSerializer.plainText().serialize(player.getOpenInventory().title());
		final boolean inGameBeforePaying = plugin.getGames().getSession(player).isPresent();
		click(11);

		// then
		assertThat(title).isEqualTo("Pay $30.00?");
		assertThat(inGameBeforePaying).isFalse();
		assertThat(plugin.getGames().getSession(player)).isPresent();
		assertThat(bank.balance(player)).isEqualTo(70);
	}

	@Test
	void cancelingThePaymentPaysNothing() {
		// given
		final FakeBank bank = new FakeBank();
		plugin.setBank(bank);
		bank.set(player, 100);
		track.getOptions().setFee(30);
		open();
		click(0);

		// when
		click(15);

		// then
		assertThat(plugin.getGames().getSession(player)).isEmpty();
		assertThat(bank.balance(player)).isEqualTo(100);
	}

	@Test
	void tracksWithoutPermissionShowLocked() {
		// given
		track.getOptions().setPermission("tracks.vip");
		open();

		// when
		final List<String> lore = lore(top().getItem(0));
		click(0);

		// then
		assertThat(lore).last().isEqualTo("Locked");
		assertThat(plugin.getGames().getSession(player)).isEmpty();
		assertThat(messages(player)).containsExactly("You may not enter track tower.");
	}

	@Test
	void completedTracksGlowAndShowTheResults() {
		// given
		plugin.getResults().recordRun(track.getId(), player.getUniqueId(), 610, Instant.now()).join();

		// when
		open();

		// then
		final ItemStack item = top().getItem(0);
		assertThat(item.getItemMeta().getEnchantmentGlintOverride()).isTrue();
		assertThat(lore(item)).contains("Your best: 00:30.50", "Completions: 1");
	}

	@Test
	void tracksByPlayersShowTheirAuthors() {
		// given
		track.setType(TrackType.PLAYERS);
		track.setAuthors(List.of(player.getUniqueId()));

		// when
		open();

		// then
		assertThat(lore(top().getItem(0))).last().isEqualTo("Built by " + player.getName());
	}

	@Test
	void manyTracksGoOnPages() {
		// given
		for (int i = 0; i < 45; i++) {
			another("track" + i, 100 + i * 10);
		}

		// when
		open();
		final ItemStack next = top().getItem(53);
		final ItemStack previous = top().getItem(45);
		click(53);

		// then
		assertThat(next).isNotNull();
		assertThat(previous).isNull();
		assertThat(top().getItem(0)).isNotNull();
		assertThat(top().getItem(1)).isNull();
		assertThat(top().getItem(45)).isNotNull();
		assertThat(top().getItem(53)).isNull();
	}

	@Test
	void clicksNeverMoveItems() {
		// given
		open();
		final InventoryClickEvent event = new InventoryClickEvent(player.getOpenInventory(),
				InventoryType.SlotType.CONTAINER, 30, ClickType.SHIFT_LEFT, InventoryAction.MOVE_TO_OTHER_INVENTORY);

		// when
		server.getPluginManager().callEvent(event);

		// then
		assertThat(event.isCancelled()).isTrue();
	}

	@Test
	void theMenuItemAndTheCommandOpenIt() {
		// given
		player.performCommand("ptracks menu");
		waitForResults();
		final boolean byCommand = top().getHolder() instanceof Menu;
		player.closeInventory();
		plugin.getGames().enter(player, track);

		// when
		plugin.getGames().use(player, GameItem.MENU);
		waitForResults();

		// then
		assertThat(byCommand).isTrue();
		assertThat(top().getHolder()).isInstanceOf(Menu.class);
	}

	@Test
	void choosingAnotherTrackFromAGameSwitchesTracks() {
		// given
		final Track other = another("cave", 50);
		plugin.getGames().enter(player, track);
		open();

		// when: cave comes first, by name
		click(0);

		// then
		assertThat(plugin.getGames().getSession(player)).map(session -> session.getTrack()).contains(other);
	}
}
