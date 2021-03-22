/*
 * Copyright Andret (c) 2019-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.event.player;

import eu.andret.ats.parkour.parkour.ParkourGame;
import eu.andret.ats.parkour.player.ParkourPlayer;
import lombok.EqualsAndHashCode;
import lombok.Value;

/**
 * Event that is called when player leaves the parkour game.
 */
@Value
@EqualsAndHashCode(callSuper = true)
public class PlayerQuitGameEvent extends AbstractParkourPlayerEvent {
	/**
	 * Constructor.
	 *
	 * @param parkourGame The game that player is in.
	 * @param parkourPlayer The player that triggers the event.
	 */
	public PlayerQuitGameEvent(final ParkourGame parkourGame, final ParkourPlayer parkourPlayer) {
		super(parkourGame, parkourPlayer);
	}
}
