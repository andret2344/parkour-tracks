package eu.andret.parkourtracks.track;

import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * A spot in a world given by name, for places outside any track such as the lobby.
 */
public record WorldSpot(@NotNull String world, @NotNull Spot spot) {
	@NotNull
	public static WorldSpot of(@NotNull final Location location) {
		final World world = Objects.requireNonNull(location.getWorld(), "The location has no world");
		return new WorldSpot(world.getName(), Spot.of(location));
	}
}
