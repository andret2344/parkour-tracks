/*
 * Copyright Andret (c) 2018-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.region;

import com.sk89q.worldedit.math.BlockVector3;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.jetbrains.annotations.NotNull;

@Value
@AllArgsConstructor
public class BasicLocation {
	int x;
	int y;
	int z;

	public BasicLocation(@NotNull final BlockVector3 vector) {
		this(vector.getX(), vector.getY(), vector.getZ());
	}
	
	@NotNull
	public BlockVector3 toBlockVector3() {
		return BlockVector3.at(x, y, z);
	}
}
