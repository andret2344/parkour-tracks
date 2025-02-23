/*
 * Copyright Andret Tools System (c) 2025. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.item;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public interface ParkourItem {
	@NotNull
	ItemStack toItemStack();
}
