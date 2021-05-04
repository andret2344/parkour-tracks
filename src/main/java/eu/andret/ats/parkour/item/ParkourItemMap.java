/*
 *  Copyright Andret (c) 2018-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.item;

import lombok.NonNull;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;

public class ParkourItemMap {
	private final ParkourItem[] row = new ParkourItem[9];

	public void setItem(final int position, @NonNull @NotNull final ParkourItem itemStack) {
		row[position] = itemStack;
	}

	public void removeItem(final int position) {
		row[position] = null;
	}

	public void removeItem(@NonNull @NotNull final ParkourItem itemStack) {
		for (int i = 0; i < row.length; i++) {
			if (itemStack.equals(row[i])) {
				row[i] = null;
			}
		}
	}

	public int getPosition(@NonNull @NotNull final ParkourItem itemStack) {
		for (int i = 0; i < row.length; i++) {
			if (itemStack.equals(row[i])) {
				return i;
			}
		}
		return -1;
	}

	public void iterate(final BiConsumer<Integer, ItemStack> consumer) {
		IntStream.range(0, row.length)
				.forEach(i -> consumer.accept(i, Optional.ofNullable(row[i])
						.map(ParkourItem::toItemStack)
						.orElse(null)));
	}
}
