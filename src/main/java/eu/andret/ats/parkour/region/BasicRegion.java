/*
 *  Copyright Andret (c) 2018-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.region;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import eu.andret.ats.parkour.player.ParkourPlayer;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.NonFinal;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@Value
@NonFinal
@AllArgsConstructor
public class BasicRegion {
	@NotNull
	@NonNull
	CuboidRegion region;

	public boolean contains(final Location location) {
		if (location == null) {
			return false;
		}
		return region.contains(BlockVector3.at(location.getX(), location.getY(), location.getZ()));
	}

	public boolean contains(final Player player) {
		if (player == null) {
			return false;
		}
		return contains(player.getLocation());
	}

	public boolean contains(final ParkourPlayer player) {
		if (player == null) {
			return false;
		}
		return contains(player.getPlayer());
	}
}
