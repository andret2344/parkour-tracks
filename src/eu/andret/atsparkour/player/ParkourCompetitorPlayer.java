package eu.andret.atsparkour.player;

import org.bukkit.entity.Player;

public class ParkourCompetitorPlayer extends ParkourPlayer {
    private int falls = 0, completes = 0;

    ParkourCompetitorPlayer(Player player) {
        super(player);
    }

    public int getFalls() {
        return falls;
    }

    public int getCompletes() {
        return completes;
    }

    public void addFall() {
        falls++;
    }

    public void addComplete() {
        completes++;
    }

    @Override
    public boolean equals(Object o) {
        if (!super.equals(o)) {
            return false;
        }
        if (o == this) {
            return true;
        }
        if (!(o instanceof ParkourCompetitorPlayer)) {
            return false;
        }
        return player.equals(((ParkourCompetitorPlayer) o).player);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() +
                "{name=" + player.getName() +
                ", lastcheckpoint=" + lastcheckpoint +
                ", ignoring=" + ignoring +
                ", completes=" + completes +
                ", falls=" + falls +
                ", spectating=" + spectating + "}";
    }

    @Override
    public void reset() {
        super.reset();
        completes = 0;
        falls = 0;
    }
}
