/*
 * Copyright Andret (c) 2019. Copying and modifying allowed only keeping git link reference.
 */
package eu.andret.ats.parkour.event.player;

import eu.andret.ats.parkour.parkour.ParkourGame;
import eu.andret.ats.parkour.player.ParkourPlayer;
import lombok.EqualsAndHashCode;
import lombok.Value;

/**
 * The event that is called when player joins the parkour game.
 */
@Value
@EqualsAndHashCode(callSuper = true)
public class PlayerJoinGameEvent extends AbstractParkourPlayerEvent {
	/**
	 * Constructor.
	 *
	 * @param parkour The game that player is in.
	 * @param player The player that triggers the event.
	 */
	public PlayerJoinGameEvent(final ParkourGame parkour, final ParkourPlayer player) {
		super(parkour, player);
	}
}
