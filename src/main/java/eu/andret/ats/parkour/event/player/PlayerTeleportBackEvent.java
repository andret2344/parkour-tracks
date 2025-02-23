/*
 * Copyright Andret Tools System (c) 2025. Copying and modifying allowed only keeping git link reference.
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
 * The event that is called when player is being teleported back.
 */
@Value
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class PlayerTeleportBackEvent extends AbstractParkourPlayerEvent {
	/**
	 * The checkpoint the player was teleported to.
	 */
	@NotNull
	Checkpoint checkpoint;

	/**
	 * Constructor.
	 *
	 * @param game       The game that player is in.
	 * @param player     The player that triggers the event.
	 * @param checkpoint The checkpoint the player is teleported to.
	 */
	public PlayerTeleportBackEvent(@NotNull final ParkourGame game, @NotNull final ParkourPlayer player,
								   @NotNull final Checkpoint checkpoint) {
		super(game, player);
		this.checkpoint = checkpoint;
	}
}
