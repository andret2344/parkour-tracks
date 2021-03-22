/*
 * Copyright Andret (c) 2019-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.tasks.database;

import eu.andret.ats.parkour.parkour.ParkourGame;

import java.sql.Connection;

public abstract class AbstractParkourTask extends AbstractTask {
	protected final ParkourGame parkourGame;

	protected AbstractParkourTask(final Connection connection, final ParkourGame parkourGame) {
		super(connection);
		this.parkourGame = parkourGame;
	}
}
