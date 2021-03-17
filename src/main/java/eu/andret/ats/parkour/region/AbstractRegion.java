/*
 * Copyright Andret (c) 2019. Copying and modifying allowed only keeping git link reference.
 */
package eu.andret.ats.parkour.region;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import eu.andret.ats.parkour.JSONSerializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.bukkit.Location;
import org.json.JSONObject;

@Data
@AllArgsConstructor
public abstract class AbstractRegion implements JSONSerializable {
	private CuboidRegion region;

	public boolean contains(final Location loc) {
		return region.contains(BlockVector3.at(loc.getX(), loc.getY(), loc.getZ()));
	}

	@Override
	public JSONObject toJSON() {
		final JSONObject object = new JSONObject();
		object.put("x1", region.getPos1().getX());
		object.put("y1", region.getPos1().getY());
		object.put("z1", region.getPos1().getZ());
		object.put("x2", region.getPos2().getX());
		object.put("y2", region.getPos2().getY());
		object.put("z2", region.getPos2().getZ());
		return object;
	}

	@Override
	public void fromJSON(final JSONObject object) {
		region.setPos1(BlockVector3.at(
				object.getDouble("x1"),
				object.getDouble("y1"),
				object.getDouble("z1")));
		region.setPos2(BlockVector3.at(
				object.getDouble("x2"),
				object.getDouble("y2"),
				object.getDouble("z2")));
	}
}
