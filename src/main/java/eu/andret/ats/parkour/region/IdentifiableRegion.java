/*
 * Copyright Andret (c) 2018-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.region;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public abstract class IdentifiableRegion extends BasicRegion {
	@Getter
	protected int index;

	protected IdentifiableRegion(final int index, @NotNull final CuboidRegion cuboidRegion) {
		this(index, cuboidRegion.getPos1(), cuboidRegion.getPos2());
	}

	protected IdentifiableRegion(final int index, @NotNull final BlockVector3 pos1, @NotNull final BlockVector3 pos2) {
		super(pos1, pos2);
		this.index = index;
	}
}
