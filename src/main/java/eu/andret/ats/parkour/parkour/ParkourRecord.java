/*
 * Copyright Andret (c) 2019-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.parkour;

import lombok.Value;

@Value
public class ParkourRecord {
	String nick;
	ParkourGame parkourGame;
	float time;
}
