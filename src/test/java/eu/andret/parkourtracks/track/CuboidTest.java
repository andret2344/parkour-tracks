package eu.andret.parkourtracks.track;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CuboidTest {
	@Test
	void ofSortsCorners() {
		// when
		final Cuboid cuboid = Cuboid.of(5, 10, -3, 1, 2, 7);

		// then
		assertThat(cuboid).isEqualTo(new Cuboid(1, 2, -3, 5, 10, 7));
	}

	@Test
	void rejectsMinimumAboveMaximum() {
		// when / then
		assertThatThrownBy(() -> new Cuboid(1, 0, 0, 0, 0, 0)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new Cuboid(0, 1, 0, 0, 0, 0)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new Cuboid(0, 0, 1, 0, 0, 0)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void containsPointsOfBlocksWithBothCornersIncluded() {
		// given
		final Cuboid cuboid = new Cuboid(0, 0, 0, 2, 2, 2);

		// when / then
		assertThat(cuboid.contains(0.0, 0.0, 0.0)).isTrue();
		assertThat(cuboid.contains(2.99, 2.99, 2.99)).isTrue();
		assertThat(cuboid.contains(3.0, 1.0, 1.0)).isFalse();
		assertThat(cuboid.contains(1.0, 3.0, 1.0)).isFalse();
		assertThat(cuboid.contains(1.0, 1.0, 3.0)).isFalse();
	}

	@Test
	void containsUsesTheBlockBelowNegativeCoordinates() {
		// given
		final Cuboid cuboid = new Cuboid(-1, -1, -1, -1, -1, -1);

		// when / then
		assertThat(cuboid.contains(-0.5, -0.5, -0.5)).isTrue();
		assertThat(cuboid.contains(-1.5, -0.5, -0.5)).isFalse();
		assertThat(cuboid.contains(0.0, -0.5, -0.5)).isFalse();
	}

	@Test
	void containsCuboidOnlyWhenWhollyInside() {
		// given
		final Cuboid cuboid = new Cuboid(0, 0, 0, 10, 10, 10);

		// when / then
		assertThat(cuboid.contains(new Cuboid(0, 0, 0, 10, 10, 10))).isTrue();
		assertThat(cuboid.contains(new Cuboid(2, 2, 2, 3, 3, 3))).isTrue();
		assertThat(cuboid.contains(new Cuboid(5, 5, 5, 11, 5, 5))).isFalse();
		assertThat(cuboid.contains(new Cuboid(-1, 5, 5, 5, 5, 5))).isFalse();
	}

	@Test
	void overlapsWhenSharingABlock() {
		// given
		final Cuboid cuboid = new Cuboid(0, 0, 0, 10, 10, 10);

		// when / then
		assertThat(cuboid.overlaps(new Cuboid(10, 10, 10, 20, 20, 20))).isTrue();
		assertThat(cuboid.overlaps(new Cuboid(-5, 2, 2, 0, 3, 3))).isTrue();
		assertThat(cuboid.overlaps(new Cuboid(11, 0, 0, 20, 10, 10))).isFalse();
		assertThat(cuboid.overlaps(new Cuboid(0, 11, 0, 10, 20, 10))).isFalse();
		assertThat(cuboid.overlaps(new Cuboid(0, 0, -10, 10, 10, -1))).isFalse();
	}
}
