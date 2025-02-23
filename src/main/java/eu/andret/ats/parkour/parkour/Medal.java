/*
 * Copyright Andret Tools System (c) 2025. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.parkour;

import org.jetbrains.annotations.NotNull;

public record Medal(@NotNull String name, @NotNull String displayName, int importance) implements Comparable<Medal> {
	@Override
	public int compareTo(@NotNull final Medal other) {
		return other.importance - importance;
	}
}
