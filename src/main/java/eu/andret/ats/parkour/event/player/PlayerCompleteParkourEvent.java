/*
 * Copyright Andret (c) 2019. Copying and modifying allowed only keeping git link reference.
 */
package eu.andret.ats.parkour.event.player;

import eu.andret.ats.parkour.parkour.ParkourGame;
import eu.andret.ats.parkour.player.ParkourPlayer;
import lombok.EqualsAndHashCode;
import lombok.Value;

/**
 * The event that triggers when player achieves last checkpoint
 */
@Value
@EqualsAndHashCode(callSuper = true)
public final class PlayerCompleteParkourEvent extends AbstractParkourPlayerEvent {
	/**
	 * Constructor.
	 *
	 * @param parkour The game that player is in.
	 * @param player The player that triggers the event.
	 */
	public PlayerCompleteParkourEvent(ParkourGame parkour, ParkourPlayer player) {
		super(parkour, player);
	}
}
