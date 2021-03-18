/*
 * Copyright Andret (c) 2019. Copying and modifying allowed only keeping git link reference.
 */
package eu.andret.ats.parkour.parkour;

import eu.andret.ats.parkour.player.PlayerManager;
import eu.andret.ats.parkour.region.GameRegion;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = true)
public class ParkourGroup extends ParkourGame {
	private final List<Parkour> parkours = new ArrayList<>();

	public ParkourGroup(final String name, final GameRegion gameRegion, final World world) {
		super(name, gameRegion, world);
	}

	public void addParkour(final Parkour parkour) {
		parkours.add(parkour);
	}

	public boolean removeParkour(final Parkour parkour) {
		return parkours.remove(parkour);
	}

	@Override
	public void addPlayer(final Player player) {
		super.addPlayer(PlayerManager.getParkourCompetitorPlayer(player));
	}
}
