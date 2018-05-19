package eu.andret.atsparkour.region;

import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.AbstractWorld;

import java.util.Map;

public class GameRegion extends AbstractRegion {

    public GameRegion(CuboidRegion cuboidregion) {
        super(cuboidregion);
    }

    public GameRegion(AbstractWorld l, Map<String, Object> m) {
        super(l, m);
    }
}