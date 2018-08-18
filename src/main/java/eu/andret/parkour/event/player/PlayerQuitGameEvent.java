package eu.andret.parkour.event.player;

import eu.andret.parkour.parkour.ParkourGame;
import eu.andret.parkour.player.ParkourPlayer;
import lombok.EqualsAndHashCode;
import lombok.Value;

/**
 * Event that is called when player leaves the parkour game.
 */
@Value
@EqualsAndHashCode(callSuper = true)
public final class PlayerQuitGameEvent extends AbstractParkourPlayerEvent {
    /**
     * Constructor.
     *
     * @param parkour The game that player is in.
     * @param player  The player that triggers the event.
     */
    public PlayerQuitGameEvent(ParkourGame parkour, ParkourPlayer player) {
        super(parkour, player);
    }
}