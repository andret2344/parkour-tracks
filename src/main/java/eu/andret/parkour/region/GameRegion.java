package eu.andret.parkour.region;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.BukkitWorld;
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
        this(new CuboidRegion((com.sk89q.worldedit.world.World) new BukkitWorld(world), Vector.ZERO, Vector.ZERO));
    }
}