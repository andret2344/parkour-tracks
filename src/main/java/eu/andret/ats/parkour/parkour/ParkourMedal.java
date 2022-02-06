/*
 * Copyright Andret (c) 2018-2022. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.parkour;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ParkourMedal {
	private final Medal medal;
	private double time;
	private double reward;

	public ParkourMedal(final Medal medal) {
		this(medal, 0, 0);
	}

	public void setTime(final double time) {
		if (time < 0) {
			throw new IllegalArgumentException("Time cannot be negative, " + time + " provided");
		}
		this.time = time;
	}

	public void setReward(final double reward) {
		if (reward < 0) {
			throw new IllegalArgumentException("Reward cannot be negative, " + time + " provided");
		}
		this.reward = reward;
	}
}
