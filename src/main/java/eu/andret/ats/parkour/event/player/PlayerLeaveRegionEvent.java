/*
 *  Copyright Andret (c) 2018-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.event.player;

import eu.andret.ats.parkour.parkour.ParkourGame;
import eu.andret.ats.parkour.region.BasicRegion;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.bukkit.entity.Player;

/**
 * Event that is called when player leaves the region.
 */
@Value
@EqualsAndHashCode(callSuper = true)
public class PlayerLeaveRegionEvent extends AbstractPlayerEvent {
	/**
	 * The region that player left.
	 */
	BasicRegion region;

	/**
	 * Constructor.
	 *
	 * @param game The game that player is in.
	 * @param player The player that triggers the event.
	 * @param region The region the player left.
	 */
	public PlayerLeaveRegionEvent(final ParkourGame game, final Player player, final BasicRegion region) {
		super(game, player);
		this.region = region;
	}
}
