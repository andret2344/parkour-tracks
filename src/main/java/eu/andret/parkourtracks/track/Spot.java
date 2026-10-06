package eu.andret.parkourtracks.track;

import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

/**
 * A position with the direction a player faces there, without a world.
 */
public record Spot(double x, double y, double z, float yaw, float pitch) {
	@NotNull
	public static Spot of(@NotNull final Location location) {
		return new Spot(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
	}

	@NotNull
	public Location toLocation(@NotNull final World world) {
		return new Location(world, x, y, z, yaw, pitch);
	}
}
