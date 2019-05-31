/*
 * Copyright Andret (c) 2019. Copying and modifying allowed only keeping git link reference.
 */
package eu.andret.parkour.event.game;

import eu.andret.parkour.parkour.ParkourGame;
import lombok.EqualsAndHashCode;
import lombok.Value;

/**
 * The event that is called when Parkour finishes.
 */
@Value
@EqualsAndHashCode(callSuper = true)
public final class GameStopEvent extends AbstractGameEvent {
	/**
	 * Constructor.
	 *
	 * @param parkour The game that has been started.
	 */
	public GameStopEvent(ParkourGame parkour) {
		super(parkour);
	}
}
