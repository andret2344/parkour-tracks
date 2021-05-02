/*
 * Copyright Andret (c) 2019-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.tasks.database;

import eu.andret.ats.parkour.parkour.ParkourGame;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;

public abstract class AbstractParkourTask extends AbstractTask {
	@NotNull
	protected final ParkourGame game;

	protected AbstractParkourTask(@NotNull final Connection connection, @NotNull final ParkourGame game) {
		super(connection);
		this.game = game;
	}
}
