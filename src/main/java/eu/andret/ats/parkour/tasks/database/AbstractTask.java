/*
 * Copyright Andret Tools System (c) 2025. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.tasks.database;

import eu.andret.ats.parkour.ParkourPlugin;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;

@AllArgsConstructor
public abstract class AbstractTask implements Runnable {
	@NotNull
	protected final ParkourPlugin plugin;

	public abstract void go(@NotNull Connection connection);

	@Override
	public final void run() {
		plugin.getConnection().ifPresent(this::go);
	}
}
