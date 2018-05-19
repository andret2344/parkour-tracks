package eu.andret.atsparkour.tasks;

import eu.andret.atsparkour.atsParkour;
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
            pl.sendMessage(atsParkour.msg("10sek", false));
        }
        if (i == 0) {
            atsParkour.getInstance().getListeners().stopScheduling(pl);
        }
        i--;
    }

    public int getLeastTime() {
        return i;
    }
}
