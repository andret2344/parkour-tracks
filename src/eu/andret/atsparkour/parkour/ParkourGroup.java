package eu.andret.atsparkour.parkour;

import eu.andret.atsparkour.player.PlayerManager;
import eu.andret.atsparkour.region.GameRegion;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ParkourGroup extends ParkourGame {
    private final List<Parkour> parkours = new ArrayList<>();

    public ParkourGroup(String name, GameRegion gameregion, World world) {
        super(name, gameregion, world);
    }

    public void addParkour(Parkour e) {
        parkours.add(e);
    }

    public boolean removeParkour(Parkour e) {
        return parkours.remove(e);
    }

    @Override
    public void addPlayer(Player player) {
        super.addPlayer(PlayerManager.getParkourCompetitorPlayer(player));
    }
}