/*
 * Copyright Andret (c) 2019. Copying and modifying allowed only keeping git link reference.
 */
package eu.andret.ats.parkour.event.player;

import eu.andret.ats.parkour.parkour.ParkourGame;
import eu.andret.ats.parkour.region.AbstractRegion;
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
	AbstractRegion region;

	/**
	 * Constructor.
	 *
	 * @param parkour The game that player is in.
	 * @param player The player that triggers the event.
	 * @param region The regoin that player came in.
	 */
	public PlayerEnterRegionEvent(final ParkourGame parkour, final Player player, final AbstractRegion region) {
		super(parkour, player);
		this.region = region;
	}
}
