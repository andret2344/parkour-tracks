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
import org.jetbrains.annotations.NotNull;

@Value
@NonFinal
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class DirectionalRegion extends ParkourRegion {
	double yaw;
	double pitch;

	public DirectionalRegion(@NotNull final ParkourLocation pos1, @NotNull final ParkourLocation pos2, @NotNull final ParkourGame parkourGame, final double yaw, final double pitch) {
		super(pos1, pos2, parkourGame);
		this.yaw = yaw;
		this.pitch = pitch;
	}

	public DirectionalRegion(final CuboidRegion cuboidRegion, final @NotNull ParkourGame parkourGame, final double yaw, final double pitch) {
		this(new ParkourLocation(cuboidRegion.getPos1()), new ParkourLocation(cuboidRegion.getPos2()), parkourGame, yaw, pitch);
	}
}
