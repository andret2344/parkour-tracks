/*
 * Copyright Andret (c) 2019. Copying and modifying allowed only keeping git link reference.
 */
package eu.andret.parkour.player;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import org.bukkit.entity.Player;

@Data
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
