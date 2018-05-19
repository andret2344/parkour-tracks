package eu.andret.atsparkour.region;

import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.AbstractWorld;

import java.util.Map;

public class Checkpoint extends AbstractRegion {
    private float yaw, pitch;

    public Checkpoint(CuboidRegion cuboidregion, float yaw, float pitch) {
        super(cuboidregion);
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public Checkpoint(AbstractWorld l, Map<String, Object> m) {
        super(l, m);
        try {
            yaw = Float.valueOf("" + m.get("yaw"));
        } catch (Exception ex) {
            yaw = 0;
        }
        try {
            pitch = Float.valueOf("" + m.get("pitch"));
        } catch (Exception ex) {
            pitch = 0;
        }
    }

    public float getPitch() {
        return pitch;
    }

    public float getYaw() {
        return yaw;
    }

    @Override
    public Map<String, Object> toYamlStructure() {
        Map<String, Object> map = super.toYamlStructure();
        map.put("yaw", yaw);
        map.put("pitch", pitch);
        return map;
    }
}
