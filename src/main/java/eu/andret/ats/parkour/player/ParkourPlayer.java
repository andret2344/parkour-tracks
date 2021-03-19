/*
 * Copyright Andret (c) 2019. Copying and modifying allowed only keeping git link reference.
 */
package eu.andret.ats.parkour.player;

import com.sk89q.worldedit.math.Vector3;
import eu.andret.ats.parkour.event.player.PlayerTeleportBackEvent;
import eu.andret.ats.parkour.parkour.ParkourGame;
import eu.andret.ats.parkour.parkour.ParkourManager;
import eu.andret.ats.parkour.region.DirectionalRegion;
import lombok.Data;
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

	ParkourPlayer(final Player player) {
		this.player = player;
	}

	public boolean teleportToSpawn() {
		return teleportToCheckpoint(0);
	}

	public boolean teleportToCheckpoint(final int id) {
		// TODO: WHY?
		if (inAnyParkour()) {
			final ParkourGame parkourGame = ParkourManager.getParkour(player);
			final DirectionalRegion checkpointRegion = parkourGame.getCheckpoints().get(id);
			if (checkpointRegion == null) {
				return false;
			}
			final Vector3 vector = checkpointRegion.getCuboidRegion().getCenter();
			player.teleport(new Location(parkourGame.getWorld(), vector.getX() + 0.5, vector.getY(), vector.getZ() + 0.5, (float) checkpointRegion.getYaw(), (float) checkpointRegion.getPitch()));
			Bukkit.getPluginManager().callEvent(new PlayerTeleportBackEvent(parkourGame, this, checkpointRegion));
		}
		return true;
	}

	public void setSpectating(final boolean spectating) {
		this.spectating = spectating;
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
