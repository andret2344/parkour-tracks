/*
 * Copyright Andret (c) 2019-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.parkour;

import eu.andret.ats.parkour.player.ParkourPlayer;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class ParkourManager {
	private final List<ParkourGame> parkourGames = new ArrayList<>();
	@Getter
	@Setter
	private Location lobbyLocation;

	public void addParkour(final ParkourGame parkourGame) {
		parkourGames.add(parkourGame);
	}

	public void sortGames() {
		Collections.sort(parkourGames);
	}

	public List<ParkourGame> getAllGames() {
		return new ArrayList<>(parkourGames);
	}

	public ParkourGame getParkour(final Player player) {
		return parkourGames.stream()
				.filter(parkour -> parkour.getPlayers().stream().map(ParkourPlayer::getPlayer).anyMatch(player::equals))
				.findAny()
				.orElse(null);
	}

	public List<Player> getPlayersInGames() {
		return parkourGames.stream()
				.map(ParkourGame::getPlayers)
				.flatMap(Collection::stream)
				.map(ParkourPlayer::getPlayer)
				.collect(Collectors.toList());
	}

	public ParkourGame getParkour(final String name) {
		return parkourGames.stream()
				.filter(game -> game.getName().equals(name))
				.findAny()
				.orElse(null);
	}

	public void removeParkour(final ParkourGame parkourGame) {
		parkourGames.remove(parkourGame);
	}

	public void teleportToLobby(final ParkourPlayer player) {
		teleportToLobby(player.getPlayer());
	}

	public void teleportToLobby(final Player player) {
		player.teleport(lobbyLocation);
	}

	public boolean inAnyRegion(final ParkourGame parkourGame, final ParkourPlayer parkourPlayer) {
		return inAnyRegion(parkourGame, parkourPlayer.getPlayer());
	}

	public boolean inAnyRegion(final ParkourGame parkourGame, final Player player) {
		return inAnyRegion(parkourGame, player.getLocation());
	}

	public boolean inAnyRegion(final ParkourGame parkourGame, final Location location) {
		return parkourGame.getAllRegions().stream()
				.filter(Objects::nonNull)
				.anyMatch(region -> region.contains(location));
	}
}
