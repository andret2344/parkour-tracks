package eu.andret.parkourtracks.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Clicks in menu windows: nothing moves, a click on a slot of the window does what the slot stands for.
 */
public final class MenuListener implements Listener {
	@EventHandler(priority = EventPriority.HIGH)
	public void click(@NotNull final InventoryClickEvent event) {
		if (!(event.getView().getTopInventory().getHolder() instanceof final Menu menu)) {
			return;
		}
		event.setCancelled(true);
		if (event.getWhoClicked() instanceof final Player player
				&& event.getRawSlot() >= 0 && event.getRawSlot() < event.getView().getTopInventory().getSize()) {
			menu.click(event.getRawSlot(), player);
		}
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void drag(@NotNull final InventoryDragEvent event) {
		if (event.getView().getTopInventory().getHolder() instanceof Menu) {
			event.setCancelled(true);
		}
	}
}
