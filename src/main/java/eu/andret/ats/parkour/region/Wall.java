/*
 * Copyright Andret (c) 2018-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.region;

import com.sk89q.worldedit.regions.CuboidRegion;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import org.jetbrains.annotations.NotNull;

@Value
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Wall extends BasicRegion {
	public Wall(@NotNull final CuboidRegion cuboidRegion) {
		super(cuboidRegion);
	}
}
