package eu.andret.parkourtracks.track;

import org.jetbrains.annotations.NotNull;

/**
 * A potion effect players get for the whole game, by the key of its type (e.g. {@code minecraft:jump_boost}).
 */
public record TrackEffect(@NotNull String type, int amplifier) {
	public TrackEffect {
		if (amplifier < 0) {
			throw new IllegalArgumentException("The amplifier of an effect cannot be negative");
		}
	}
}
