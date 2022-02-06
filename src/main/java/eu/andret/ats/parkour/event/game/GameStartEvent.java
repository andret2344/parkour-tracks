/*
 * Copyright Andret (c) 2018-2022. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.event.game;

import eu.andret.ats.parkour.parkour.ParkourGame;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import org.jetbrains.annotations.NotNull;

/**
 * The event that is called when Parkour starts.
 */
@Value
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class GameStartEvent extends AbstractGameEvent {
	/**
	 * Constructor.
	 *
	 * @param game The game that has been started.
	 */
	public GameStartEvent(@NotNull final ParkourGame game) {
		super(game);
	}
}
