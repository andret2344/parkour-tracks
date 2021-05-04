/*
 *  Copyright Andret (c) 2018-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.util.serializer;

import eu.andret.ats.parkour.parkour.ParkourManager;

public interface Serializer<E> {
	ParkourManager.ParkourSetting readParkourSetting(final E e);

	E writeParkourSetting(ParkourManager.ParkourSetting setting);
}
