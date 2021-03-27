/*
 * Copyright Andret (c) 2019-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.parkour;

import eu.andret.ats.parkour.player.ParkourPlayer;
import eu.andret.ats.parkour.util.serializer.Serializer;
import lombok.Data;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class ParkourManager<E> {
	private final Serializer<E> serializer;
	private ParkourSetting setting = new ParkourSetting();

	@Data
	public static class ParkourSetting {

		private final List<ParkourGame> parkourGames = new ArrayList<>();
		private Location lobbyLocation;
	}

	public ParkourManager(final Serializer<E> serializer) {
		this.serializer = serializer;
	}

	public void addParkour(final ParkourGame parkourGame) {
		setting.parkourGames.add(parkourGame);
	}

	public void sortGames() {
		Collections.sort(setting.parkourGames);
	}

	public List<ParkourGame> getAllGames() {
		return new ArrayList<>(setting.parkourGames);
	}

	public ParkourGame getParkour(final Player player) {
		return setting.parkourGames.stream()
				.filter(parkour -> parkour.getPlayers().stream().map(ParkourPlayer::getPlayer).anyMatch(player::equals))
				.findAny()
				.orElse(null);
	}

	public List<Player> getPlayersInGames() {
		return setting.parkourGames.stream()
				.map(ParkourGame::getPlayers)
				.flatMap(Collection::stream)
				.map(ParkourPlayer::getPlayer)
				.collect(Collectors.toList());
	}

	public ParkourGame getParkour(final String name) {
		return setting.parkourGames.stream()
				.filter(game -> game.getName().equals(name))
				.findAny()
				.orElse(null);
	}

	public void removeParkour(final ParkourGame parkourGame) {
		setting.parkourGames.remove(parkourGame);
	}

	public void teleportToLobby(final ParkourPlayer player) {
		teleportToLobby(player.getPlayer());
	}

	public void teleportToLobby(final Player player) {
		player.teleport(setting.lobbyLocation);
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

	public Location getLobbyLocation() {
		return setting.getLobbyLocation();
	}

	public void setLobbyLocation(final Location location) {
		setting.setLobbyLocation(location);
	}

	public void deserialize(final E e) {
		setting = serializer.readParkourSetting(e);
	}

	public E serialize() {
		return serializer.writeParkourSetting(setting);
	}
}
