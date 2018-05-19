package eu.andret.atsparkour.event;

import eu.andret.atsparkour.parkour.ParkourGame;
import eu.andret.atsparkour.region.Checkpoint;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class PlayerAchieveCheckpointEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private Player player;
    private ParkourGame parkour;
    private int nr;

    public PlayerAchieveCheckpointEvent(Player player, ParkourGame parkour, int nr) {
        this.player = player;
        this.parkour = parkour;
        this.nr = nr;
    }

    public Player getPlayer() {
        return player;
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