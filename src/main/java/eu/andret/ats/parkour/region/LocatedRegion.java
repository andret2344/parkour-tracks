/*
 * Copyright Andret (c) 2018-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.region;

import com.sk89q.worldedit.regions.CuboidRegion;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.NonFinal;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

@Value
@NonFinal
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class LocatedRegion extends BasicRegion {
	Location location;

	public LocatedRegion(@NotNull final BasicLocation pos1, @NotNull final BasicLocation pos2, @NotNull final Location location) {
		super(pos1, pos2);
		this.location = location;
	}

	public LocatedRegion(@NotNull final CuboidRegion cuboidRegion, @NotNull final Location location) {
		this(new BasicLocation(cuboidRegion.getPos1()), new BasicLocation(cuboidRegion.getPos2()), location);
	}
}
