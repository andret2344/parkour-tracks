/*
 * Copyright Andret (c) 2019-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.item;

import lombok.ToString;
import lombok.Value;
import lombok.experimental.NonFinal;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

@Value
@NonFinal
@ToString
public class ParkourInteractiveItem implements ParkourItem {
	Material material;
	String name;
	List<String> lore;

	@Override
	public ItemStack toItemStack() {
		final ItemStack itemStack = new ItemStack(material);
		final ItemMeta itemMeta = itemStack.getItemMeta();
		if (itemMeta == null) {
			return itemStack;
		}
		itemMeta.setDisplayName(name);
		itemMeta.setLore(lore);
		itemStack.setItemMeta(itemMeta);
		return itemStack;
	}
}
