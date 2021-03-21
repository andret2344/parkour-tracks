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
 * The event that is called when player enters the region.
 */
@Value
@EqualsAndHashCode(callSuper = true)
public class PlayerEnterRegionEvent extends AbstractPlayerEvent {
	/**
	 * The region that player entered.
	 */
	BasicRegion basicRegion;

	/**
	 * Constructor.
	 *
	 * @param parkourGame The game that player is in.
	 * @param player The player that triggers the event.
	 * @param basicRegion The region that player came in.
	 */
	public PlayerEnterRegionEvent(final ParkourGame parkourGame, final Player player, final BasicRegion basicRegion) {
		super(parkourGame, player);
		this.basicRegion = basicRegion;
	}
}
