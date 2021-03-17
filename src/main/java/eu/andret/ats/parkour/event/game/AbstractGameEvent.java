/*
 * Copyright Andret (c) 2019. Copying and modifying allowed only keeping git link reference.
 */
package eu.andret.ats.parkour.event.game;

import eu.andret.ats.parkour.parkour.ParkourGame;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Aggregating class.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
public class AbstractGameEvent extends Event {
	/**
	 * List of all Handlers.
	 */
	private static final HandlerList HANDLERS = new HandlerList();
	
	/**
	 * The Parkour that has been started.
	 */
	private ParkourGame parkour;

	@NotNull
	@Override
	public final HandlerList getHandlers() {
		return getHandlerList();
	}

	public static HandlerList getHandlerList() {
		return HANDLERS;
	}
}
