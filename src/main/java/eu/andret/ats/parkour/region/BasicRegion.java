/*
 * Copyright Andret (c) 2019-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.region;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.NonFinal;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

@Value
@NonFinal
@AllArgsConstructor
public class BasicRegion {
	@NotNull
	@NonNull
	CuboidRegion region;

	public boolean contains(final Location location) {
		if (location == null) {
			return false;
		}
		return region.contains(BlockVector3.at(location.getX(), location.getY(), location.getZ()));
	}
}
