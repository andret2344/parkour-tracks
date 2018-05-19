package eu.andret.atsparkour.event;

import eu.andret.atsparkour.parkour.ParkourGame;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class PlayerTeleportBackEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private Player player;
    private ParkourGame parkour;
    private int id;

    public PlayerTeleportBackEvent(Player player, ParkourGame parkour, int id) {
        this.player = player;
        this.parkour = parkour;
        this.id = id;
    }

    public Player getPlayer() {
        return player;
    }

    public ParkourGame getGame() {
        return parkour;
    }

    public int getCheckpointId() {
        return id;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}