package eu.andret.atsparkour.event;

import eu.andret.atsparkour.parkour.ParkourGame;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class PlayerCompleteParkourEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private Player player;
    private ParkourGame parkour;

    public PlayerCompleteParkourEvent(Player player, ParkourGame parkour) {
        this.player = player;
        this.parkour = parkour;
    }

    public Player getPlayer() {
        return player;
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