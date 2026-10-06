package eu.andret.parkourtracks.game;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Using the game items, and keeping them where they are: they cannot be moved, dropped, swapped to the other hand
 * or put into a container. Players in a game pick nothing up either, as their inventory is given back as it was.
 */
public final class GameItemListener implements Listener {
	@NotNull
	private final GameManager games;

	public GameItemListener(@NotNull final GameManager games) {
		this.games = games;
	}

	/**
	 * Runs for cancelled events too: Bukkit itself cancels clicking the air with an item.
	 */
	@EventHandler(priority = EventPriority.HIGH)
	public void use(@NotNull final PlayerInteractEvent event) {
		if (event.getHand() != EquipmentSlot.HAND || event.getAction() == Action.PHYSICAL) {
			return;
		}
		games.getItems().identify(event.getItem()).ifPresent(item -> {
			event.setCancelled(true);
			final Player player = event.getPlayer();
			if (games.getSession(player).isEmpty()) {
				// A game item outside a game is a leftover with no use there
				player.getInventory().setItemInMainHand(null);
				return;
			}
			games.use(player, item);
		});
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void click(@NotNull final InventoryClickEvent event) {
		final boolean hotbarItem = event.getHotbarButton() >= 0
				&& isGameItem(event.getWhoClicked().getInventory().getItem(event.getHotbarButton()));
		if (isGameItem(event.getCurrentItem()) || isGameItem(event.getCursor()) || hotbarItem) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void drag(@NotNull final InventoryDragEvent event) {
		if (isGameItem(event.getOldCursor())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void drop(@NotNull final PlayerDropItemEvent event) {
		if (isGameItem(event.getItemDrop().getItemStack())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void swap(@NotNull final PlayerSwapHandItemsEvent event) {
		if (isGameItem(event.getMainHandItem()) || isGameItem(event.getOffHandItem())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void pickup(@NotNull final EntityPickupItemEvent event) {
		if (event.getEntity() instanceof final Player player && games.getSession(player).isPresent()) {
			event.setCancelled(true);
		}
	}

	private boolean isGameItem(@Nullable final ItemStack item) {
		return games.getItems().identify(item).isPresent();
	}
}
