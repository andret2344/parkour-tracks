package eu.andret.parkour.tasks;

import eu.andret.parkour.ParkourPlugin;
import eu.andret.parkour.parkour.ParkourGame;
import eu.andret.parkour.util.Data;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.function.Consumer;

public class ParkourDataOperations implements Runnable {
    private ParkourGame parkour;
    private Connection sql = ParkourPlugin.getInstance().getConnection();
    private float time, bestTime;
    private Consumer<ParkourDataOperations> callback;
    private Player player;
    private int count, earned = 0;

    public ParkourDataOperations(ParkourGame parkour, Player player, Consumer<ParkourDataOperations> callback) {
        this.parkour = parkour;
        this.player = player;
        this.callback = callback;
    }

    @Override
    public void run() {
        try {
            PreparedStatement stat = sql.prepareStatement(String.format("SELECT time FROM %s "
                    + "WHERE parkour=? ORDER BY time LIMIT 1", Data.TABLE_RECORDS));
            stat.setString(1, parkour.getName());
            ResultSet rs = stat.executeQuery();
            time = rs.next() ? rs.getFloat("time") : 0;

            stat = sql.prepareStatement(String.format("SELECT * FROM %s "
                    + "WHERE parkour=? AND nick=?", Data.TABLE_RECORDS));
            stat.setString(1, parkour.getName());
            stat.setString(2, player.getName());
            rs = stat.executeQuery();
            if (rs.next()) {
                bestTime = rs.getFloat("time");
                count = rs.getInt("count");
                earned = rs.getInt("earned");
            } else {
                bestTime = 0;
                count = 0;
                earned = 0;
            }
            callback.accept(this);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public float getTime() {
        return time;
    }

    public float getBestTime() {
        return bestTime;
    }

    public int getCount() {
        return count;
    }

    public int getEarned() {
        return earned;
    }
}
