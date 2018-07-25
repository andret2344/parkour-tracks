package eu.andret.parkour.tasks;

import eu.andret.parkour.Parkour;
import eu.andret.parkour.data.Data;

public class KeepConnection implements Runnable {

    @Override
    public void run() {
        try {
            Parkour.getInstance().getConnection().prepareStatement("SELECT id FROM " + Data.recordstable + " WHERE id<0").executeQuery();
        } catch (Exception ignored) {
        }
    }
}
