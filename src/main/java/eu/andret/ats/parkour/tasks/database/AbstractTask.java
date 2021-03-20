/*
 * Copyright Andret (c) 2019-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.tasks.database;

import lombok.AllArgsConstructor;

import java.sql.Connection;

@AllArgsConstructor
public abstract class AbstractTask implements Runnable {
	protected final Connection connection;
}
