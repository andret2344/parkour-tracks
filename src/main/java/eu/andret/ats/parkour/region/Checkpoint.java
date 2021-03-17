/*
 * Copyright Andret (c) 2019. Copying and modifying allowed only keeping git link reference.
 */
package eu.andret.ats.parkour.region;

import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.NonFinal;
import org.bukkit.World;
import org.json.JSONObject;

@Value
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
		this(new CuboidRegion(new BukkitWorld(world), BlockVector3.ZERO, BlockVector3.ZERO), yaw, pitch);
	}

	public Checkpoint(CuboidRegion region) {
		this(region, 0, 0);
	}

	public Checkpoint(World world) {
		this(world, 0, 0);
	}

	@Override
	public JSONObject toJSON() {
		JSONObject object = super.toJSON();
		object.put("yaw", yaw);
		object.put("pitch", pitch);
		return object;
	}

	@Override
	public void fromJSON(JSONObject object) {
		super.fromJSON(object);
		yaw = object.getFloat("yaw");
		pitch = object.getFloat("pitch");
	}
}
