/*
 * Copyright Andret (c) 2019. Copying and modifying allowed only keeping git link reference.
 */
package eu.andret.ats.parkour.parkour;

import eu.andret.ats.parkour.player.PlayerManager;
import eu.andret.ats.parkour.region.GameRegion;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.bukkit.World;
import org.bukkit.entity.Player;

@Value
@EqualsAndHashCode(callSuper = true)
public class Parkour extends ParkourGame {

	public Parkour(String name, GameRegion gameRegion, World world) {
		super(name, gameRegion, world);
	}

	@Override
	public void addPlayer(Player player) {
		super.addPlayer(PlayerManager.getParkourSinglePlayer(player));
	}
}
