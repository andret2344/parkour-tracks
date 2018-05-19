package eu.andret.atsparkour.event;

import eu.andret.atsparkour.parkour.ParkourGame;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class GameStopEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private ParkourGame parkour;

    public GameStopEvent(ParkourGame parkour) {
        this.parkour = parkour;
    }

    public ParkourGame getGame() {
        return parkour;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}