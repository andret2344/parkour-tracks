package eu.andret.parkour.region;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.BukkitWorld;
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
    private float yaw;
    @NonFinal
    private float pitch;

    public Checkpoint(CuboidRegion region, float yaw, float pitch) {
        super(region);
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public Checkpoint(World world, float yaw, float pitch) {
        this(new CuboidRegion((com.sk89q.worldedit.world.World) new BukkitWorld(world), Vector.ZERO, Vector.ZERO), yaw, pitch);
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
        String yawString = String.valueOf(structure.get("yaw"));
        String pitchString = String.valueOf(structure.get("pitch"));
        if (yawString.matches("[0-9]+(\\.[0-9]+)?")) {
            yaw = Float.valueOf(yawString);
        }
        if (pitchString.matches("[0-9]+(\\.[0-9]+)?")) {
            pitch = Float.valueOf(pitchString);
        }
    }
}
