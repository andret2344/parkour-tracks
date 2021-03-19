/*
 * Copyright Andret (c) 2019. Copying and modifying allowed only keeping git link reference.
 */
package eu.andret.ats.parkour.parkour;

import eu.andret.ats.parkour.player.ParkourPlayer;
import lombok.experimental.UtilityClass;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@UtilityClass
public final class ParkourManager {
	private final List<ParkourGame> PARKOUR_GAMES = new ArrayList<>();
	private Location lobby;

	public void addParkour(final ParkourGame parkourGame) {
		PARKOUR_GAMES.add(parkourGame);
	}

	public void sortGames() {
		Collections.sort(PARKOUR_GAMES);
	}

	public Location getLobbyLocation() {
		return lobby;
	}

	public void setLobbyLocation(final Location lobby) {
		ParkourManager.lobby = lobby;
	}

	public List<ParkourGame> getAllGames() {
		return new ArrayList<>(PARKOUR_GAMES);
	}

	public static ParkourGame getParkour(final Player player) {
		return PARKOUR_GAMES.stream()
				.filter(parkour -> parkour.getPlayers().stream().map(ParkourPlayer::getPlayer).anyMatch(player::equals))
				.findAny()
				.orElse(null);
	}

	public static List<Player> getPlayersInGames() {
		return PARKOUR_GAMES.stream()
				.map(ParkourGame::getPlayers)
				.flatMap(Collection::stream)
				.map(ParkourPlayer::getPlayer)
				.collect(Collectors.toList());
	}

	public static ParkourGame getParkour(final String name) {
		return PARKOUR_GAMES.stream()
				.filter(game -> game.getName().equals(name))
				.findAny()
				.orElse(null);
	}

	public static void removeParkour(final ParkourGame parkourGame) {
		PARKOUR_GAMES.remove(parkourGame);
	}
}
