/*
 * Copyright Andret (c) 2019-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.parkour;

import lombok.Value;

import java.util.UUID;

@Value
public class ParkourRecord {
	UUID uuid;
	ParkourGame game;
	double time;
}
