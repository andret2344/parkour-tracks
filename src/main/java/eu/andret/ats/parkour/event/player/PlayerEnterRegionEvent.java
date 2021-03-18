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
	AbstractRegion abstractRegion;

	/**
	 * Constructor.
	 *
	 * @param parkourGame The game that player is in.
	 * @param player The player that triggers the event.
	 * @param abstractRegion The regoin that player came in.
	 */
	public PlayerEnterRegionEvent(final ParkourGame parkourGame, final Player player, final AbstractRegion abstractRegion) {
		super(parkourGame, player);
		this.abstractRegion = abstractRegion;
	}
}
