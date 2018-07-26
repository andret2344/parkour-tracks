package eu.andret.parkour.event.game;

import eu.andret.parkour.parkour.ParkourGame;
import lombok.EqualsAndHashCode;

/**
 * The event that is called when Parkour starts.
 */
@EqualsAndHashCode(callSuper = true)
public final class GameStartEvent extends AbstractGameEvent {
    /**
     * Constructor.
     *
     * @param parkour The game that has been started.
     */
    public GameStartEvent(final ParkourGame parkour) {
        super(parkour);
    }
}
