/*
 * Copyright Andret (c) 2019. Copying and modifying allowed only keeping git link reference.
 */
package eu.andret.parkour.region;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.regions.CuboidRegion;
import eu.andret.parkour.YmlSerializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.bukkit.Location;

import java.util.Map;
import java.util.TreeMap;

@Data
@AllArgsConstructor
public abstract class AbstractRegion implements YmlSerializable {
	private CuboidRegion region;

	public boolean contains(Location loc) {
		return region.contains(new Vector(loc.getX(), loc.getY(), loc.getZ()));
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
