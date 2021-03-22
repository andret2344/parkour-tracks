/*
 * Copyright Andret (c) 2019-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.parkour;

import eu.andret.ats.parkour.player.PlayerManager;
import eu.andret.ats.parkour.region.BasicRegion;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.bukkit.World;
import org.bukkit.entity.Player;

@Value
@EqualsAndHashCode(callSuper = true)
public class Parkour extends ParkourGame {
	public Parkour(final String name, final BasicRegion region, final World world) {
		super(name, region, world);
	}

	@Override
	public boolean addPlayer(final Player player) {
		return super.addPlayer(PlayerManager.getParkourSinglePlayer(player));
	}
}
