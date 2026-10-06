package eu.andret.parkourtracks.economy;

import eu.andret.parkourtracks.config.Medal;
import eu.andret.parkourtracks.track.MedalThreshold;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Which medal rewards a time is owed: every medal whose threshold has a time the run did not exceed, with a reward,
 * not paid yet. A run good for gold is good for silver too, so it can be owed several.
 */
public final class MedalPayouts {
	private MedalPayouts() {
	}

	/**
	 * A medal reward owed.
	 */
	public record Due(@NotNull Medal medal, double amount) {
	}

	@NotNull
	public static List<Due> due(@NotNull final List<Medal> medals, @NotNull final Map<String, MedalThreshold> thresholds,
								final int ticks, @NotNull final Set<String> paid) {
		return medals.stream()
				.filter(medal -> !paid.contains(medal.key()))
				.filter(medal -> {
					final MedalThreshold threshold = thresholds.get(medal.key());
					return threshold != null && threshold.hasTime() && ticks <= threshold.ticks() && threshold.reward() > 0;
				})
				.map(medal -> new Due(medal, thresholds.get(medal.key()).reward()))
				.toList();
	}
}
