package eu.andret.ats.parkour.region;

import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.NonFinal;
import org.bukkit.World;

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

	public DirectionalRegion(final World world, final double yaw, final double pitch) {
		this(new CuboidRegion(new BukkitWorld(world), BlockVector3.ZERO, BlockVector3.ZERO), yaw, pitch);
	}
}
