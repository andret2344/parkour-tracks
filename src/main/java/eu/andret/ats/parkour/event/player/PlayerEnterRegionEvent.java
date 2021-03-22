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
	BasicRegion region;

	/**
	 * Constructor.
	 *
	 * @param game The game that player is in.
	 * @param player The player that triggers the event.
	 * @param region The region that player came in.
	 */
	public PlayerEnterRegionEvent(final ParkourGame game, final Player player, final BasicRegion region) {
		super(game, player);
		this.region = region;
	}
}
