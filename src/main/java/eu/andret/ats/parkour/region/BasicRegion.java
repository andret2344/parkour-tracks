/*
 * Copyright Andret (c) 2018-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.region;

import com.sk89q.worldedit.regions.CuboidRegion;
import eu.andret.ats.parkour.player.ParkourPlayer;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.experimental.NonFinal;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@Value
@NonFinal
@AllArgsConstructor
public class BasicRegion {
	@NotNull
	protected ParkourLocation pos1;
	@NotNull
	protected ParkourLocation pos2;

	public BasicRegion(final CuboidRegion cuboidRegion) {
		this(new ParkourLocation(cuboidRegion.getPos1()), new ParkourLocation(cuboidRegion.getPos2()));
	}

	public boolean contains(@Nullable final Location location) {
		return Optional.ofNullable(location)
				.filter(loc -> isBetween(pos1.getX(), pos2.getX(), loc.getX()))
				.filter(loc -> isBetween(pos1.getY(), pos2.getY(), loc.getY()))
				.filter(loc -> isBetween(pos1.getZ(), pos2.getZ(), loc.getZ()))
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

	private boolean isBetween(final double left, final double right, final double value) {
		final double min = Math.min(left, right);
		final double max = Math.max(left, right);
		return value >= min && value <= max;
	}
}
