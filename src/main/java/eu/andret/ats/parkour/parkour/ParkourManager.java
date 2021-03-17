/*
 * Copyright Andret (c) 2019. Copying and modifying allowed only keeping git link reference.
 */
package eu.andret.ats.parkour.parkour;

import eu.andret.ats.parkour.player.ParkourPlayer;
import lombok.experimental.UtilityClass;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@UtilityClass
public final class ParkourManager {
	private final List<ParkourGame> games = new ArrayList<>();
	private Location lobby;

	public void addParkour(final ParkourGame parkour) {
		games.add(parkour);
	}

	public void sortGames() {
		Collections.sort(games);
	}

	public Location getLobbyLocation() {
		return lobby;
	}

	public void setLobbyLocation(final Location lobby) {
		ParkourManager.lobby = lobby;
	}

	public List<ParkourGame> getAllGames() {
		return new ArrayList<>(games);
	}

	public static ParkourGame getParkour(final Player player) {
		return games.stream()
				.filter(parkour -> parkour.getPlayers().stream().map(ParkourPlayer::getPlayer).anyMatch(player::equals))
				.findAny()
				.orElse(null);
	}

	public static List<Player> getPlayersInGames() {
		return games.stream().parallel().collect(ArrayList::new, (l, p) -> p.getPlayers().forEach(r -> l.add(r.getPlayer())), ArrayList::addAll);
	}

	public static ParkourGame getParkour(final String name) {
		return games.stream().filter(game -> game.getName().equals(name)).findAny().orElse(null);
	}

	public static void removeParkour(final ParkourGame parkour) {
		games.remove(parkour);
	}
}
