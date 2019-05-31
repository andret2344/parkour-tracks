/*
 * Copyright Andret (c) 2019. Copying and modifying allowed only keeping git link reference.
 */
package eu.andret.parkour.event.player;

import eu.andret.parkour.parkour.ParkourGame;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Aggregating class.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
public class AbstractPlayerEvent extends Event {
	/**
	 * List of all Handlers.
	 */
	private static final HandlerList HANDLERS = new HandlerList();
	/**
	 * The Parkour that has been started.
	 */
	private ParkourGame parkour;

	/**
	 * The player that triggered the event.
	 */
	private Player player;

	@Override
	public final HandlerList getHandlers() {
		return HANDLERS;
	}
}
