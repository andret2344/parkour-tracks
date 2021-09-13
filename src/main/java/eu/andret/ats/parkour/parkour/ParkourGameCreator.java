/*
 * Copyright Andret (c) 2018-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.parkour;

import com.google.gson.InstanceCreator;

import java.lang.reflect.Type;

public class ParkourGameCreator implements InstanceCreator<ParkourGame> {
	@Override
	public ParkourGame createInstance(final Type type) {
		//noinspection ConstantConditions
		return new Parkour(null, null, null);
	}
}
