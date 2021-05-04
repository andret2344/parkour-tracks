/*
 *  Copyright Andret (c) 2018-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.region;

import com.sk89q.worldedit.regions.CuboidRegion;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.NonFinal;
import org.jetbrains.annotations.NotNull;

@Value
@NonFinal
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class DirectionalRegion extends BasicRegion {
	double yaw;
	double pitch;

	public DirectionalRegion(@NotNull final CuboidRegion cuboidRegion, final double yaw, final double pitch) {
		super(cuboidRegion);
		this.yaw = yaw;
		this.pitch = pitch;
	}
}
