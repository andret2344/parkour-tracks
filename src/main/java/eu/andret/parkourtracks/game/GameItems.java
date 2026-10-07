package eu.andret.parkourtracks.game;

import eu.andret.parkourtracks.ParkourTracksPlugin;
import eu.andret.parkourtracks.config.GameItemSlot;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Optional;

/**
 * Makes the game items and recognizes them by the tag in their PDC, never by their look.
 */
public final class GameItems {
	@NotNull
	private final ParkourTracksPlugin plugin;
	@NotNull
	private final NamespacedKey key;

	public GameItems(@NotNull final ParkourTracksPlugin plugin) {
		this.plugin = plugin;
		key = new NamespacedKey(plugin, "game-item");
	}

	/**
	 * Puts every game item turned on into its hotbar slot.
	 */
	public void give(@NotNull final Player player) {
		plugin.getSettings().gameItems().forEach(slot -> player.getInventory().setItem(slot.slot(), create(slot)));
	}

	@NotNull
	private ItemStack create(@NotNull final GameItemSlot slot) {
		final ItemStack item = new ItemStack(slot.material());
		final ItemMeta meta = item.getItemMeta();
		meta.customName(plugin.getMessages().get(slot.item().getName()).decoration(TextDecoration.ITALIC, false));
		meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, slot.item().getKey());
		item.setItemMeta(meta);
		return item;
	}

	/**
	 * Which game item the stack is, if it is one.
	 */
	@NotNull
	public Optional<GameItem> identify(@Nullable final ItemStack item) {
		if (item == null || !item.hasItemMeta()) {
			return Optional.empty();
		}
		final String value = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
		return Arrays.stream(GameItem.values())
				.filter(gameItem -> gameItem.getKey().equals(value))
				.findFirst();
	}
}
