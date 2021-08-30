/*
 * Copyright Andret (c) 2018-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.player;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@Value
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
class ParkourSinglePlayer extends ParkourPlayer {
	ParkourSinglePlayer(@NotNull final Player player) {
		super(player);
	}
}
