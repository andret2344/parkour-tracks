package eu.andret.parkour.parkour;

import eu.andret.parkour.player.PlayerManager;
import eu.andret.parkour.region.GameRegion;
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