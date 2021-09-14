/*
 * Copyright Andret (c) 2018-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.parkour;

import com.google.gson.InstanceCreator;
import eu.andret.ats.parkour.ParkourPlugin;
import eu.andret.ats.parkour.region.BasicLocation;
import eu.andret.ats.parkour.region.BasicRegion;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;

@AllArgsConstructor
public class ParkourGameCreator implements InstanceCreator<ParkourGame> {
	@NotNull
	private final ParkourPlugin plugin;

	@NotNull
	@Override
	public ParkourGame createInstance(final Type type) {
		// Random data just to create the instance, it will be overwritten during deserialization
		return new Parkour(
				"",
				new BasicRegion(new BasicLocation(0, 0, 0), new BasicLocation(0, 0, 0)),
				plugin.getServer().getWorlds().get(0));
	}
}
