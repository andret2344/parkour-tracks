package eu.andret.parkourtracks.menu;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * A menu window: its inventory and what clicking each slot does. Clicks never move items, {@link MenuListener}
 * cancels them all.
 */
public final class Menu implements InventoryHolder {
	@NotNull
	private final Map<Integer, Consumer<Player>> actions = new HashMap<>();
	private Inventory inventory;

	void setInventory(@NotNull final Inventory inventory) {
		this.inventory = inventory;
	}

	void onClick(final int slot, @NotNull final Consumer<Player> action) {
		actions.put(slot, action);
	}

	void click(final int slot, @NotNull final Player player) {
		final Consumer<Player> action = actions.get(slot);
		if (action != null) {
			action.accept(player);
		}
	}

	@NotNull
	@Override
	public Inventory getInventory() {
		return inventory;
	}
}
