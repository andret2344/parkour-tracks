package eu.andret.parkour.player;

import com.sk89q.worldedit.Vector;
import eu.andret.parkour.event.PlayerTeleportBackEvent;
import eu.andret.parkour.parkour.ParkourGame;
import eu.andret.parkour.parkour.ParkourManager;
import eu.andret.parkour.region.Checkpoint;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

// import pl.mclobby.utils.ScoreboardSystem;
// import eu.andret.parkour.data.Data;

public abstract class ParkourPlayer {
    protected final Player player;
    protected int lastCheckpoint = 0;
    protected boolean ignoring = false, spectating = false;

    // private final ScoreboardSystem scoreboard = new ScoreboardSystem(15,
    // "pk");

    ParkourPlayer(Player player) {
        // scoreboard.setSidebarName(Data.scoreboardName);
        // scoreboard.showSidebar(true);
        // scoreboard.setScoreboard(player);
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    public void teleportToSpawn() {
        reset();
        teleportToCheckpoint(0);
    }

    public void teleportToCheckpoint(int id) {
        ParkourGame pk = ParkourManager.getParkour(player);
        Checkpoint cp = pk.getCheckpoint(id);
        Vector v = cp.getRegion().getCenter();
        player.teleport(new Location(pk.getWorld(), v.getX() + 0.5, v.getY(), v.getZ() + 0.5, cp.getYaw(), cp.getPitch()));
        Bukkit.getPluginManager().callEvent(new PlayerTeleportBackEvent(player, pk, id));
    }

    public int getLastVisitedCheckpoint() {
        return lastCheckpoint;
    }

    public void setLastVisitedChecpoint(int id) {
        lastCheckpoint = id;
    }

    public boolean isIgnoring() {
        return ignoring;
    }

    public boolean isSpectating() {
        return spectating;
    }

    public final void destroy() {
        PlayerManager.remove(player);
    }

    public void setIgnoring(boolean ignoring) {
        this.ignoring = ignoring;
    }

    public void setSpectating(boolean spectating) {
        this.spectating = spectating;
        if (spectating) {
            for (Player pl : Bukkit.getOnlinePlayers()) {
                pl.hidePlayer(player);
            }
        } else {
            for (Player pl : Bukkit.getOnlinePlayers()) {
                pl.showPlayer(player);
            }
        }
    }

    // public ScoreboardSystem getScoreboard() {
    // return scoreboard;
    // }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (!(o instanceof ParkourPlayer)) {
            return false;
        }
        if (o == this) {
            return true;
        }
        return player.equals(((ParkourPlayer) o).player);
    }

    @Override
    public int hashCode() {
        return player.hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{name=" + player.getName() + ", lastCheckpoint=" + lastCheckpoint + ", ignoring=" + ignoring + ", spectating=" + spectating + "}";
    }

    public boolean inAnyParkour() {
        for (ParkourGame parkour : ParkourManager.getAllGames()) {
            for (ParkourPlayer parkourPlayer : parkour.getPlayers()) {
                if (parkourPlayer.getPlayer().equals(player)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void reset() {
        lastCheckpoint = 0;
        player.setExp(0);
        player.setLevel(0);
    }

    public void teleportToLobby() {
        player.getInventory().setItem(7, new ItemStack(Material.AIR));
        player.teleport(ParkourManager.getLobbyLocation() == null ? Bukkit.getWorlds().get(0).getSpawnLocation() : ParkourManager.getLobbyLocation());
    }
}
