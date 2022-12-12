/*
 * Copyright Andret (c) 2018-2022. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.parkour;

import lombok.Value;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@Value
public class Score {
	@NotNull
	UUID uuid;
	@NotNull
	ParkourGame game;
	double time;
}
