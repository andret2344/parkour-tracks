/*
 * Copyright Andret (c) 2018-2022. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.parkour;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

@Data
@AllArgsConstructor
public class ParkourMedal {
	@NotNull
	private final Medal medal;
	private double time;
	private double reward;

	public ParkourMedal(@NotNull final Medal medal) {
		this(medal, 0, 0);
	}

	public void setTime(final double time) {
		if (time < 0) {
			throw new IllegalArgumentException("Time cannot be negative, %s provided".formatted(time));
		}
		this.time = time;
	}

	public void setReward(final double reward) {
		if (reward < 0) {
			throw new IllegalArgumentException("Reward cannot be negative, %s provided".formatted(reward));
		}
		this.reward = reward;
	}
}
