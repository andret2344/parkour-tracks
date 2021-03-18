/*
 * Copyright Andret (c) 2019. Copying and modifying allowed only keeping git link reference.
 */
package eu.andret.ats.parkour.event.game;

import eu.andret.ats.parkour.parkour.ParkourGame;
import lombok.EqualsAndHashCode;
import lombok.Value;

/**
 * The event that is called when Parkour finishes.
 */
@Value
@EqualsAndHashCode(callSuper = true)
public class GameStopEvent extends AbstractGameEvent {
	/**
	 * Constructor.
	 *
	 * @param parkourGame The game that has been started.
	 */
	public GameStopEvent(final ParkourGame parkourGame) {
		super(parkourGame);
	}
}
