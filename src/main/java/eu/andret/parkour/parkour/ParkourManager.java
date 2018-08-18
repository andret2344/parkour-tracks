package eu.andret.parkour.parkour;

import eu.andret.parkour.player.ParkourPlayer;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ParkourManager {
    private static final List<ParkourGame> games = new ArrayList<>();
    private static Location lobby;

    private ParkourManager() {
    }

    public static void addParkour(ParkourGame parkour) {
        games.add(parkour);
    }

    public static void sortGames() {
        Collections.sort(games);
    }

    public static Location getLobbyLocation() {
        return lobby;
    }

    public static void setLobbyLocation(Location lobby) {
        ParkourManager.lobby = lobby;
    }

    public static List<ParkourGame> getAllGames() {
        return new ArrayList<>(games);
    }

    public static ParkourGame getParkour(Player player) {
        for (ParkourGame parkour : games) {
            for (ParkourPlayer parkourPlayer : parkour.getPlayers()) {
                if (parkourPlayer.getPlayer().equals(player)) {
                    return parkour;
                }
            }
        }
        return null;
    }

    public static List<Player> getPlayersInGames() {
        return games.stream().parallel().collect(ArrayList::new, (l, p) -> p.getPlayers().forEach(r -> l.add(r.getPlayer())), ArrayList::addAll);
    }

    public static ParkourGame getParkour(String name) {
        for (ParkourGame s : games) {
            if (s.getName().equals(name)) {
                return s;
            }
        }
        return null;
    }

    public static void removeParkour(ParkourGame parkour) {
        games.remove(parkour);
    }
}
