/*
 * Copyright Andret (c) 2019. Copying and modifying allowed only keeping git link reference.
 */
package eu.andret.ats.parkour.event.player;

import eu.andret.ats.parkour.parkour.ParkourGame;
import eu.andret.ats.parkour.player.ParkourPlayer;
import eu.andret.ats.parkour.region.Wall;
import lombok.EqualsAndHashCode;
import lombok.Value;

/**
 * The event that is called when player hits the wall
 */
@Value
@EqualsAndHashCode(callSuper = true)
public class PlayerHitWallEvent extends AbstractParkourPlayerEvent {
	/**
	 * The wall the player hit.
	 */
	Wall wall;

	/**
	 * Constructor.
	 *
	 * @param parkourGame The game that player is in.
	 * @param parkourPlayer The player that triggers the event.
	 * @param wall The wall the player hit.
	 */
	public PlayerHitWallEvent(final ParkourGame parkourGame, final ParkourPlayer parkourPlayer, final Wall wall) {
		super(parkourGame, parkourPlayer);
		this.wall = wall;
	}
}
