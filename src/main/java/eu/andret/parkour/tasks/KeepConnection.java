package eu.andret.parkour.tasks;

import eu.andret.parkour.ParkourPlugin;
import eu.andret.parkour.util.Data;
import lombok.AllArgsConstructor;
import org.bukkit.Bukkit;

@AllArgsConstructor
public class KeepConnection implements Runnable {
    private final ParkourPlugin plugin;

    @Override
    public void run() {
        try {
            plugin.getConnection().prepareStatement("SELECT id FROM " + Data.TABLE_RECORDS + " WHERE id<0").executeQuery();
        } catch (Exception ex) {
            Bukkit.getServer().getLogger().throwing(getClass().getName(), "run", ex);
        }
    }
}
