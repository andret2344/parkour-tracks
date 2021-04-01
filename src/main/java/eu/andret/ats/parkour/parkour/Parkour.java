/*
 * Copyright Andret (c) 2019-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.parkour;

import eu.andret.ats.parkour.region.BasicRegion;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import org.bukkit.World;

@Value
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
class Parkour extends ParkourGame {
	public Parkour(final String name, final BasicRegion region, final World world) {
		super(name, region, world);
	}
}
