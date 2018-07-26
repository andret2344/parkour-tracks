package eu.andret.parkour.tasks;

import eu.andret.parkour.ParkourPlugin;
import eu.andret.parkour.parkour.ParkourGame;
import eu.andret.parkour.parkour.ParkourGame.ParkourOptions;
import eu.andret.parkour.player.PlayerManager;
import eu.andret.parkour.util.Data;
import eu.andret.parkour.util.Medal;
import org.bukkit.Location;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;

import static eu.andret.parkour.ParkourPlugin.msg;

public class DataBaseOperations implements Runnable {
    private Player player;
    private ParkourGame parkour;
    private float time;
    private Medal lastMedal = Medal.NONE;
    private final Connection sql = ParkourPlugin.getInstance().getConnection();

    public DataBaseOperations(Player player, ParkourGame parkour, float time) {
        this.player = player;
        this.parkour = parkour;
        this.time = time;
    }

    @Override
    public void run() {
        ParkourOptions o = parkour.getOptions();
        try {
            //przeszedlem kolejny raz
            PreparedStatement stat;
            if (!player.hasPermission("ats.parkour.ignorerecords")) {
                //czy moj czas jest najlepszy?
                stat = sql.prepareStatement(String.format("SELECT time FROM %s "
                        + "WHERE parkour=? ORDER BY `time` ASC LIMIT 1", Data.TABLE_RECORDS));
                stat.setString(1, parkour.getName());
                ResultSet rs = stat.executeQuery();
                if (!rs.next() || rs.getFloat("time") > time) {
                    //tak, jest najlepszy lub nie bylo zadnego
                    player.sendMessage(msg("generalRecord", false));
                    updateSign(player.getName(), time, parkour);
                }
            }
            //biore wszystkie dane gracza
            stat = sql.prepareStatement(String.format("SELECT * FROM %s "
                    + "WHERE nick=? AND parkour=?", Data.TABLE_RECORDS));
            stat.setString(1, player.getName());
            stat.setString(2, parkour.getName());
            ResultSet rs = stat.executeQuery();

            int c = 0;
            //czy byl wpis?
            if (rs.next()) {
                c = rs.getInt("count");
                float f = rs.getFloat("time");
                String s;
                //czy najlepszy?
                if (f > time) {
                    s = "UPDATE %s SET time=" + time + ", `count`=?, `earned`=?, `date`=?, `xp`=? WHERE `nick`=? AND `parkour`=?";
                    player.sendMessage(msg("newRecord", false));
                } else {
                    s = "UPDATE %s SET `count`=?, `earned`=?, `date`=?, `xp`=? WHERE `nick`=? AND `parkour`=?";
                }
                stat = sql.prepareStatement(String.format(s, Data.TABLE_RECORDS));
                stat.setInt(1, c + 1);
                stat.setInt(2, rs.getInt("earned") + o.getRewardPrice());
                stat.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
                stat.setInt(4, rs.getInt("xp") + o.getXp());
                stat.setString(5, player.getName());
                stat.setString(6, parkour.getName());
                stat.execute();
                //a jaki byl ostatni medal?
                lastMedal = o.getMedalByTime(f);
            } else {
                stat = sql.prepareStatement(String.format("INSERT INTO %s VALUES(null, ?, ?, ?, ?, 1, ?, ?)", Data.TABLE_RECORDS));
                stat.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
                stat.setString(2, player.getName());
                stat.setString(3, parkour.getName());
                stat.setFloat(4, time);
                stat.setInt(5, o.getRewardPrice());
                stat.setInt(6, o.getXp());
                stat.execute();
                player.sendMessage(msg("newRecord", false));
            }
            player.sendMessage(msg("howMany", false).replace("%COUNT%", c + 1 + ""));
            //jaki dac medal?
            Medal current = o.getMedalByTime(PlayerManager.getParkourSinglePlayer(player).getTime());
            if (lastMedal.getId() > current.getId()) {
                //ile za niego i poprzednie zarabia?
                int price = 0;
                for (int i = current.getId(); i < lastMedal.getId(); i++) {
                    price += Medal.valueOf(i).getPrice();
                }
//				LobbyCoins.getInstance().addCoins(player.getName(), price);
                player.sendMessage(msg("achieveMedal", false).replace("%MEDAL%", current.getName()).replace("%PRICE%", "" + price));
                stat = sql.prepareStatement(String.format("UPDATE %s SET "
                        + "`earned`=`earned`+" + price + " WHERE nick=? AND parkour=?", Data.TABLE_RECORDS));
                stat.setString(1, player.getName());
                stat.setString(2, parkour.getName());
                stat.execute();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void updateSign(String player, double time, ParkourGame parkour) {
        Location l = parkour.getBestRecordLocation();
        if (l != null) {
            Sign s = (Sign) l.getBlock().getState();
            FileConfiguration c = ParkourPlugin.getInstance().getConfig();
            s.setLine(0, replace(c.getString("recordSign.line1"), player, time).replace('&', '�'));
            s.setLine(1, replace(c.getString("recordSign.line2"), player, time).replace('&', '�'));
            s.setLine(2, replace(c.getString("recordSign.line3"), player, time).replace('&', '�'));
            s.setLine(3, replace(c.getString("recordSign.line4"), player, time).replace('&', '�'));
            s.update();
        }
    }

    private static String replace(String s, String nick, double time) {
        s = s.replace("%NICK%", nick);
        int mins = (int) time / 60;
        s = s.replace("%MINUTES%", ("" + (mins < 10 ? "0" + mins : mins)).substring(0, 2));
        int secs = (int) time % 60;
        s = s.replace("%SECONDS%", "" + ("" + (secs < 10 ? "0" + secs : secs)).substring(0, 2));
        int milisecs = (int) Math.round((time % 1) * 100);
        s = s.replace("%MILISECONDS%", "" + ("" + (milisecs < 10 ? "0" + milisecs : milisecs)).substring(0, 2));
        return s;
    }
}
