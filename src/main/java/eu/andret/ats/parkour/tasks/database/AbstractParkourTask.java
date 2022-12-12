/*
 * Copyright Andret (c) 2018-2022. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.tasks.database;

import eu.andret.ats.parkour.ParkourPlugin;
import eu.andret.ats.parkour.parkour.ParkourGame;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractParkourTask extends AbstractTask {
	@NotNull
	protected final ParkourGame game;

	protected AbstractParkourTask(@NotNull final ParkourPlugin plugin, @NotNull final ParkourGame game) {
		super(plugin);
		this.game = game;
	}
}
