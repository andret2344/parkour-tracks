package eu.andret.parkour.event.player;

import eu.andret.parkour.parkour.ParkourGame;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

@Value
@EqualsAndHashCode(callSuper = true)
public final class PlayerHitWallEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private Player player;
    private ParkourGame game;
    private int checkpointId;

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}