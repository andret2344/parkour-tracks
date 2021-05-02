/*
 * Copyright Andret (c) 2019-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.player;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.Vector3;
import eu.andret.ats.parkour.region.BasicRegion;
import eu.andret.ats.parkour.region.DirectionalRegion;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class PlayerManager {
	@NotNull
	private final Map<Player, ParkourSinglePlayer> players = new HashMap<>();

	@NotNull
	public ParkourPlayer getParkourPlayer(@NotNull final Player player) {
		if (players.containsKey(player)) {
			return players.get(player);
		}
		final ParkourSinglePlayer parkourPlayer = new ParkourSinglePlayer(player);
		players.put(player, parkourPlayer);
		return parkourPlayer;
	}

	public void remove(@NotNull final Player player) {
		players.remove(player);
	}

	public void teleportToRegion(@NotNull final ParkourPlayer parkourPlayer, @Nullable final DirectionalRegion basicRegion) {
		Optional.ofNullable(basicRegion)
				.map(BasicRegion::getRegion)
				.filter(region -> region.getWorld() != null)
				.ifPresent(region -> {
					final Vector3 center = region.getCenter();
					parkourPlayer.getPlayer().teleport(new Location(BukkitAdapter.adapt(region.getWorld()), center.getX() + 0.5, center.getY(), center.getZ() + 0.5, (float) basicRegion.getYaw(), (float) basicRegion.getPitch()));
				});
	}
}
