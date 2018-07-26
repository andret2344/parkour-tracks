package eu.andret.parkour.tasks;

import eu.andret.parkour.ParkourPlugin;
import eu.andret.parkour.util.Data;

public class KeepConnection implements Runnable {

    @Override
    public void run() {
        try {
            ParkourPlugin.getInstance().getConnection().prepareStatement("SELECT id FROM " + Data.TABLE_RECORDS + " WHERE id<0").executeQuery();
        } catch (Exception ignored) {
        }
    }
}
