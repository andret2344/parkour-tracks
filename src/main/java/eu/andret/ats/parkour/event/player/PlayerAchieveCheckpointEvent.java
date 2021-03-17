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
 * The event that is called when player achieves checkpoint
 */
@Value
@EqualsAndHashCode(callSuper = true)
public class PlayerAchieveCheckpointEvent extends AbstractParkourPlayerEvent {
	/**
	 * The checkpoint achieved by the player.
	 */
	Checkpoint checkpoint;

	/**
	 * Constructor.
	 *
	 * @param parkour The game that player is in.
	 * @param player The player that triggers the event.
	 * @param checkpoint The achieved checkpoint.
	 */
	public PlayerAchieveCheckpointEvent(final ParkourGame parkour, final ParkourPlayer player, final Checkpoint checkpoint) {
		super(parkour, player);
		this.checkpoint = checkpoint;
	}
}
