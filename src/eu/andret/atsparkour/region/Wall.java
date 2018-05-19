package eu.andret.atsparkour.region;

import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.AbstractWorld;

import java.util.Map;

public class Wall extends AbstractRegion {

    public Wall(CuboidRegion cuboidregion) {
        super(cuboidregion);
    }

    public Wall(AbstractWorld l, Map<String, Object> m) {
        super(l, m);
    }
}
