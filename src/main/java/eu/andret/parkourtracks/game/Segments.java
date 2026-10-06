package eu.andret.parkourtracks.game;

import eu.andret.parkourtracks.track.Cuboid;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.util.OptionalDouble;

/**
 * Where a straight move first touches a cuboid, so a move too fast to stop inside a thin region still crosses it.
 */
public final class Segments {
	private Segments() {
	}

	/**
	 * How far along the move from {@code from} to {@code to} it first touches the blocks of the cuboid: 0 at
	 * {@code from}, 1 at {@code to}.
	 *
	 * @return the fraction of the move, or empty when the move does not touch the cuboid
	 */
	@NotNull
	public static OptionalDouble entry(@NotNull final Cuboid cuboid, @NotNull final Location from,
									   @NotNull final Location to) {
		final double[] start = {from.getX(), from.getY(), from.getZ()};
		final double[] delta = {to.getX() - from.getX(), to.getY() - from.getY(), to.getZ() - from.getZ()};
		// The blocks of the cuboid span from the minimum corner to one past the maximum corner
		final double[] min = {cuboid.minX(), cuboid.minY(), cuboid.minZ()};
		final double[] max = {cuboid.maxX() + 1, cuboid.maxY() + 1, cuboid.maxZ() + 1};
		double enter = 0;
		double exit = 1;
		for (int axis = 0; axis < 3; axis++) {
			if (delta[axis] == 0) {
				if (start[axis] < min[axis] || start[axis] >= max[axis]) {
					return OptionalDouble.empty();
				}
				continue;
			}
			final double first = (min[axis] - start[axis]) / delta[axis];
			final double second = (max[axis] - start[axis]) / delta[axis];
			enter = Math.max(enter, Math.min(first, second));
			exit = Math.min(exit, Math.max(first, second));
			if (enter > exit) {
				return OptionalDouble.empty();
			}
		}
		return OptionalDouble.of(enter);
	}
}
