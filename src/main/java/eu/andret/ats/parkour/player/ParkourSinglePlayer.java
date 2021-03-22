/*
 * Copyright Andret (c) 2019-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.player;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.bukkit.entity.Player;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public final class ParkourSinglePlayer extends ParkourPlayer {
	ParkourSinglePlayer(final Player player) {
		super(player);
	}
}
