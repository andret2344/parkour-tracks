package eu.andret.parkour.parkour;

import eu.andret.parkour.player.PlayerManager;
import eu.andret.parkour.region.GameRegion;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.bukkit.World;
import org.bukkit.entity.Player;

@Value
@EqualsAndHashCode(callSuper = true)
public class Parkour extends ParkourGame {

    public Parkour(String name, GameRegion gameRegion, World world) {
        super(name, gameRegion, world);
    }

    @Override
    public void addPlayer(Player player) {
        super.addPlayer(PlayerManager.getParkourSinglePlayer(player));
    }
}
