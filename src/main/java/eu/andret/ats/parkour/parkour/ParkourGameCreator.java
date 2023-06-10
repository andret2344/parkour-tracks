/*
 * Copyright Andret (c) 2018. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.parkour;

import com.google.gson.InstanceCreator;
import com.sk89q.worldedit.math.BlockVector3;
import eu.andret.ats.parkour.ParkourPlugin;
import eu.andret.ats.parkour.region.BasicRegion;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.UUID;

@AllArgsConstructor
public class ParkourGameCreator implements InstanceCreator<ParkourGame> {
	@NotNull
	private final ParkourPlugin plugin;

	@NotNull
	@Override
	public ParkourGame createInstance(final Type type) {
		// Random data just to create the instance, it will be overwritten during deserialization
		return new Parkour(
				UUID.randomUUID(),
				"",
				new BasicRegion(BlockVector3.ZERO, BlockVector3.ZERO),
				plugin.getServer().getWorlds().get(0));
	}
}
