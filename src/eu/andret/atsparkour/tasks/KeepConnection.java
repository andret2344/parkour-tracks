package eu.andret.atsparkour.tasks;

import eu.andret.atsparkour.atsParkour;
import eu.andret.atsparkour.data.Data;

public class KeepConnection implements Runnable {

    @Override
    public void run() {
        try {
            atsParkour.getInstance().getConnection().prepareStatement("SELECT id FROM " + Data.recordstable + " WHERE id<0").executeQuery();
        } catch (Exception ignored) {
        }
    }
}
