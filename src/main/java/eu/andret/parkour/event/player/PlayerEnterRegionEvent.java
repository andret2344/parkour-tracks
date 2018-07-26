package eu.andret.parkour.event.player;

import eu.andret.parkour.parkour.ParkourGame;
import eu.andret.parkour.region.AbstractRegion;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

@Value
@EqualsAndHashCode(callSuper = true)
public final class PlayerEnterRegionEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private Player player;
    private ParkourGame parkour;
    private AbstractRegion region;

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
