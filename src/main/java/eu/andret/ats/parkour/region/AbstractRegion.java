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
	private CuboidRegion cuboidRegion;

	public boolean contains(final Location location) {
		return cuboidRegion.contains(BlockVector3.at(location.getX(), location.getY(), location.getZ()));
	}

	@Override
	public JSONObject toJSON() {
		final JSONObject object = new JSONObject();
		object.put("x1", cuboidRegion.getPos1().getX());
		object.put("y1", cuboidRegion.getPos1().getY());
		object.put("z1", cuboidRegion.getPos1().getZ());
		object.put("x2", cuboidRegion.getPos2().getX());
		object.put("y2", cuboidRegion.getPos2().getY());
		object.put("z2", cuboidRegion.getPos2().getZ());
		return object;
	}

	@Override
	public void fromJSON(final JSONObject object) {
		cuboidRegion.setPos1(BlockVector3.at(
				object.getDouble("x1"),
				object.getDouble("y1"),
				object.getDouble("z1")));
		cuboidRegion.setPos2(BlockVector3.at(
				object.getDouble("x2"),
				object.getDouble("y2"),
				object.getDouble("z2")));
	}
}
