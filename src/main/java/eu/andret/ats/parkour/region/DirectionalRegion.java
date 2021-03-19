package eu.andret.ats.parkour.region;

import com.sk89q.worldedit.regions.CuboidRegion;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.NonFinal;

@Value
@NonFinal
@EqualsAndHashCode(callSuper = true)
public class DirectionalRegion extends AbstractRegion {
	double yaw;
	double pitch;

	public DirectionalRegion(final CuboidRegion cuboidRegion, final double yaw, final double pitch) {
		super(cuboidRegion);
		this.yaw = yaw;
		this.pitch = pitch;
	}
}
