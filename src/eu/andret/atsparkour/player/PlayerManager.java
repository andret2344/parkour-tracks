package eu.andret.atsparkour.player;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public final class PlayerManager {
    private static final Map<Player, ParkourSinglePlayer> singlePlayers = new HashMap<>();
    private static final Map<Player, ParkourCompetitorPlayer> competitorPlayers = new HashMap<>();

    public static ParkourSinglePlayer getParkourSinglePlayer(Player player) {
        if (singlePlayers.containsKey(player)) {
            return singlePlayers.get(player);
        }
        ParkourSinglePlayer p = new ParkourSinglePlayer(player);
        singlePlayers.put(player, p);
        return p;
    }

    public static ParkourCompetitorPlayer getParkourCompetitorPlayer(Player player) {
        System.out.print("dodal competitora");
        if (competitorPlayers.containsKey(player)) {
            return competitorPlayers.get(player);
        }
        ParkourCompetitorPlayer p = new ParkourCompetitorPlayer(player);
        competitorPlayers.put(player, p);
        return p;
    }

    public static ParkourPlayer remove(Player player) {
        ParkourPlayer pp = singlePlayers.remove(player);
        if (pp != null) {
            return pp;
        }
        return competitorPlayers.remove(player);
    }

    public static boolean playerExists(Player player) {
        return singlePlayers.containsKey(player) || competitorPlayers.containsKey(player);
    }
}
