/*
 * Copyright Andret (c) 2018-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.region;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

@Value
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Checkpoint extends IdentifiableRegion {
	Location location;

	public Checkpoint(final int index, @NotNull final BlockVector3 pos1, @NotNull final BlockVector3 pos2,
					  @NotNull final Location location) {
		super(index, pos1, pos2);
		this.location = location;
	}

	public Checkpoint(final int index, @NotNull final CuboidRegion cuboidRegion, @NotNull final Location location) {
		this(index, cuboidRegion.getPos1(), cuboidRegion.getPos2(), location);
	}
}
