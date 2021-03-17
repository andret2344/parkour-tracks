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
public final class PlayerHitWallEvent extends AbstractParkourPlayerEvent {
	/**
	 * The wall the player hit.
	 */
	private Wall wall;

	/**
	 * Constructor.
	 *
	 * @param parkour The game that player is in.
	 * @param player The player that triggers the event.
	 * @param wall The wall the player hit.
	 */
	public PlayerHitWallEvent(ParkourGame parkour, ParkourPlayer player, Wall wall) {
		super(parkour, player);
		this.wall = wall;
	}
}
