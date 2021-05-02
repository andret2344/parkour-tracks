/*
 * Copyright Andret (c) 2019-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.parkour;

import lombok.Value;
import org.jetbrains.annotations.NotNull;

@Value
public class ParkourMedal implements Comparable<ParkourMedal> {
	@NotNull
	String name;
	@NotNull
	String displayName;
	int importance;

	@Override
	public int compareTo(@NotNull final ParkourMedal other) {
		return other.importance - importance;
	}

	@Override
	public String toString() {
		return "ParkourMedal(" +
				"name=" + name +
				", displayName=" + displayName + "&r" +
				", importance=" + importance +
				')';
	}
}
