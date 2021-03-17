/*
 * Copyright Andret (c) 2019. Copying and modifying allowed only keeping git link reference.
 */
package eu.andret.ats.parkour.region;

import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.bukkit.World;

@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = true)
public class GameRegion extends AbstractRegion {
	public GameRegion(final CuboidRegion region) {
		super(region);
	}

	public GameRegion(final World world) {
		this(new CuboidRegion(new BukkitWorld(world), BlockVector3.ZERO, BlockVector3.ZERO));
	}
}
