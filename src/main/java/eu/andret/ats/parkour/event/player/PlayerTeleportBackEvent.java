/*
 * Copyright Andret (c) 2019. Copying and modifying allowed only keeping git link reference.
 */
package eu.andret.ats.parkour.event.player;

import eu.andret.ats.parkour.parkour.ParkourGame;
import eu.andret.ats.parkour.player.ParkourPlayer;
import eu.andret.ats.parkour.region.Checkpoint;
import lombok.EqualsAndHashCode;
import lombok.Value;

/**
 * The event that is called when player is being teleported back.
 */
@Value
@EqualsAndHashCode(callSuper = true)
public class PlayerTeleportBackEvent extends AbstractParkourPlayerEvent {
	/**
	 * The checkpoint the player was teleported to.
	 */
	Checkpoint checkpoint;

	/**
	 * Constructor.
	 *
	 * @param parkourGame The game that player is in.
	 * @param parkourPlayer The player that triggers the event.
	 * @param checkpoint The checkpoint the player is teleported to.
	 */
	public PlayerTeleportBackEvent(final ParkourGame parkourGame, final ParkourPlayer parkourPlayer, final Checkpoint checkpoint) {
		super(parkourGame, parkourPlayer);
		this.checkpoint = checkpoint;
	}
}
