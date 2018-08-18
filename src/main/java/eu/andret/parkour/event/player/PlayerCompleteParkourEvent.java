package eu.andret.parkour.event.player;

import eu.andret.parkour.parkour.ParkourGame;
import eu.andret.parkour.player.ParkourPlayer;
import lombok.EqualsAndHashCode;
import lombok.Value;

/**
 * The event that triggers when player achieves last checkpoint
 */
@Value
@EqualsAndHashCode(callSuper = true)
public final class PlayerCompleteParkourEvent extends AbstractParkourPlayerEvent {
    /**
     * Constructor.
     *
     * @param parkour The game that player is in.
     * @param player  The player that triggers the event.
     */
    public PlayerCompleteParkourEvent(ParkourGame parkour, ParkourPlayer player) {
        super(parkour, player);
    }
}