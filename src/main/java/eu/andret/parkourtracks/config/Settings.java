package eu.andret.parkourtracks.config;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

/**
 * The content of {@code config.yml}.
 *
 * @param medals                 the medals of the server, best first
 * @param finishDelaySeconds     how long a player who finished waits before being sent to the lobby
 * @param timerDisplay           where the running time is shown
 * @param sprintGraceTicks       how long a player on a sprint-forced track may stop sprinting before going back
 * @param gameItems              the game items turned on, with their slots
 * @param scoreboard             whether players in a game get the plugin's sidebar
 * @param backupFrequencyMinutes how often backups are made, 0 for never
 * @param backupKeep             how many backups are kept
 * @param refundOnStop           whether players get their fee back when an admin stops the track
 */
public record Settings(@NotNull List<Medal> medals, int finishDelaySeconds, @NotNull TimerDisplay timerDisplay,
					   int sprintGraceTicks, @NotNull List<GameItemSlot> gameItems, boolean scoreboard,
					   int backupFrequencyMinutes, int backupKeep, boolean refundOnStop) {
	public Settings {
		medals = List.copyOf(medals);
		gameItems = List.copyOf(gameItems);
	}

	@NotNull
	public Optional<Medal> findMedal(@NotNull final String key) {
		return medals.stream()
				.filter(medal -> medal.key().equals(key))
				.findFirst();
	}
}
