/*
 * Copyright Andret (c) 2019. Copying and modifying allowed only keeping git link reference.
 */
package eu.andret.parkour.parkour;

import eu.andret.parkour.player.PlayerManager;
import eu.andret.parkour.region.GameRegion;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class ParkourGroup extends ParkourGame {
	private final List<Parkour> parkours = new ArrayList<>();

	public ParkourGroup(String name, GameRegion gameregion, World world) {
		super(name, gameregion, world);
	}

	public void addParkour(Parkour e) {
		parkours.add(e);
	}

	public boolean removeParkour(Parkour e) {
		return parkours.remove(e);
	}

	@Override
	public void addPlayer(Player player) {
		super.addPlayer(PlayerManager.getParkourCompetitorPlayer(player));
	}
}
