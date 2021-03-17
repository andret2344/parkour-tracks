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
public final class PlayerTeleportBackEvent extends AbstractParkourPlayerEvent {
	/**
	 * The checkpoint the player was teleported to.
	 */
	private Checkpoint checkpoint;

	/**
	 * Constructor.
	 *
	 * @param parkour The game that player is in.
	 * @param player The player that triggers the event.
	 * @param checkpoint The checkpoint the player is teleported to.
	 */
	public PlayerTeleportBackEvent(ParkourGame parkour, ParkourPlayer player, Checkpoint checkpoint) {
		super(parkour, player);
		this.checkpoint = checkpoint;
	}
}
