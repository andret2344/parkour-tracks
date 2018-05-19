package eu.andret.atsparkour.parkour;

import eu.andret.atsparkour.player.PlayerManager;
import eu.andret.atsparkour.region.GameRegion;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class Parkour extends ParkourGame {

    public Parkour(String name, GameRegion gameregion, World world) {
        super(name, gameregion, world);
    }

    @Override
    public void addPlayer(Player player) {
        super.addPlayer(PlayerManager.getParkourSinglePlayer(player));
    }
}