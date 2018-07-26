package eu.andret.parkour.region;


import com.sk89q.worldedit.regions.CuboidRegion;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bukkit.World;

@Data
@EqualsAndHashCode(callSuper = true)
public class Wall extends AbstractRegion {
    public Wall(CuboidRegion region) {
        super(region);
    }

    public Wall(World world) {
        super(world);
    }
}
