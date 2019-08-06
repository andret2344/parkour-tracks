/*
 * Copyright Andret (c) 2019. Copying and modifying allowed only keeping git link reference.
 */
package eu.andret.parkour.region;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.regions.CuboidRegion;
import eu.andret.parkour.JSONSerializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.bukkit.Location;
import org.json.JSONObject;

@Data
@AllArgsConstructor
public abstract class AbstractRegion implements JSONSerializable {
	private CuboidRegion region;

	public boolean contains(Location loc) {
		return region.contains(new Vector(loc.getX(), loc.getY(), loc.getZ()));
	}

	@Override
	public JSONObject toJSON() {
		JSONObject object = new JSONObject();
		object.put("x1", region.getPos1().getX());
		object.put("y1", region.getPos1().getY());
		object.put("z1", region.getPos1().getZ());
		object.put("x2", region.getPos2().getX());
		object.put("y2", region.getPos2().getY());
		object.put("z2", region.getPos2().getZ());
		return object;
	}

	@Override
	public void fromJSON(JSONObject object) {
		region.setPos1(new Vector(
				object.getDouble("x1"),
				object.getDouble("y1"),
				object.getDouble("z1")));
		region.setPos2(new Vector(
				object.getDouble("x2"),
				object.getDouble("y2"),
				object.getDouble("z2")));
	}
}
