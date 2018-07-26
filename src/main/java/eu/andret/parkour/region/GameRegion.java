package eu.andret.parkour.region;

import com.sk89q.worldedit.regions.CuboidRegion;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bukkit.World;

@Data
@EqualsAndHashCode(callSuper = true)
public class GameRegion extends AbstractRegion {
    public GameRegion(CuboidRegion region) {
        super(region);
    }

    public GameRegion(World world) {
        super(world);
    }
}