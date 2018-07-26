package eu.andret.parkour.tasks;

import eu.andret.parkour.ParkourPlugin;
import eu.andret.parkour.parkour.ParkourGame;
import eu.andret.parkour.util.Data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class TopPlayersDataOperations implements Runnable {
    private ParkourGame parkour;
    private Connection sql = ParkourPlugin.getInstance().getConnection();
    private int count;
    private final Map<String, Float> result = new HashMap<>();
    private Consumer<TopPlayersDataOperations> callback;

    public TopPlayersDataOperations(ParkourGame parkour, int count, Consumer<TopPlayersDataOperations> callback) {
        this.parkour = parkour;
        this.count = count;
        this.callback = callback;
    }

    @Override
    public void run() {
        try {
            PreparedStatement stat = sql.prepareStatement(String.format("SELECT nick, time FROM %s "
                    + "WHERE parkour=? ORDER BY time LIMIT 10", Data.TABLE_RECORDS));
            stat.setString(1, parkour.getName());
            ResultSet rs = stat.executeQuery();
            for (int i = 0; i < count && rs.next(); i++) {
                result.put(rs.getString("nick"), rs.getFloat("time"));
            }
            callback.accept(this);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public Map<String, Float> getResult() {
        return result;
    }
}
