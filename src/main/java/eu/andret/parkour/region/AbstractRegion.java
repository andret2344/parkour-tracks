package eu.andret.parkour.region;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.World;
import eu.andret.parkour.YmlSerializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

@Data
@AllArgsConstructor
public abstract class AbstractRegion implements YmlSerializable {
    private CuboidRegion region;

    public AbstractRegion(org.bukkit.World world) {
        this(new CuboidRegion((World) new BukkitWorld(world), Vector.ZERO, Vector.ZERO));
    }

    public boolean contains(Location loc) {
        return region.contains(new Vector(loc.getX(), loc.getY(), loc.getZ()));
    }

    @Deprecated
    public void fill(Material mat) {
        Vector v1 = region.getPos1();
        Vector v2 = region.getPos2();
        for (int x = v1.getBlockX(); x < v2.getBlockX(); x++) {
            for (int y = v1.getBlockY(); y < v2.getBlockY(); y++) {
                for (int z = v1.getBlockZ(); x < v2.getBlockZ(); x++) {
                    ((Block) Objects.requireNonNull(region.getWorld()).getBlock(new Vector(x, y, z))).setType(mat);
                }
            }
        }
    }

    @Override
    public Map<String, Object> toYmlStructure() {
        Map<String, Object> map = new TreeMap<>();
        map.put("x1", region.getPos1().getX());
        map.put("y1", region.getPos1().getY());
        map.put("z1", region.getPos1().getZ());
        map.put("x2", region.getPos2().getX());
        map.put("y2", region.getPos2().getY());
        map.put("z2", region.getPos2().getZ());
        return map;
    }

    @Override
    public void fromYmlStructure(Map<String, Object> structure) {
        region.setPos1(new Vector(
                (double) structure.get("x1"),
                (double) structure.get("y1"),
                (double) structure.get("z1")));
        region.setPos2(new Vector(
                (double) structure.get("x2"),
                (double) structure.get("y2"),
                (double) structure.get("z2")));
    }
}
