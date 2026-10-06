package eu.andret.parkourtracks.track;

/**
 * A track's requirement for one medal: a run of at most {@code ticks} ticks earns it, once, with {@code reward}.
 * A threshold without a time set yet has {@code ticks} 0 and is never awarded.
 */
public record MedalThreshold(int ticks, double reward) {
	public MedalThreshold {
		if (ticks < 0) {
			throw new IllegalArgumentException("The time of a medal cannot be negative");
		}
		if (reward < 0) {
			throw new IllegalArgumentException("The reward of a medal cannot be negative");
		}
	}

	public boolean hasTime() {
		return ticks > 0;
	}
}
