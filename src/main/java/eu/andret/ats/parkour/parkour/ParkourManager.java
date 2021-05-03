/*
 * Copyright Andret (c) 2019-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.parkour;

import eu.andret.ats.parkour.ParkourPlugin;
import eu.andret.ats.parkour.player.ParkourPlayer;
import eu.andret.ats.parkour.region.BasicRegion;
import eu.andret.ats.parkour.util.serializer.Serializer;
import lombok.Data;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public final class ParkourManager<E> {
	@NotNull
	private final Serializer<E> serializer;
	@NotNull
	private ParkourSetting setting = new ParkourSetting();

	@Data
	public static class ParkourSetting {
		@NotNull
		private final List<ParkourGame> parkourGames = new ArrayList<>();
		@Nullable
		private Location lobbyLocation;
	}

	public ParkourManager(@NotNull final Serializer<E> serializer) {
		this.serializer = serializer;
	}

	@NotNull
	public ParkourGame createParkour(@NotNull final String name, @NotNull final BasicRegion region, @Nullable final World world) {
		if (world == null) {
			throw new IllegalArgumentException("World cannot be null!");
		}
		final ParkourGame game = new Parkour(name, region, world);
		setting.parkourGames.add(game);
		return game;
	}

	@NotNull
	public List<ParkourGame> getAllGames() {
		return new ArrayList<>(setting.parkourGames);
	}

	@Nullable
	public ParkourGame getParkour(@NotNull final Player player) {
		return setting.parkourGames.stream()
				.filter(parkour -> parkour.getPlayers().stream().map(ParkourPlayer::getPlayer).anyMatch(player::equals))
				.findAny()
				.orElse(null);
	}

	@NotNull
	public List<Player> getPlayersInGames() {
		return setting.parkourGames.stream()
				.map(ParkourGame::getPlayers)
				.flatMap(Collection::stream)
				.map(ParkourPlayer::getPlayer)
				.collect(Collectors.toList());
	}

	@Nullable
	public ParkourGame getParkour(@NotNull final String name) {
		return setting.parkourGames.stream()
				.filter(game -> game.getName().equals(name))
				.findAny()
				.orElse(null);
	}

	public void removeParkour(@NotNull final ParkourGame parkourGame) {
		setting.parkourGames.remove(parkourGame);
	}

	public void teleportToLobby(@NotNull final ParkourPlayer player) {
		teleportToLobby(player.getPlayer());
	}

	public void teleportToLobby(@NotNull final Player player) {
		Optional.of(setting)
				.map(ParkourSetting::getLobbyLocation)
				.ifPresent(player::teleport);
	}

	public boolean inAnyRegion(@NotNull final ParkourGame parkourGame, @Nullable final ParkourPlayer parkourPlayer) {
		return Optional.ofNullable(parkourPlayer)
				.map(ParkourPlayer::getPlayer)
				.filter(player -> inAnyRegion(parkourGame, player))
				.isPresent();
	}

	public boolean inAnyRegion(@NotNull final ParkourGame parkourGame, @Nullable final Player player) {
		return Optional.ofNullable(player)
				.map(Player::getLocation)
				.filter(location -> inAnyRegion(parkourGame, location))
				.isPresent();
	}

	public boolean inAnyRegion(@NotNull final ParkourGame parkourGame, @Nullable final Location location) {
		return parkourGame.getAllRegions().stream()
				.filter(Objects::nonNull)
				.anyMatch(region -> region.contains(location));
	}

	@Nullable
	public Location getLobbyLocation() {
		return setting.getLobbyLocation();
	}

	public void setLobbyLocation(@NotNull final Location location) {
		setting.setLobbyLocation(location);
	}

	public void deserialize(@NotNull final E e) {
		setting = serializer.readParkourSetting(e);
	}

	@NotNull
	public E serialize() {
		return serializer.writeParkourSetting(setting);
	}

	public void setHidden(@NotNull final Player player, @NotNull final ParkourPlugin plugin, final boolean shouldHide) {
		getAllGames()
				.stream()
				.map(ParkourGame::getPlayers)
				.flatMap(Collection::stream)
				.map(ParkourPlayer::getPlayer)
				.forEach(hidingPlayer -> {
					if (shouldHide) {
						player.showPlayer(plugin, hidingPlayer);
					} else {
						player.hidePlayer(plugin, hidingPlayer);
					}
				});
	}
}
