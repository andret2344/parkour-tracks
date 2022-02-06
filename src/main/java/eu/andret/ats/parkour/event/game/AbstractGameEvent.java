/*
 * Copyright Andret (c) 2018-2022. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.event.game;

import eu.andret.ats.parkour.parkour.ParkourGame;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.NonFinal;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Aggregating class.
 */
@Value
@NonFinal
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class AbstractGameEvent extends Event {
	/**
	 * List of all Handlers.
	 */
	private static final HandlerList HANDLERS = new HandlerList();

	/**
	 * The Parkour that has been started.
	 */
	@NotNull
	ParkourGame game;

	@NotNull
	@Override
	public final HandlerList getHandlers() {
		return getHandlerList();
	}

	public static HandlerList getHandlerList() {
		return HANDLERS;
	}
}
