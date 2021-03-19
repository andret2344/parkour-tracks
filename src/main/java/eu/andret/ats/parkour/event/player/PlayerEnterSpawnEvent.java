/*
 * Copyright Andret (c) 2019. Copying and modifying allowed only keeping git link reference.
 */
package eu.andret.ats.parkour.event.player;

import eu.andret.ats.parkour.parkour.ParkourGame;
import eu.andret.ats.parkour.player.ParkourPlayer;
import lombok.EqualsAndHashCode;
import lombok.Value;

/**
 * The event that is called when player achieves checkpoint
 */
@Value
@EqualsAndHashCode(callSuper = true)
public class PlayerEnterSpawnEvent extends AbstractParkourPlayerEvent {
	/**
	 * Constructor.
	 *
	 * @param parkourGame The game that player is in.
	 * @param parkourPlayer The player that triggers the event.
	 */
	public PlayerEnterSpawnEvent(final ParkourGame parkourGame, final ParkourPlayer parkourPlayer) {
		super(parkourGame, parkourPlayer);
	}
}
