/*
 * Copyright Andret (c) 2019. Copying and modifying allowed only keeping git link reference.
 */
package eu.andret.ats.parkour.util;

import eu.andret.ats.parkour.tasks.TeleportCount;
import lombok.experimental.UtilityClass;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@UtilityClass
public final class SchedulerManager {
	public final Map<UUID, Integer> COUNT_TIME = new HashMap<>();
	public final Map<UUID, Integer> TELEPORT_COUNT = new HashMap<>();
	public final Map<UUID, TeleportCount> TELEPORT_COUNT_2 = new HashMap<>();
	public final Map<UUID, BukkitTask> AUTHOR_TASK = new HashMap<>();
}
