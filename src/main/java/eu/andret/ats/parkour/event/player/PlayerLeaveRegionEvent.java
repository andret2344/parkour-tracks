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
 * Event that is called when player leaves the region.
 */
@Value
@EqualsAndHashCode(callSuper = true)
public class PlayerLeaveRegionEvent extends AbstractPlayerEvent {
	/**
	 * The region that player left.
	 */
	AbstractRegion abstractRegion;

	/**
	 * Constructor.
	 *
	 * @param parkourGame The game that player is in.
	 * @param player The player that triggers the event.
	 * @param abstractRegion The region the player left.
	 */
	public PlayerLeaveRegionEvent(final ParkourGame parkourGame, final Player player, final AbstractRegion abstractRegion) {
		super(parkourGame, player);
		this.abstractRegion = abstractRegion;
	}
}
