/*
 * Copyright Andret (c) 2018-2022. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.event.player;

import eu.andret.ats.parkour.parkour.ParkourGame;
import eu.andret.ats.parkour.player.ParkourPlayer;
import eu.andret.ats.parkour.region.Checkpoint;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import org.jetbrains.annotations.NotNull;

/**
 * The event that is called when player achieves checkpoint
 */
@Value
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class PlayerAchieveCheckpointEvent extends AbstractParkourPlayerEvent {
	/**
	 * The checkpoint achieved by the player.
	 */
	@NotNull
	Checkpoint checkpoint;

	/**
	 * Constructor.
	 *
	 * @param game The game that player is in.
	 * @param player The player that triggers the event.
	 * @param checkpoint The achieved checkpoint.
	 */
	public PlayerAchieveCheckpointEvent(@NotNull final ParkourGame game, @NotNull final ParkourPlayer player,
										@NotNull final Checkpoint checkpoint) {
		super(game, player);
		this.checkpoint = checkpoint;
	}
}
