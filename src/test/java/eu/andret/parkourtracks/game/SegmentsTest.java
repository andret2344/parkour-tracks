package eu.andret.parkourtracks.game;

import eu.andret.parkourtracks.track.Cuboid;
import org.bukkit.Location;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SegmentsTest {
	static Location at(final double x, final double y, final double z) {
		return new Location(null, x, y, z);
	}

	@Test
	void findsWhereAMoveFirstTouchesTheCuboid() {
		// given: the blocks x 5, so the space from x 5 to 6
		final Cuboid cuboid = new Cuboid(5, 0, 0, 5, 2, 2);

		// when / then
		assertThat(Segments.findEntry(cuboid, at(3, 1, 1), at(7, 1, 1))).hasValue(0.5);
		assertThat(Segments.findEntry(cuboid, at(7, 1, 1), at(3, 1, 1))).hasValue(0.25);
		assertThat(Segments.findEntry(cuboid, at(5.5, 1, 1), at(7, 1, 1))).hasValue(0);
	}

	@Test
	void missesWhenTheMoveDoesNotReachOrPassesBeside() {
		// given
		final Cuboid cuboid = new Cuboid(5, 0, 0, 5, 2, 2);

		// when / then
		assertThat(Segments.findEntry(cuboid, at(1, 1, 1), at(4.5, 1, 1))).isEmpty();
		assertThat(Segments.findEntry(cuboid, at(3, 1, 5), at(7, 1, 5))).isEmpty();
		assertThat(Segments.findEntry(cuboid, at(3, 5, 1), at(7, 5, 1))).isEmpty();
	}

	@Test
	void handlesDiagonalMoves() {
		// given
		final Cuboid cuboid = new Cuboid(0, 0, 0, 0, 0, 0);

		// when / then
		assertThat(Segments.findEntry(cuboid, at(-1, -1, 0.5), at(1, 1, 0.5))).hasValue(0.5);
		assertThat(Segments.findEntry(cuboid, at(-1, 1.5, 0.5), at(1.5, -1, 0.5))).isPresent();
		assertThat(Segments.findEntry(cuboid, at(-1, 3.5, 0.5), at(3, -0.5, 0.5))).isEmpty();
	}
}
