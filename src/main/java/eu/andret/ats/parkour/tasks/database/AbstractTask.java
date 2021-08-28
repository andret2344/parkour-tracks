/*
 * Copyright Andret (c) 2018-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.tasks.database;

import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;

@AllArgsConstructor
public abstract class AbstractTask implements Runnable {
	@NotNull
	protected final Connection connection;
}
