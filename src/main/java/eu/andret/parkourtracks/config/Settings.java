package eu.andret.parkourtracks.config;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

/**
 * The content of {@code config.yml}.
 *
 * @param medals             the medals of the server, best first
 * @param finishDelaySeconds how long a player who finished waits before being sent to the lobby
 * @param timerDisplay       where the running time is shown
 */
public record Settings(@NotNull List<Medal> medals, int finishDelaySeconds, @NotNull TimerDisplay timerDisplay) {
	public Settings {
		medals = List.copyOf(medals);
	}

	@NotNull
	public Optional<Medal> findMedal(@NotNull final String key) {
		return medals.stream()
				.filter(medal -> medal.key().equals(key))
				.findFirst();
	}
}
