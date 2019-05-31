/*
 * Copyright Andret (c) 2019. Copying and modifying allowed only keeping git link reference.
 */
package eu.andret.parkour.event.player;

import eu.andret.parkour.parkour.ParkourGame;
import eu.andret.parkour.region.AbstractRegion;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.bukkit.entity.Player;

/**
 * Event that is called when player leaves the region.
 */
@Value
@EqualsAndHashCode(callSuper = true)
public final class PlayerLeaveRegionEvent extends AbstractPlayerEvent {
	/**
	 * The region that player leaved.
	 */
	private AbstractRegion region;

	/**
	 * Constructor.
	 *
	 * @param parkour The game that player is in.
	 * @param player The player that triggers the event.
	 * @param region The region the player leaved.
	 */
	public PlayerLeaveRegionEvent(ParkourGame parkour, Player player, AbstractRegion region) {
		super(parkour, player);
		this.region = region;
	}
}
