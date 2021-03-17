/*
 * Copyright Andret (c) 2019. Copying and modifying allowed only keeping git link reference.
 */
package eu.andret.ats.parkour.player;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.bukkit.entity.Player;

@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = true)
public class ParkourCompetitorPlayer extends ParkourPlayer {
	@Setter(AccessLevel.NONE)
	private int falls = 0;
	@Setter(AccessLevel.NONE)
	private int completes = 0;

	ParkourCompetitorPlayer(Player player) {
		super(player);
	}

	public void addFall() {
		falls++;
	}

	public void addComplete() {
		completes++;
	}

	@Override
	public void reset() {
		super.reset();
		completes = 0;
		falls = 0;
	}
}
