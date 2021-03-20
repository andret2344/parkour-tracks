/*
 * Copyright Andret (c) 2019-2021. Copying and modifying allowed only keeping git link reference.
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
	BasicRegion basicRegion;

	/**
	 * Constructor.
	 *
	 * @param parkourGame The game that player is in.
	 * @param player The player that triggers the event.
	 * @param basicRegion The region the player left.
	 */
	public PlayerLeaveRegionEvent(final ParkourGame parkourGame, final Player player, final BasicRegion basicRegion) {
		super(parkourGame, player);
		this.basicRegion = basicRegion;
	}
}
