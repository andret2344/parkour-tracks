/*
 * Copyright Andret (c) 2018-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.region;

import com.sk89q.worldedit.math.BlockVector3;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class ParkourLocation {
	int x;
	int y;
	int z;

	public ParkourLocation(final BlockVector3 vector) {
		this(vector.getX(), vector.getY(), vector.getZ());
	}
}
