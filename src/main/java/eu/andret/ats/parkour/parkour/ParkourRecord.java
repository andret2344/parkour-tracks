/*
 * Copyright Andret (c) 2018-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.parkour;

import lombok.Value;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@Value
public class ParkourRecord {
	@NotNull
	UUID uuid;
	@NotNull
	ParkourGame game;
	double time;
}
