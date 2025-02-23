/*
 * Copyright Andret Tools System (c) 2025. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.region;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import eu.andret.ats.parkour.player.ParkourPlayer;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@Data
@RequiredArgsConstructor
public class BasicRegion {
	@NotNull
	protected final BlockVector3 pos1;
	@NotNull
	protected final BlockVector3 pos2;

	public BasicRegion(@NotNull final CuboidRegion cuboidRegion) {
		this(cuboidRegion.getPos1(), cuboidRegion.getPos2());
	}

	public boolean contains(@Nullable final Location location) {
		return Optional.ofNullable(location)
				.filter(loc -> toCuboidRegion().contains(BlockVector3.at(loc.getX(), loc.getY(), loc.getZ())))
				.isPresent();
	}

	public boolean contains(@Nullable final Player player) {
		return Optional.ofNullable(player)
				.map(Player::getLocation)
				.map(this::contains)
				.orElse(false);
	}

	public boolean contains(@Nullable final ParkourPlayer player) {
		return Optional.ofNullable(player)
				.map(ParkourPlayer::getPlayer)
				.map(this::contains)
				.orElse(false);
	}

	@NotNull
	public CuboidRegion toCuboidRegion() {
		return new CuboidRegion(pos1, pos2);
	}
}
