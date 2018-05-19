package eu.andret.atsparkour.event;

import eu.andret.atsparkour.parkour.ParkourGame;
import eu.andret.atsparkour.region.AbstractRegion;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class PlayerEnterRegoinEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private Player player;
    private ParkourGame parkour;
    private AbstractRegion region;

    public PlayerEnterRegoinEvent(Player player, ParkourGame parkour, AbstractRegion region) {
        this.player = player;
        this.parkour = parkour;
        this.region = region;
    }

    public AbstractRegion getRegion() {
        return region;
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
