/*
 * Copyright Andret (c) 2018-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.parkour;

import eu.andret.ats.parkour.region.BasicRegion;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

@Value
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
class Parkour extends ParkourGame {
	Parkour(final int id, @NotNull final String name, @NotNull final BasicRegion region, @NotNull final World world) {
		super(id, name, region, world);
	}
}
