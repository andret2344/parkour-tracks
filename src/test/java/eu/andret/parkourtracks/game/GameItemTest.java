package eu.andret.parkourtracks.game;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Item;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class GameItemTest extends GameTest {
	/**
	 * Right-clicks the air with the item in the given hotbar slot.
	 */
	PlayerInteractEvent use(final PlayerMock player, final int slot) {
		player.getInventory().setHeldItemSlot(slot);
		final PlayerInteractEvent event = new PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR,
				player.getInventory().getItemInMainHand(), null, BlockFace.SELF, EquipmentSlot.HAND);
		server.getPluginManager().callEvent(event);
		return event;
	}

	@Test
	void joiningGivesTheGameItemsWithTheirNames() {
		// when
		final PlayerMock player = onSpawn();

		// then
		assertThat(player.getInventory().getItem(0).getType()).isEqualTo(Material.SLIME_BALL);
		assertThat(player.getInventory().getItem(1).getType()).isEqualTo(Material.CLOCK);
		assertThat(player.getInventory().getItem(7).getType()).isEqualTo(Material.ENDER_EYE);
		assertThat(player.getInventory().getItem(8).getType()).isEqualTo(Material.RED_BED);
		assertThat(PlainTextComponentSerializer.plainText().serialize(player.getInventory().getItem(8).getItemMeta().customName()))
				.isEqualTo("Leave the track");
		assertThat(games().getItems().identify(player.getInventory().getItem(8))).contains(GameItem.EXIT);
	}

	@Test
	void itemsCanBeMovedTurnedOffAndMadeOfOtherMaterials() throws IOException {
		// given
		writeConfig("""
				game-items:
				  exit:
				    slot: 4
				    material: barrier
				  hide:
				    enabled: false
				""");
		plugin.reload();

		// when
		final PlayerMock player = onSpawn();

		// then
		assertThat(player.getInventory().getItem(4).getType()).isEqualTo(Material.BARRIER);
		assertThat(player.getInventory().getItem(8)).isNull();
		assertThat(player.getInventory().contains(Material.ENDER_EYE)).isFalse();
		assertThat(player.getInventory().getItem(0).getType()).isEqualTo(Material.SLIME_BALL);
	}

	@Test
	void aLookalikeIsNoGameItem() {
		// when / then
		assertThat(games().getItems().identify(new ItemStack(Material.RED_BED))).isEmpty();
		assertThat(games().getItems().identify(null)).isEmpty();
	}

	@Test
	void backGoesToTheLastCheckpoint() {
		// given
		final PlayerMock player = running();
		walkTo(player, 9.5);

		// when
		final PlayerInteractEvent event = use(player, 0);

		// then
		assertThat(event.useItemInHand()).isEqualTo(Event.Result.DENY);
		assertThat(player.getLocation()).isEqualTo(at(8.5, 1, 2.5));
	}

	@Test
	void restartGoesToTheSpawn() {
		// given
		final PlayerMock player = running();
		walkTo(player, 9.5);

		// when
		use(player, 1);

		// then
		assertThat(player.getLocation()).isEqualTo(at(2.5, 1, 2.5));
		assertThat(session(player).getPhase()).isEqualTo(Phase.WAITING);
	}

	@Test
	void exitLeavesToTheLobby() {
		// given
		final PlayerMock player = running();

		// when
		use(player, 8);

		// then
		assertThat(games().getSession(player)).isEmpty();
		assertThat(player.getLocation()).isEqualTo(at(100, 64, 100));
		assertThat(player.getInventory().contains(Material.RED_BED)).isFalse();
	}

	@Test
	void hideHidesTheOtherPlayersOfTheTrackOnly() {
		// given
		final PlayerMock hider = onSpawn();
		final PlayerMock other = onSpawn();
		final PlayerMock outside = player();

		// when
		use(hider, 7);
		final PlayerMock newcomer = onSpawn();

		// then
		assertThat(hider.canSee(other)).isFalse();
		assertThat(hider.canSee(newcomer)).isFalse();
		assertThat(hider.canSee(outside)).isTrue();
		assertThat(other.canSee(hider)).isTrue();
		assertThat(messages(hider)).containsExactly("The other players on the track are hidden.");
	}

	@Test
	void hidingEndsWithTheGameBothWays() {
		// given
		final PlayerMock hider = onSpawn();
		final PlayerMock other = onSpawn();
		use(hider, 7);
		final PlayerMock leaver = onSpawn();

		// when
		games().leave(leaver, LeaveReason.EXIT);
		games().leave(hider, LeaveReason.EXIT);

		// then
		assertThat(hider.canSee(other)).isTrue();
		assertThat(hider.canSee(leaver)).isTrue();
	}

	@Test
	void hideAgainShowsThePlayers() {
		// given
		final PlayerMock hider = onSpawn();
		final PlayerMock other = onSpawn();
		use(hider, 7);

		// when
		use(hider, 7);

		// then
		assertThat(hider.canSee(other)).isTrue();
	}

	@Test
	void aGameItemOutsideAGameIsRemovedWhenUsed() {
		// given
		final PlayerMock player = onSpawn();
		final ItemStack bed = player.getInventory().getItem(8);
		games().leave(player, LeaveReason.EXIT);
		player.getInventory().setItem(8, bed);

		// when
		use(player, 8);

		// then
		assertThat(player.getInventory().getItem(8)).isNull();
	}

	@Test
	void gameItemsCannotBeMovedDroppedOrSwapped() {
		// given
		final PlayerMock player = onSpawn();
		final ItemStack bed = player.getInventory().getItem(8);
		final InventoryClickEvent click = new InventoryClickEvent(player.openInventory(player.getInventory()),
				InventoryType.SlotType.QUICKBAR, 8, ClickType.LEFT, InventoryAction.PICKUP_ALL);
		final Item dropped = world.dropItem(at(2, 1, 2), bed);
		final PlayerDropItemEvent drop = new PlayerDropItemEvent(player, dropped);
		final PlayerSwapHandItemsEvent swap = new PlayerSwapHandItemsEvent(player, null, bed);

		// when
		server.getPluginManager().callEvent(click);
		server.getPluginManager().callEvent(drop);
		server.getPluginManager().callEvent(swap);

		// then
		assertThat(click.isCancelled()).isTrue();
		assertThat(drop.isCancelled()).isTrue();
		assertThat(swap.isCancelled()).isTrue();
	}

	@Test
	void playersInAGamePickNothingUp() {
		// given
		final PlayerMock player = onSpawn();
		final Item item = world.dropItem(at(2, 1, 2), new ItemStack(Material.ENDER_PEARL));
		final EntityPickupItemEvent event = new EntityPickupItemEvent(player, item, 0);

		// when
		server.getPluginManager().callEvent(event);

		// then
		assertThat(event.isCancelled()).isTrue();
	}
}
