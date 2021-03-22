/*
 * Copyright Andret (c) 2019-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.player;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.Vector3;
import eu.andret.ats.parkour.region.BasicRegion;
import eu.andret.ats.parkour.region.DirectionalRegion;
import lombok.experimental.UtilityClass;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@UtilityClass
public final class PlayerManager {
	private final Map<Player, ParkourSinglePlayer> singlePlayers = new HashMap<>();
	private final Map<Player, ParkourCompetitorPlayer> competitorPlayers = new HashMap<>();

	public ParkourSinglePlayer getParkourSinglePlayer(final Player player) {
		if (singlePlayers.containsKey(player)) {
			return singlePlayers.get(player);
		}
		final ParkourSinglePlayer p = new ParkourSinglePlayer(player);
		singlePlayers.put(player, p);
		return p;
	}

	public ParkourCompetitorPlayer getParkourCompetitorPlayer(final Player player) {
		if (competitorPlayers.containsKey(player)) {
			return competitorPlayers.get(player);
		}
		final ParkourCompetitorPlayer p = new ParkourCompetitorPlayer(player);
		competitorPlayers.put(player, p);
		return p;
	}

	public ParkourPlayer getParkourPlayer(final Player player) {
		if (singlePlayers.containsKey(player)) {
			return singlePlayers.get(player);
		}
		if (competitorPlayers.containsKey(player)) {
			return competitorPlayers.get(player);
		}
		return null;
	}

	public ParkourPlayer remove(final Player player) {
		final ParkourPlayer parkourPlayer = singlePlayers.remove(player);
		if (parkourPlayer != null) {
			return parkourPlayer;
		}
		return competitorPlayers.remove(player);
	}

	public void teleportToRegion(final ParkourPlayer parkourPlayer, final DirectionalRegion basicRegion) {
		Optional.ofNullable(basicRegion)
				.map(BasicRegion::getRegion)
				.filter(x -> x.getWorld() != null)
				.ifPresent(x -> {
					final Vector3 vector = x.getCenter();
					parkourPlayer.getPlayer().teleport(new Location(BukkitAdapter.adapt(x.getWorld()), vector.getX() + 0.5, vector.getY(), vector.getZ() + 0.5, (float) basicRegion.getYaw(), (float) basicRegion.getPitch()));
				});
	}
}
