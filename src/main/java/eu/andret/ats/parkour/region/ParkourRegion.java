/*
 * Copyright Andret (c) 2018-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.region;

import com.sk89q.worldedit.regions.CuboidRegion;
import eu.andret.ats.parkour.parkour.ParkourGame;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.NonFinal;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

@Value
@NonFinal
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class ParkourRegion extends BasicRegion {
	@NotNull
	ParkourGame parkourGame;

	public ParkourRegion(@NotNull final BasicLocation pos1, @NotNull final BasicLocation pos2, @NotNull final ParkourGame parkourGame) {
		super(pos1, pos2);
		this.parkourGame = parkourGame;
	}

	public ParkourRegion(@NotNull final CuboidRegion cuboidRegion, @NotNull final ParkourGame parkourGame) {
		this(new BasicLocation(cuboidRegion.getPos1()), new BasicLocation(cuboidRegion.getPos2()), parkourGame);
	}

	public Location getCenter() {
		return new Location(parkourGame.getWorld(),
				(pos1.getX() + pos2.getX()) / 2d,
				(pos1.getY() + pos2.getY()) / 2d,
				(pos1.getZ() + pos2.getZ()) / 2d);
	}
}
