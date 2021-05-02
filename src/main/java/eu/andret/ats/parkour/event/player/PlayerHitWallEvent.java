/*
 * Copyright Andret (c) 2019-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.event.player;

import eu.andret.ats.parkour.parkour.ParkourGame;
import eu.andret.ats.parkour.player.ParkourPlayer;
import eu.andret.ats.parkour.region.BasicRegion;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import org.jetbrains.annotations.NotNull;

/**
 * The event that is called when player hits the wall
 */
@Value
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class PlayerHitWallEvent extends AbstractParkourPlayerEvent {
	/**
	 * The wall the player hit.
	 */
	@NotNull
	BasicRegion region;

	/**
	 * Constructor.
	 *
	 * @param game The game that player is in.
	 * @param player The player that triggers the event.
	 * @param region The wall the player hit.
	 */
	public PlayerHitWallEvent(@NotNull final ParkourGame game, @NotNull final ParkourPlayer player, @NotNull final BasicRegion region) {
		super(game, player);
		this.region = region;
	}
}
