package eu.andret.parkour.region;

import com.sk89q.worldedit.regions.CuboidRegion;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.NonFinal;
import org.bukkit.World;

import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class Checkpoint extends AbstractRegion {
    @NonFinal
    private float yaw, pitch;

    public Checkpoint(CuboidRegion region, float yaw, float pitch) {
        super(region);
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public Checkpoint(World world, float yaw, float pitch) {
        super(world);
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public Checkpoint(CuboidRegion region) {
        this(region, 0, 0);
    }

    public Checkpoint(World world) {
        this(world, 0, 0);
    }

    @Override
    public Map<String, Object> toYmlStructure() {
        Map<String, Object> map = super.toYmlStructure();
        map.put("yaw", yaw);
        map.put("pitch", pitch);
        return map;
    }

    @Override
    public void fromYmlStructure(Map<String, Object> structure) {
        super.fromYmlStructure(structure);
        float yaw, pitch;
        try {
            yaw = Float.valueOf("" + structure.get("yaw"));
        } catch (Exception ex) {
            yaw = 0;
        }
        try {
            pitch = Float.valueOf("" + structure.get("pitch"));
        } catch (Exception ex) {
            pitch = 0;
        }
        this.yaw = yaw;
        this.pitch = pitch;
    }
}
