/*
 *  Copyright Andret (c) 2018-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.parkour;

import lombok.Value;
import org.jetbrains.annotations.NotNull;

@Value
public class ParkourMedal implements Comparable<ParkourMedal> {
	String name;
	String display;
	int importance;

	@Override
	public int compareTo(@NotNull final ParkourMedal other) {
		return other.importance - importance;
	}
}
