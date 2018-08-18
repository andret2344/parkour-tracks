package eu.andret.parkour.util;

import eu.andret.parkour.tasks.TeleportCount;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SchedulerManager {
    public static final Map<UUID, Integer> COUNT_TIME = new HashMap<>();
    public static final Map<UUID, Integer> TELEPORT_COUNT = new HashMap<>();
    public static final Map<UUID, TeleportCount> TELEPORT_COUNT_2 = new HashMap<>();
    public static final Map<UUID, BukkitTask> AUTHOR_TASK = new HashMap<>();

    private SchedulerManager() {
    }
}
