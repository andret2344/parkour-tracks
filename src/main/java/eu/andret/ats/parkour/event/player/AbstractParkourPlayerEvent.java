/*
 * Copyright Andret (c) 2019-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.event.player;

import eu.andret.ats.parkour.parkour.ParkourGame;
import eu.andret.ats.parkour.player.ParkourPlayer;
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
public class AbstractParkourPlayerEvent extends Event {
	/**
	 * List of all Handlers.
	 */
	private static final HandlerList HANDLERS = new HandlerList();

	/**
	 * The Parkour that has been started.
	 */
	private ParkourGame game;

	/**
	 * The player that triggered the event.
	 */
	private ParkourPlayer player;

	@NotNull
	@Override
	public final HandlerList getHandlers() {
		return getHandlerList();
	}

	public static HandlerList getHandlerList() {
		return HANDLERS;
	}
}
