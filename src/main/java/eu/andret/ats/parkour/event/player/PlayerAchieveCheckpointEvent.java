/*
 * Copyright Andret (c) 2019-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.event.player;

import eu.andret.ats.parkour.parkour.ParkourGame;
import eu.andret.ats.parkour.player.ParkourPlayer;
import eu.andret.ats.parkour.region.DirectionalRegion;
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
	DirectionalRegion checkpointRegion;

	/**
	 * Constructor.
	 *
	 * @param parkourGame The game that player is in.
	 * @param parkourPlayer The player that triggers the event.
	 * @param checkpointRegion The achieved checkpoint.
	 */
	public PlayerAchieveCheckpointEvent(final ParkourGame parkourGame, final ParkourPlayer parkourPlayer, final DirectionalRegion checkpointRegion) {
		super(parkourGame, parkourPlayer);
		this.checkpointRegion = checkpointRegion;
	}
}
