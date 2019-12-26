/*
 * Copyright Andret (c) 2019. Copying and modifying allowed only keeping git link reference.
 */
package eu.andret.parkour.player;

import com.sk89q.worldedit.math.Vector3;
import eu.andret.parkour.event.player.PlayerTeleportBackEvent;
import eu.andret.parkour.parkour.ParkourGame;
import eu.andret.parkour.parkour.ParkourManager;
import eu.andret.parkour.region.Checkpoint;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@Data
public abstract class ParkourPlayer {
	protected final Player player;
	protected int lastVisitedCheckpointId = 0;
	protected boolean ignoring = false;
	protected boolean spectating = false;

	ParkourPlayer(Player player) {
		this.player = player;
	}

	public void teleportToSpawn() {
		reset();
		teleportToCheckpoint(0);
	}

	public void teleportToCheckpoint(int id) {
		if (inAnyParkour()) {
			ParkourGame pk = ParkourManager.getParkour(player);
			Checkpoint cp = pk.getCheckpoint(id);
			Vector3 v = cp.getRegion().getCenter();
			player.teleport(new Location(pk.getWorld(), v.getX() + 0.5, v.getY(), v.getZ() + 0.5, cp.getYaw(), cp.getPitch()));
			Bukkit.getPluginManager().callEvent(new PlayerTeleportBackEvent(pk, this, cp));
		}
	}

	public final void destroy() {
		PlayerManager.remove(player);
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

	public boolean inAnyParkour() {
		return ParkourManager.getParkour(player) != null;
	}

	public void reset() {
		lastVisitedCheckpointId = 0;
		player.setExp(0);
		player.setLevel(0);
	}

	public void teleportToLobby() {
		player.getInventory().setItem(7, new ItemStack(Material.AIR));
		player.teleport(ParkourManager.getLobbyLocation() == null ? Bukkit.getWorlds().get(0).getSpawnLocation() : ParkourManager.getLobbyLocation());
	}
}
