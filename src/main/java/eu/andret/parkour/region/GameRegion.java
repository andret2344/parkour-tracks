package eu.andret.parkour.region;

import com.sk89q.worldedit.regions.CuboidRegion;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = true)
public class GameRegion extends AbstractRegion {
    public GameRegion(CuboidRegion region) {
        super(region);
    }
}