package eu.andret.parkour.player;

import eu.andret.parkour.Parkour;
import eu.andret.parkour.parkour.ParkourGame;
import eu.andret.parkour.parkour.ParkourManager;
import eu.andret.parkour.region.Checkpoint;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class ParkourSinglePlayer extends ParkourPlayer implements Runnable {
    private float time = 0;
    private int i = 0;

    ParkourSinglePlayer(Player player) {
        super(player);
    }

    public float getTime() {
        return time;
    }

    public void setTime(float time) {
        this.time = time;
    }

    @Override
    public boolean equals(Object o) {
        if (!super.equals(o)) {
            return false;
        }
        if (o == this) {
            return true;
        }
        if (!(o instanceof ParkourSinglePlayer)) {
            return false;
        }
        return player.equals(((ParkourSinglePlayer) o).player);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{name=" + player.getName() + ", time=" + time + ", lastCheckpoint=" + lastCheckpoint + ", ignoring=" + ignoring + ", spectating=" + spectating + "}";
    }

    @Override
    public void run() {
        ParkourGame p = ParkourManager.getParkour(player);
        if (ignoring || !PlayerManager.playerExists(player) || p == null || !p.isRunning() || spectating) {
            return;
        }
        for (Checkpoint c : p.getCheckpointList()) {
            if (c.contains(player)) {
                return;
            }
        }
        if (p.getSpawn().contains(player)) {
            i = 0;
        }
        if (!Parkour.getInstance().getListeners().getTeleportCounts().containsKey(player.getUniqueId())) {
            time = (i++) / 20F;
        }
        if (i == 1 && !spectating) {
            player.playSound(player.getLocation(), Sound.BLOCK_LEVER_CLICK, 0.5F, 0.5F);
        }
        player.setLevel((int) time);
        player.setExp(time % 1);
    }

    @Override
    public void reset() {
        super.reset();
        time = 0;
        i = 0;
    }
}
