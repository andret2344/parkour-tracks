package eu.andret.parkour.tasks;

import eu.andret.parkour.ParkourPlugin;
import org.bukkit.entity.Player;

public class TeleportCount implements Runnable {
    private Player pl;
    private int i = 5;

    public TeleportCount(Player player) {
        pl = player;
    }

    @Override
    public void run() {
        if (i == 5) {
            pl.sendMessage(ParkourPlugin.msg("10sek", false));
        }
        if (i == 0) {
            ParkourPlugin.getInstance().getListeners().stopScheduling(pl);
        }
        i--;
    }

    public int getLeastTime() {
        return i;
    }
}
