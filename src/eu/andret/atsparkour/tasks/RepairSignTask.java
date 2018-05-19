package eu.andret.atsparkour.tasks;

import eu.andret.atsparkour.atsParkour;
import eu.andret.atsparkour.data.Data;
import eu.andret.atsparkour.parkour.ParkourGame;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class RepairSignTask implements Runnable {
    private ParkourGame parkour;
    private Connection sql = atsParkour.getInstance().getConnection();

    public RepairSignTask(ParkourGame parkour) {
        this.parkour = parkour;
    }

    @Override
    public void run() {
        try {
            PreparedStatement stat = sql.prepareStatement(String.format("SELECT * FROM %s "
                    + "WHERE parkour=? ORDER BY `time` ASC LIMIT 1", Data.recordstable));
            stat.setString(1, parkour.getName());
            ResultSet rs = stat.executeQuery();
            if (!rs.next()) {
                DataBaseOperations.updateSign("========", 0.00, parkour);
            } else {
                DataBaseOperations.updateSign(rs.getString("nick"), rs.getFloat("time"), parkour);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
