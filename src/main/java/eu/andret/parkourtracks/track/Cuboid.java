package eu.andret.parkourtracks.track;

import org.jetbrains.annotations.NotNull;

/**
 * A box of whole blocks, both corners included. It has no world: a track's regions all lie in the track's world.
 */
public record Cuboid(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
	public Cuboid {
		if (minX > maxX || minY > maxY || minZ > maxZ) {
			throw new IllegalArgumentException("The minimum corner of a cuboid cannot be above its maximum corner");
		}
	}

	/**
	 * Makes the cuboid spanned by two opposite corners given in any order.
	 */
	@NotNull
	public static Cuboid of(final int x1, final int y1, final int z1, final int x2, final int y2, final int z2) {
		return new Cuboid(Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2),
				Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2));
	}

	/**
	 * Whether the block containing the given point is part of this cuboid.
	 */
	public boolean contains(final double x, final double y, final double z) {
		return contains((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
	}

	public boolean contains(final int x, final int y, final int z) {
		return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
	}

	/**
	 * Whether the given cuboid lies wholly inside this one.
	 */
	public boolean contains(@NotNull final Cuboid other) {
		return contains(other.minX, other.minY, other.minZ) && contains(other.maxX, other.maxY, other.maxZ);
	}

	/**
	 * Whether the two cuboids share at least one block.
	 */
	public boolean overlaps(@NotNull final Cuboid other) {
		return minX <= other.maxX && maxX >= other.minX
				&& minY <= other.maxY && maxY >= other.minY
				&& minZ <= other.maxZ && maxZ >= other.minZ;
	}
}
