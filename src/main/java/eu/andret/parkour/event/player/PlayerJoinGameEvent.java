package eu.andret.parkour.event.player;

import eu.andret.parkour.parkour.ParkourGame;
import eu.andret.parkour.player.ParkourPlayer;
import lombok.EqualsAndHashCode;
import lombok.Value;

/**
 * The event that is called when player joins the parkour game.
 */
@Value
@EqualsAndHashCode(callSuper = true)
public final class PlayerJoinGameEvent extends AbstractParkourPlayerEvent {
    /**
     * Constructor.
     *
     * @param parkour The game that player is in.
     * @param player  The player that triggers the event.
     */
    public PlayerJoinGameEvent(ParkourGame parkour, ParkourPlayer player) {
        super(parkour, player);
    }
}