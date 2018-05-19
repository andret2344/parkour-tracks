package eu.andret.atsparkour.event;

import eu.andret.atsparkour.parkour.ParkourGame;
import eu.andret.atsparkour.region.Checkpoint;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

public final class PlayerEnterEffectRegionEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private Player player;
    private ParkourGame parkour;
    private int nr;

    public PlayerEnterEffectRegionEvent(Player player, ParkourGame parkour, int nr) {
        this.player = player;
        this.parkour = parkour;
        this.nr = nr;
    }

    public Player getPlayer() {
        return player;
    }

    public List<PotionEffectType> getAdditableEffects() {
        return parkour.getEffectRegionList().get(nr).getEffectsToAdd();
    }

    public List<PotionEffectType> getRemovableEffects() {
        return parkour.getEffectRegionList().get(nr).getEffectsToRemove();
    }

    public ParkourGame getGame() {
        return parkour;
    }

    public int getCheckpointId() {
        return nr;
    }

    public Checkpoint getCheckpointLocation() {
        return parkour.getCheckpoint(nr);
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
