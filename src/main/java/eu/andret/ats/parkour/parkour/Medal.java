/*
 * Copyright Andret (c) 2018-2022. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.parkour;

import lombok.Value;
import org.jetbrains.annotations.NotNull;

@Value
public class Medal implements Comparable<Medal> {
	@NotNull
	String name;
	@NotNull
	String displayName;
	int importance;

	@Override
	public int compareTo(@NotNull final Medal other) {
		return other.importance - importance;
	}
}
