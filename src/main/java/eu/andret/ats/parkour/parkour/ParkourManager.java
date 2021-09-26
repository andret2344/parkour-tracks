/*
 * Copyright Andret (c) 2018-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.parkour;

import eu.andret.ats.parkour.ParkourPlugin;
import eu.andret.ats.parkour.player.ParkourPlayer;
import eu.andret.ats.parkour.region.BasicRegion;
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

@Data
public final class ParkourManager {
	@NotNull
	private ParkourSetting setting = new ParkourSetting();

	@Data
	public static class ParkourSetting {
		@NotNull
		private final List<ParkourGame> games = new ArrayList<>();
		@Nullable
		private Location lobby;

		@NotNull
		private ParkourGame add(@NotNull final ParkourGame game) {
			games.add(game);
			return game;
		}
	}

	@NotNull
	public ParkourGame createParkour(@NotNull final String name, @NotNull final BasicRegion region, @NotNull final World world) {
		return Optional.of(world)
				.map(w -> new Parkour(-1, name, region, w))
				.map(setting::add)
				.orElseThrow(() -> new IllegalArgumentException("World cannot be null!"));
	}

	@NotNull
	public List<ParkourGame> getAllGames() {
		return new ArrayList<>(setting.games);
	}

	@Nullable
	public ParkourGame getParkour(@NotNull final Player player) {
		return setting.games.stream()
				.filter(parkour -> parkour.getPlayers().stream()
						.map(ParkourPlayer::getPlayer)
						.anyMatch(player::equals))
				.findAny()
				.orElse(null);
	}

	@NotNull
	public List<Player> getPlayersInGames() {
		return setting.games.stream()
				.map(ParkourGame::getPlayers)
				.flatMap(Collection::stream)
				.map(ParkourPlayer::getPlayer)
				.toList();
	}

	@Nullable
	public ParkourGame getParkour(@NotNull final String name) {
		return setting.games.stream()
				.filter(game -> game.getName().equals(name))
				.findAny()
				.orElse(null);
	}

	public void removeParkour(@NotNull final ParkourGame parkourGame) {
		setting.games.remove(parkourGame);
	}

	public void teleportToLobby(@NotNull final ParkourPlayer player) {
		teleportToLobby(player.getPlayer());
	}

	public void teleportToLobby(@NotNull final Player player) {
		Optional.of(setting)
				.map(ParkourSetting::getLobby)
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
	public Location getLobby() {
		return setting.getLobby();
	}

	public void setLobby(@NotNull final Location location) {
		setting.setLobby(location);
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
