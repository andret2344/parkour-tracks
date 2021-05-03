/*
 *  Copyright Andret (c) 2018-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.event.player;

import eu.andret.ats.parkour.parkour.ParkourGame;
import eu.andret.ats.parkour.player.ParkourPlayer;
import eu.andret.ats.parkour.region.DirectionalRegion;
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
	DirectionalRegion region;

	/**
	 * Constructor.
	 *
	 * @param game The game that player is in.
	 * @param player The player that triggers the event.
	 * @param region The checkpoint the player is teleported to.
	 */
	public PlayerTeleportBackEvent(final ParkourGame game, final ParkourPlayer player, final DirectionalRegion region) {
		super(game, player);
		this.region = region;
	}
}
