/*
 * Copyright Andret (c) 2018-2022. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.event.player;

import eu.andret.ats.parkour.parkour.ParkourGame;
import eu.andret.ats.parkour.region.BasicRegion;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * The event that is called when player enters the region.
 */
@Value
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class PlayerEnterRegionEvent extends AbstractPlayerEvent {
	/**
	 * The region that player entered.
	 */
	@NotNull
	BasicRegion region;

	/**
	 * Constructor.
	 *
	 * @param game The game that player is in.
	 * @param player The player that triggers the event.
	 * @param region The region that player came in.
	 */
	public PlayerEnterRegionEvent(@NotNull final ParkourGame game, @NotNull final Player player, @NotNull final BasicRegion region) {
		super(game, player);
		this.region = region;
	}
}
