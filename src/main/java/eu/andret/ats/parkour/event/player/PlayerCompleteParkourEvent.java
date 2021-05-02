/*
 * Copyright Andret (c) 2019-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.event.player;

import eu.andret.ats.parkour.parkour.ParkourGame;
import eu.andret.ats.parkour.player.ParkourPlayer;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import org.jetbrains.annotations.NotNull;

/**
 * The event that triggers when player achieves last checkpoint
 */
@Value
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class PlayerCompleteParkourEvent extends AbstractParkourPlayerEvent {
	/**
	 * Constructor.
	 *
	 * @param game The game that player is in.
	 * @param player The player that triggers the event.
	 */
	public PlayerCompleteParkourEvent(@NotNull final ParkourGame game, @NotNull final ParkourPlayer player) {
		super(game, player);
	}
}
