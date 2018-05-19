package eu.andret.atsparkour.region;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.AbstractWorld;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public abstract class AbstractRegion {
    private CuboidRegion region;

    public AbstractRegion(CuboidRegion cuboidregion) {
        region = cuboidregion;
    }

    public AbstractRegion(AbstractWorld l, Map<String, Object> m) {
        this(new CuboidRegion(l,
                        new Vector(
                                (double) m.get("x1"),
                                (double) m.get("y1"),
                                (double) m.get("z1")),
                        new Vector(
                                (double) m.get("x2"),
                                (double) m.get("y2"),
                                (double) m.get("z2"))
                )
        );
    }

    public CuboidRegion getRegion() {
        return region;
    }

    public boolean contains(Location loc) {
        return region.contains(new Vector(loc.getX(), loc.getY(), loc.getZ()));
    }

    public boolean contains(Player player) {
        return contains(player.getLocation());
    }

    public void updateRegion(CuboidRegion cuboidregion) {
        region = cuboidregion;
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
    public String toString() {
        return getClass().getSimpleName() + "{"
                + "x1=" + region.getPos1().getBlockX() + ", "
                + "y1=" + region.getPos1().getBlockY() + ", "
                + "z1=" + region.getPos1().getBlockZ() + ", "
                + "x2=" + region.getPos2().getBlockX() + ", "
                + "y2=" + region.getPos2().getBlockY() + ", "
                + "z2=" + region.getPos2().getBlockZ() + "}";
    }

    public Map<String, Object> toYamlStructure() {
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
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (o == this) {
            return true;
        }
        if (!(o instanceof AbstractRegion)) {
            return false;
        }
        AbstractRegion a = (AbstractRegion) o;
        Vector av1 = a.region.getPos1(), av2 = a.region.getPos2(), tv1 = region.getPos1(), tv2 = region.getPos2();
        return
                av1.getX() == tv1.getX() &&
                        av1.getY() == tv1.getY() &&
                        av1.getZ() == tv1.getZ() &&
                        av2.getX() == tv2.getX() &&
                        av2.getY() == tv2.getY() &&
                        av2.getZ() == tv2.getZ();
    }
}
