/*
 * Copyright Andret (c) 2019. Copying and modifying allowed only keeping git link reference.
 */
package eu.andret.parkour.region;

import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bukkit.World;

@Data
@EqualsAndHashCode(callSuper = true)
public class Wall extends AbstractRegion {
	public Wall(CuboidRegion region) {
		super(region);
	}

	public Wall(World world) {
		this(new CuboidRegion(new BukkitWorld(world), BlockVector3.ZERO, BlockVector3.ZERO));
	}
}
