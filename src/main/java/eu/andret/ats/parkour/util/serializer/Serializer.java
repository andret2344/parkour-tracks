/*
 * Copyright Andret (c) 2019-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.util.serializer;

import eu.andret.ats.parkour.parkour.ParkourManager;
import org.jetbrains.annotations.NotNull;

public interface Serializer<E> {
	@NotNull
	ParkourManager.ParkourSetting readParkourSetting(@NotNull final E e);

	@NotNull
	E writeParkourSetting(@NotNull ParkourManager.ParkourSetting setting);
}
