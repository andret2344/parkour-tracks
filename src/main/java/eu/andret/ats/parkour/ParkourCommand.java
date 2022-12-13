/*
 * Copyright Andret (c) 2018-2022. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import eu.andret.arguments.AnnotatedCommandExecutor;
import eu.andret.arguments.api.annotation.Argument;
import eu.andret.arguments.api.annotation.BaseCommand;
import eu.andret.arguments.api.annotation.TypeFallback;
import eu.andret.arguments.api.entity.ExecutorType;
import eu.andret.ats.parkour.entity.MedalRequirement;
import eu.andret.ats.parkour.entity.MedalSetupOption;
import eu.andret.ats.parkour.entity.SimpleLever;
import eu.andret.ats.parkour.event.game.GameStartEvent;
import eu.andret.ats.parkour.event.game.GameStopEvent;
import eu.andret.ats.parkour.event.player.PlayerQuitGameEvent;
import eu.andret.ats.parkour.parkour.Effect;
import eu.andret.ats.parkour.parkour.Medal;
import eu.andret.ats.parkour.parkour.ParkourGame;
import eu.andret.ats.parkour.parkour.ParkourMedal;
import eu.andret.ats.parkour.player.ParkourPlayer;
import eu.andret.ats.parkour.region.BasicRegion;
import eu.andret.ats.parkour.region.Checkpoint;
import eu.andret.ats.parkour.region.Wall;
import eu.andret.ats.parkour.tasks.database.FetchParkourBestScoreTask;
import eu.andret.ats.parkour.tutorial.TutorialManager;
import eu.andret.ats.parkour.tutorial.TutorialPlayer;
import eu.andret.ats.parkour.util.Constants;
import eu.andret.ats.parkour.util.Data;
import eu.andret.ats.parkour.util.M;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.DoubleFunction;
import java.util.function.ToDoubleFunction;

@SuppressWarnings("DuplicatedCode")
@BaseCommand("parkour")
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public final class ParkourCommand extends AnnotatedCommandExecutor<ParkourPlugin> {
	private static final double EPSILON = 0.001;

	public ParkourCommand(@NotNull final CommandSender sender, @NotNull final ParkourPlugin plugin) {
		super(sender, plugin);
	}

	// === TUTORIAL ===
	@Nullable
	@Argument(permission = "ats.parkour.tutorial", executorType = ExecutorType.PLAYER, description = "Runs tutorial")
	public String tutorial(@NotNull final SimpleLever lever) {
		final Player player = (Player) sender;
		final TutorialManager tutorialManager = plugin.getTutorialManager();
		switch (lever) {
			case ON -> {
				if (tutorialManager.hasPlayer(player)) {
					return "&dYou are already in the tutorial... Be polite!";
				}
				tutorialManager.getPlayer(player).sendMessage();
				return null;
			}
			case OFF -> {
				if (!tutorialManager.hasPlayer(player)) {
					return "&dYou are not in the tutorial... Be polite!";
				}
				tutorialManager.removePlayer(player);
				return "&dOh, that's sad you don't want to learn anymore, but I appreciate your knowledge. Bye!";
			}
			default -> {
				return "&dWhat to do with tutorial? Set it to true or false? Please, specify";
			}
		}
	}

	// === GLOBAL ===
	@NotNull
	@Argument(permission = "ats.parkour.lobby", executorType = ExecutorType.PLAYER, description = "Teleports to lobby")
	public String lobby() {
		final Player player = (Player) sender;
		final Location location = plugin.getParkourManager().getLobby();
		if (location == null) {
			executeTutorial(player, 0);
			return plugin.msg(M.Error.DEFAULT.noLobby);
		}
		plugin.getParkourManager().teleportToLobby(player);
		if (plugin.getTutorialManager().hasPlayer(player)) {
			final TutorialPlayer tutorialPlayer = plugin.getTutorialManager().getPlayer(player);
			if (tutorialPlayer.done(0)) {
				tutorialPlayer.next().next().sendMessage().next().next().sendMessage();
			}
		}
		return plugin.msg(M.General.LOBBY.success)
				.replace(Constants.COORD_X, plugin.formatCoord(location.getX()))
				.replace(Constants.COORD_Y, plugin.formatCoord(location.getY()))
				.replace(Constants.COORD_Z, plugin.formatCoord(location.getZ()))
				.replace(Constants.COORD_YAW, plugin.formatCoord(location.getYaw()))
				.replace(Constants.COORD_PITCH, plugin.formatCoord(location.getPitch()));
	}

	@NotNull
	@Argument(permission = "ats.parkour.setLobby", executorType = ExecutorType.PLAYER, description = "Sets lobby location to player's location")
	public String setLobby() {
		final Player player = (Player) sender;
		final Location location = player.getLocation();
		plugin.getParkourManager().setLobby(location);
		if (plugin.getTutorialManager().hasPlayer(player)) {
			final TutorialPlayer tutorialPlayer = plugin.getTutorialManager().getPlayer(player);
			if (tutorialPlayer.done(1)) {
				tutorialPlayer.next().next().sendMessage().next().sendMessage();
			}
		}
		return plugin.msg(M.General.SET_LOBBY.success)
				.replace(Constants.COORD_X, plugin.formatCoord(location.getX()))
				.replace(Constants.COORD_Y, plugin.formatCoord(location.getY()))
				.replace(Constants.COORD_Z, plugin.formatCoord(location.getZ()))
				.replace(Constants.COORD_YAW, plugin.formatCoord(location.getYaw()))
				.replace(Constants.COORD_PITCH, plugin.formatCoord(location.getPitch()));
	}

	@NotNull
	@Argument(permission = "ats.parkour.list", description = "Lists all parkour games", aliases = "ls")
	public List<String> list() {
		final List<ParkourGame> allGames = plugin.getParkourManager().getAllGames();
		if (allGames.isEmpty()) {
			return List.of(plugin.msg(M.List.GAMES.empty));
		}
		sender.sendMessage(plugin.msg(M.List.GAMES.header)
				.replace(Constants.COUNT, String.valueOf(allGames.size())));
		if (sender instanceof final Player player) {
			executeTutorial(player, 11);
		}
		return allGames.stream()
				.map(parkourGame -> plugin.msg(M.List.GAMES.item)
						.replace(Constants.NAME, parkourGame.getName())
						.replace(Constants.DISPLAY_NAME, parkourGame.getDisplayName())
						.replace(Constants.RUNNING, plugin.misc(parkourGame.isRunning() ? "suffix-started" : "suffix-stopped")))
				.map(text -> ChatColor.translateAlternateColorCodes('&', text))
				.toList();
	}

	@Argument(permission = "ats.parkour.fix", description = "Fixes signs after database connection troubles.")
	public void fix() {
		plugin.getParkourManager().getAllGames()
				.stream()
				.map(parkourGame -> new FetchParkourBestScoreTask(plugin, parkourGame, 1, data -> data.stream().findFirst()
						.ifPresentOrElse(plugin::updateSyncSign, () -> plugin.updateSyncSign(parkourGame))))
				.forEach(fetchParkourBestScoreTask -> plugin.getServer().getScheduler().runTaskAsynchronously(plugin, fetchParkourBestScoreTask));
		sender.sendMessage(plugin.msg(M.General.FIX.success));
	}

	@NotNull
	@Argument(permission = "ats.parkour.ignore", executorType = ExecutorType.PLAYER, description = "Allows sender to ignore parkour regions interaction", aliases = "i")
	public String ignore() {
		final Player player = (Player) sender;
		final ParkourPlayer parkourPlayer = plugin.getPlayerManager().getParkourPlayer(player);
		if (parkourPlayer.isIgnoring()) {
			plugin.getParkourManager().getAllGames().stream()
					.filter(parkourGame -> plugin.getParkourManager().inAnyRegion(parkourGame, player))
					.forEach(parkourGame -> plugin.getParkourManager().teleportToLobby(player));
			parkourPlayer.setIgnoring(false);
			return plugin.msg(M.General.IGNORE.success)
					.replace(Constants.VALUE, Boolean.FALSE.toString());
		}
		final ParkourGame parkour = plugin.getParkourManager().getParkour(player);
		if (parkour != null) {
			plugin.getServer().getPluginManager().callEvent(new PlayerQuitGameEvent(parkour, parkourPlayer));
			parkour.removePlayer(parkourPlayer);
		}
		parkourPlayer.setIgnoring(true);
		return plugin.msg(M.General.IGNORE.success)
				.replace(Constants.VALUE, Boolean.TRUE.toString());
	}

	@Argument(permission = "ats.parkour.help", description = "Shows help page", aliases = "?")
	public void help() {
		help(1);
	}

	@NotNull
	@Argument(permission = "ats.parkour.help", description = "Shows help page", aliases = "?")
	public List<String> help(final int page) {
		final Map<String, String> messages = plugin.getHelpDescription();
		final int maxPages = (int) Math.ceil(messages.size() / 5.);
		final int skip = 5 * (page - 1);
		if (page > maxPages) {
			return Collections.emptyList();
		}
		sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.msg(M.List.HELP.header)
				.replace(Constants.PAGE, String.valueOf(page))
				.replace(Constants.PAGES, String.valueOf(maxPages))));
		return messages.entrySet()
				.stream()
				.skip(skip)
				.limit(5)
				.map(command -> plugin.msg(M.List.HELP.item)
						.replace(Constants.ARGUMENT, command.getKey())
						.replace(Constants.DESCRIPTION, command.getValue()))
				.map(text -> ChatColor.translateAlternateColorCodes('&', text))
				.toList();
	}

	// === GENERAL ===
	@NotNull
	@Argument(permission = "ats.parkour.create", executorType = ExecutorType.PLAYER, description = "Creates parkour game")
	public String create(@NotNull final String name) {
		if (plugin.getParkourManager().getLobby() == null) {
			return plugin.msg(M.Error.DEFAULT.missingLobby);
		}
		final Player player = (Player) sender;
		final CuboidRegion region = getSelectionRegion(player);
		if (region == null) {
			return plugin.msg(M.Error.DEFAULT.invalidSelection);
		}
		if (!name.matches("[a-zA-Z\\d_-]+")) {
			return plugin.msg(M.Error.DEFAULT.invalidName);
		}
		final ParkourGame parkour = plugin.getParkourManager().getParkour(name);
		if (parkour != null) {
			return plugin.msg(M.Error.DEFAULT.alreadyExists).replace(Constants.NAME, name);
		}
		executeTutorial(player, 4);
		final World world = player.getLocation().getWorld();
		if (world == null) {
			return plugin.msg(M.Error.DEFAULT.invalidGame);
		}
		final ParkourGame parkourGame = plugin.getParkourManager().createParkour(name, new BasicRegion(region), world);
		return plugin.msg(M.Executive.CREATE.success)
				.replace(Constants.NAME, parkourGame.getName());
	}

	@NotNull
	@Argument(permission = "ats.parkour.remove", description = "Removes parkour game")
	public String remove(@NotNull final ParkourGame parkourGame) {
		if (parkourGame.isRunning() && plugin.isEditLockActive()) {
			return plugin.msg(M.Error.DEFAULT.forbiddenModification);
		}
		plugin.getParkourManager().removeParkour(parkourGame);
		return plugin.msg(M.Executive.REMOVE.success)
				.replace(Constants.NAME, parkourGame.getName())
				.replace(Constants.DISPLAY_NAME, parkourGame.getDisplayName());
	}

	@NotNull
	@Argument(permission = "ats.parkour.start", description = "Starts parkour game")
	public String start(@NotNull final ParkourGame parkourGame) {
		if (parkourGame.isRunning()) {
			return plugin.msg(M.Error.DEFAULT.alreadyStarted);
		}
		if (parkourGame.getCheckpoints().size() < 2) {
			return plugin.msg(M.Error.DEFAULT.missingCheckpoint2);
		}
		if (!verifyMedals(parkourGame)) {
			return plugin.msg(M.Error.DEFAULT.missingMedals);
		}
		if (sender instanceof Player player && executeTutorial(player, 12)) {
			plugin.getTutorialManager().removePlayer(player);
		}
		parkourGame.setRunning(true);
		plugin.getServer().getPluginManager().callEvent(new GameStartEvent(parkourGame));
		plugin.hideCheckpoints(parkourGame);
		return plugin.msg(M.Executive.START.success)
				.replace(Constants.NAME, parkourGame.getName())
				.replace(Constants.DISPLAY_NAME, parkourGame.getDisplayName());
	}

	@NotNull
	@Argument(permission = "ats.parkour.stop", description = "Stops parkour game")
	public String stop(@NotNull final ParkourGame parkourGame) {
		if (!parkourGame.isRunning()) {
			return plugin.msg(M.Error.DEFAULT.alreadyStopped);
		}
		parkourGame.setRunning(false);
		plugin.getServer().getPluginManager().callEvent(new GameStopEvent(parkourGame));
		plugin.showCheckpoints(parkourGame);
		return plugin.msg(M.Executive.STOP.success)
				.replace(Constants.NAME, parkourGame.getName())
				.replace(Constants.DISPLAY_NAME, parkourGame.getDisplayName());
	}

	@NotNull
	@Argument(permission = "ats.parkour.recreate", executorType = ExecutorType.PLAYER, description = "Sets new region for parkour game")
	public String recreate(@NotNull final ParkourGame parkourGame) {
		if (parkourGame.isRunning() && plugin.isEditLockActive()) {
			return plugin.msg(M.Error.DEFAULT.forbiddenModification);
		}
		final CuboidRegion region = getSelectionRegion((Player) sender);
		if (region == null) {
			return plugin.msg(M.Error.DEFAULT.invalidSelection);
		}
		parkourGame.setRegion(new BasicRegion(region));
		return plugin.msg(M.Executive.RECREATE.success);
	}

	@NotNull
	@Argument(permission = "ats.parkour.rename", description = "Sets new name for parkour game")
	public String rename(@NotNull final ParkourGame parkourGame, @NotNull final String name) {
		if (parkourGame.isRunning() && plugin.isEditLockActive()) {
			return plugin.msg(M.Error.DEFAULT.forbiddenModification);
		}
		if (plugin.getParkourManager().getParkour(name) != null) {
			return plugin.msg(M.Error.DEFAULT.alreadyExists)
					.replace(Constants.NAME, name);
		}
		parkourGame.setName(name);
		return plugin.msg(M.Executive.RENAME.success)
				.replace(Constants.OLD_NAME, parkourGame.getName())
				.replace(Constants.NEW_NAME, name);
	}

	@NotNull
	@Argument(permission = "ats.parkour.teleport", executorType = ExecutorType.PLAYER, description = "Teleports sender to parkour's spawn region", aliases = "tp")
	public String teleport(@NotNull final ParkourGame parkourGame) {
		final ParkourPlayer parkourPlayer = plugin.getPlayerManager().getParkourPlayer((Player) sender);
		parkourGame.addPlayer(parkourPlayer);
		parkourPlayer.reset();
		if (parkourGame.getCheckpoints().isEmpty()) {
			return plugin.msg(M.Error.DEFAULT.missingCheckpoint1);
		}
		plugin.getPlayerManager().teleportToCheckpoint(parkourPlayer, parkourGame.getCheckpoints().get(0));
		return plugin.msg(M.Executive.TELEPORT.success)
				.replace(Constants.NAME, parkourGame.getName())
				.replace(Constants.DISPLAY_NAME, parkourGame.getDisplayName());
	}

	// === REGIONS ===
	@NotNull
	@Argument(permission = "ats.parkour.addCheckpoint", description = "Adds checkpoint to parkour game", executorType = ExecutorType.PLAYER)
	public String addCheckpoint(@NotNull final ParkourGame parkourGame) {
		if (parkourGame.isRunning() && plugin.isEditLockActive()) {
			return plugin.msg(M.Error.DEFAULT.forbiddenModification);
		}
		final Player player = (Player) sender;
		final CuboidRegion region = getSelectionRegion(player);
		if (region == null) {
			return plugin.msg(M.Error.DEFAULT.invalidSelection);
		}
		if (parkourGame.getCheckpoints().isEmpty()) {
			executeTutorial(player, 5);
		} else {
			executeTutorial(player, 6, false);
		}
		final Location location = player.getLocation();
		parkourGame.getCheckpoints().add(new Checkpoint(region, location));
		final String sizeString = String.valueOf(parkourGame.getCheckpoints().size());
		plugin.createArmorStand(parkourGame, location, sizeString);
		return plugin.msg(M.Region.Checkpoint.ADD.success)
				.replace(Constants.INDEX, sizeString);
	}

	@NotNull
	@Argument(permission = "ats.parkour.setCheckpoint", description = "Sets specified parkour game checkpoint", executorType = ExecutorType.PLAYER)
	public String setCheckpoint(@NotNull final ParkourGame parkourGame, final int index) {
		if (parkourGame.isRunning() && plugin.isEditLockActive()) {
			return plugin.msg(M.Error.DEFAULT.forbiddenModification);
		}
		final Player player = (Player) sender;
		final CuboidRegion region = getSelectionRegion(player);
		if (region == null) {
			return plugin.msg(M.Error.DEFAULT.invalidSelection);
		}
		if (index <= 0) {
			return plugin.msg(M.Error.DEFAULT.negativeNumber);
		}
		if (index > parkourGame.getCheckpoints().size()) {
			return plugin.msg(M.Error.DEFAULT.tooLargeNumber);
		}
		final Location location = player.getLocation();
		parkourGame.getCheckpoints().set(index, new Checkpoint(region, location));
		final String stringIndex = String.valueOf(index);
		plugin.moveArmorStand(parkourGame, location, stringIndex);
		executeTutorial(player, 6, false);
		return plugin.msg(M.Region.Checkpoint.SET.success)
				.replace(Constants.INDEX, stringIndex);
	}

	@NotNull
	@Argument(permission = "ats.parkour.addWall", description = "Adds wall to parkour game", executorType = ExecutorType.PLAYER)
	public String addWall(@NotNull final ParkourGame parkourGame) {
		if (parkourGame.isRunning() && plugin.isEditLockActive()) {
			return plugin.msg(M.Error.DEFAULT.forbiddenModification);
		}
		final Player player = (Player) sender;
		final CuboidRegion region = getSelectionRegion(player);
		if (region == null) {
			return plugin.msg(M.Error.DEFAULT.invalidSelection);
		}
		parkourGame.getWalls().add(new Wall(region));
		executeTutorial(player, 9, false);
		return plugin.msg(M.Region.Wall.ADD.success)
				.replace(Constants.INDEX, String.valueOf(parkourGame.getWalls().size()));
	}

	@NotNull
	@Argument(permission = "ats.parkour.setWall", description = "Sets specified parkour game wall", executorType = ExecutorType.PLAYER)
	public String setWall(@NotNull final ParkourGame parkourGame, final int index) {
		if (parkourGame.isRunning() && plugin.isEditLockActive()) {
			return plugin.msg(M.Error.DEFAULT.forbiddenModification);
		}
		final Player player = (Player) sender;
		final CuboidRegion region = getSelectionRegion(player);
		if (region == null) {
			return plugin.msg(M.Error.DEFAULT.invalidSelection);
		}
		if (index <= 0) {
			return plugin.msg(M.Error.DEFAULT.negativeNumber);
		}
		if (index > parkourGame.getWalls().size()) {
			return plugin.msg(M.Error.DEFAULT.tooLargeNumber);
		}
		parkourGame.getWalls().set(index, new Wall(region));
		executeTutorial(player, 9, false);
		return plugin.msg(M.Region.Wall.SET.success)
				.replace(Constants.INDEX, String.valueOf(index));
	}

	@NotNull
	@Argument(permission = "ats.parkour.effects", description = "Shows current parkour effects")
	public List<String> effects(@NotNull final ParkourGame parkourGame) {
		if (parkourGame.getEffects().isEmpty()) {
			return List.of(plugin.msg(M.List.EFFECT.empty));
		}
		sender.sendMessage(plugin.msg(M.List.EFFECT.header));
		return parkourGame.getEffects()
				.stream()
				.map(effect -> plugin.msg(M.List.EFFECT.item)
						.replace(Constants.EFFECT, effect.effectType().getName())
						.replace(Constants.AMPLIFIER, String.valueOf(effect.amplifier())))
				.map(text -> ChatColor.translateAlternateColorCodes('&', text))
				.toList();
	}

	@NotNull
	@Argument(permission = "ats.parkour.setEffect", description = "Sets parkour game effect")
	public String setEffect(@NotNull final ParkourGame parkourGame, @NotNull final PotionEffectType type,
							final int power) {
		if (parkourGame.isRunning() && plugin.isEditLockActive()) {
			return plugin.msg(M.Error.DEFAULT.forbiddenModification);
		}
		if (power <= 0) {
			parkourGame.getEffects().removeIf(effect -> effect.effectType().equals(type));
			return plugin.msg(M.Amplifier.EFFECT.removed)
					.replace(Constants.EFFECT, type.getName());
		}
		parkourGame.getEffects().add(new Effect(type, power));
		return plugin.msg(M.Amplifier.EFFECT.added)
				.replace(Constants.EFFECT, type.getName())
				.replace(Constants.AMPLIFIER, String.valueOf(power));
	}

	// === OPTIONS ===
	@NotNull
	@Argument(permission = "ats.parkour.sprintForced", description = "Shows value of sprintForced flag")
	public String sprintForced(@NotNull final ParkourGame parkourGame) {
		return plugin.msg(M.Option.SPRINT_FORCED.get)
				.replace(Constants.VALUE, String.valueOf(parkourGame.getOptions().isSprintForced()));
	}

	@NotNull
	@Argument(permission = "ats.parkour.sprintForced", description = "Sets value of sprintForced flag")
	public String sprintForced(@NotNull final ParkourGame parkourGame, final boolean sprintForced) {
		if (parkourGame.isRunning() && plugin.isEditLockActive()) {
			return plugin.msg(M.Error.DEFAULT.forbiddenModification);
		}
		parkourGame.getOptions().setSprintForced(sprintForced);
		return plugin.msg(M.Option.SPRINT_FORCED.set)
				.replace(Constants.VALUE, String.valueOf(sprintForced));
	}

	@NotNull
	@Argument(permission = "ats.parkour.alwaysSpawn", description = "Shows value of alwaysSpawn flag")
	public String alwaysSpawn(@NotNull final ParkourGame parkourGame) {
		return plugin.msg(M.Option.ALWAYS_SPAWN.get)
				.replace(Constants.VALUE, String.valueOf(parkourGame.getOptions().isAlwaysSpawn()));
	}

	@NotNull
	@Argument(permission = "ats.parkour.alwaysSpawn", description = "Sets value of alwaysSpawn flag")
	public String alwaysSpawn(@NotNull final ParkourGame parkourGame, final boolean alwaysSpawn) {
		if (parkourGame.isRunning() && plugin.isEditLockActive()) {
			return plugin.msg(M.Error.DEFAULT.forbiddenModification);
		}
		parkourGame.getOptions().setAlwaysSpawn(alwaysSpawn);
		return plugin.msg(M.Option.ALWAYS_SPAWN.set)
				.replace(Constants.VALUE, String.valueOf(alwaysSpawn));
	}

	@NotNull
	@Argument(permission = "ats.parkour.reward", description = "Shows value of reward")
	public String reward(@NotNull final ParkourGame parkourGame) {
		if (plugin.getFinancialProvider().isEmpty()) {
			return plugin.msg(M.Error.DEFAULT.noEconomy);
		}
		return plugin.msg(M.Option.REWARD.get)
				.replace(Constants.VALUE, plugin.formatMoney(parkourGame.getOptions().getReward()));
	}

	@NotNull
	@Argument(permission = "ats.parkour.reward", description = "Sets value of reward")
	public String reward(@NotNull final ParkourGame parkourGame, final double reward) {
		if (plugin.getFinancialProvider().isEmpty()) {
			return plugin.msg(M.Error.DEFAULT.noEconomy);
		}
		if (parkourGame.isRunning() && plugin.isEditLockActive()) {
			return plugin.msg(M.Error.DEFAULT.forbiddenModification);
		}
		parkourGame.getOptions().setReward(reward);
		return plugin.msg(M.Option.REWARD.set)
				.replace(Constants.VALUE, plugin.formatMoney(reward));
	}

	@NotNull
	@Argument(permission = "ats.parkour.savingResults", description = "Shows value of savingResults flag")
	public String savingResults(@NotNull final ParkourGame parkourGame) {
		return plugin.msg(M.Option.SAVING_RESULTS.get)
				.replace(Constants.VALUE, String.valueOf(parkourGame.getOptions().isSavingResults()));
	}

	@NotNull
	@Argument(permission = "ats.parkour.savingResults", description = "Sets value of savingResults flag")
	public String savingResults(@NotNull final ParkourGame parkourGame, final boolean savingResults) {
		if (parkourGame.isRunning() && plugin.isEditLockActive()) {
			return plugin.msg(M.Error.DEFAULT.forbiddenModification);
		}
		parkourGame.getOptions().setSavingResults(savingResults);
		return plugin.msg(M.Option.SAVING_RESULTS.set)
				.replace(Constants.VALUE, String.valueOf(savingResults));
	}

	@NotNull
	@Argument(permission = "ats.parkour.vipOnly", description = "Shows value of vipOnly flag")
	public String vipOnly(@NotNull final ParkourGame parkourGame) {
		return plugin.msg(M.Option.VIP_ONLY.get)
				.replace(Constants.VIP_ONLY, String.valueOf(parkourGame.getOptions().isVipOnly()));
	}

	@NotNull
	@Argument(permission = "ats.parkour.vipOnly", description = "Sets value of vipOnly flag")
	public String vipOnly(@NotNull final ParkourGame parkourGame, final boolean vipOnly) {
		if (parkourGame.isRunning() && plugin.isEditLockActive()) {
			return plugin.msg(M.Error.DEFAULT.forbiddenModification);
		}
		parkourGame.getOptions().setVipOnly(vipOnly);
		return plugin.msg(M.Option.VIP_ONLY.set)
				.replace(Constants.VALUE, String.valueOf(vipOnly));
	}

	@NotNull
	@Argument(permission = "ats.parkour.damageAllowed", description = "Shows value of damageAllowed flag")
	public String damageAllowed(@NotNull final ParkourGame parkourGame) {
		return plugin.msg(M.Option.DAMAGE_ALLOWED.get)
				.replace(Constants.VALUE, String.valueOf(parkourGame.getOptions().isDamageAllowed()));
	}

	@NotNull
	@Argument(permission = "ats.parkour.damageAllowed", description = "Sets value of damageAllowed flag")
	public String damageAllowed(@NotNull final ParkourGame parkourGame, final boolean damageAllowed) {
		if (parkourGame.isRunning() && plugin.isEditLockActive()) {
			return plugin.msg(M.Error.DEFAULT.forbiddenModification);
		}
		parkourGame.getOptions().setDamageAllowed(damageAllowed);
		return plugin.msg(M.Option.DAMAGE_ALLOWED.set)
				.replace(Constants.VALUE, String.valueOf(damageAllowed));
	}

	@NotNull
	@Argument(permission = "ats.parkour.boat", description = "Shows value of boat flag")
	public String boat(@NotNull final ParkourGame parkourGame) {
		return plugin.msg(M.Option.BOAT.get)
				.replace(Constants.VALUE, String.valueOf(parkourGame.getOptions().isBoat()));
	}

	@NotNull
	@Argument(permission = "ats.parkour.boat", description = "Sets value of boat flag")
	public String boat(@NotNull final ParkourGame parkourGame, final boolean boat) {
		if (parkourGame.isRunning() && plugin.isEditLockActive()) {
			return plugin.msg(M.Error.DEFAULT.forbiddenModification);
		}
		parkourGame.getOptions().setBoat(boat);
		return plugin.msg(M.Option.BOAT.set)
				.replace(Constants.VALUE, String.valueOf(boat));
	}

	@NotNull
	@Argument(permission = "ats.parkour.enabled", description = "Shows value of enabled flag")
	public String enabled(@NotNull final ParkourGame parkourGame) {
		return plugin.msg(M.Option.ENABLED.get)
				.replace(Constants.ENABLED, String.valueOf(parkourGame.getOptions().isEnabled()));
	}

	@NotNull
	@Argument(permission = "ats.parkour.enabled", description = "Sets value of enabled flag")
	public String enabled(@NotNull final ParkourGame parkourGame, final boolean enabled) {
		if (parkourGame.isRunning() && plugin.isEditLockActive()) {
			return plugin.msg(M.Error.DEFAULT.forbiddenModification);
		}
		parkourGame.getOptions().setEnabled(enabled);
		return plugin.msg(M.Option.ENABLED.set)
				.replace(Constants.ENABLED, String.valueOf(enabled));
	}

	@NotNull
	@Argument(permission = "ats.parkour.modifyInventory", description = "Shows value of modifyInventory flag")
	public String modifyInventory(@NotNull final ParkourGame parkourGame) {
		return plugin.msg(M.Option.MODIFY_INVENTORY.get)
				.replace(Constants.VALUE, String.valueOf(parkourGame.getOptions().isModifyInventory()));
	}

	@NotNull
	@Argument(permission = "ats.parkour.modifyInventory", description = "Sets value of modifyInventory flag")
	public String modifyInventory(@NotNull final ParkourGame parkourGame, final boolean modifyInventory) {
		if (parkourGame.isRunning() && plugin.isEditLockActive()) {
			return plugin.msg(M.Error.DEFAULT.forbiddenModification);
		}
		parkourGame.getOptions().setModifyInventory(modifyInventory);
		return plugin.msg(M.Option.MODIFY_INVENTORY.set)
				.replace(Constants.VALUE, String.valueOf(modifyInventory));
	}

	@NotNull
	@Argument(permission = "ats.parkour.info", description = "Shows information about parkour game")
	public String info(@NotNull final ParkourGame parkourGame) {
		return parkourGame.toString();
	}

	@NotNull
	@Argument(permission = "ats.parkour.difficulty", description = "Shows parkour game difficulty")
	public String difficulty(@NotNull final ParkourGame parkourGame) {
		return plugin.msg(M.Option.DIFFICULTY.get)
				.replace(Constants.VALUE, String.valueOf(parkourGame.getOptions().getDifficulty()));
	}

	@NotNull
	@Argument(permission = "ats.parkour.difficulty", description = "Sets parkour game difficulty")
	public String difficulty(@NotNull final ParkourGame parkourGame, final int difficulty) {
		if (parkourGame.isRunning() && plugin.isEditLockActive()) {
			return plugin.msg(M.Error.DEFAULT.forbiddenModification);
		}
		parkourGame.getOptions().setDifficulty(difficulty);
		return plugin.msg(M.Option.DIFFICULTY.set)
				.replace(Constants.VALUE, String.valueOf(difficulty));
	}

	@NotNull
	@Argument(permission = "ats.parkour.displayName", description = "Shows parkour game displayName")
	public String displayName(@NotNull final ParkourGame parkourGame) {
		return plugin.msg(M.Parkour.DISPLAY_NAME.get)
				.replace(Constants.VALUE, parkourGame.getDisplayName());
	}

	@NotNull
	@Argument(permission = "ats.parkour.displayName", description = "Sets parkour game displayName")
	public String displayName(@NotNull final ParkourGame parkourGame, @NotNull final String... newName) {
		if (parkourGame.isRunning() && plugin.isEditLockActive()) {
			return plugin.msg(M.Error.DEFAULT.forbiddenModification);
		}
		// Adding &r to reset colors in huge texts, e.g. `/pk info <name>`
		final String name = String.join(" ", newName) + "&r";
		parkourGame.setDisplayName(name);
		executeTutorial((Player) sender, 10, false);
		return plugin.msg(M.Parkour.DISPLAY_NAME.set).replace(Constants.VALUE, name);
	}

	@NotNull
	@Argument(permission = "ats.parkour.color", description = "Sets value of parkour color")
	public String color(@NotNull final ParkourGame parkourGame, @NotNull final DyeColor color) {
		if (parkourGame.isRunning() && plugin.isEditLockActive()) {
			return plugin.msg(M.Error.DEFAULT.forbiddenModification);
		}
		parkourGame.getOptions().setColor(color);
		return plugin.msg(M.Option.COLOR.set)
				.replace(Constants.VALUE, color.name());
	}

	@NotNull
	@Argument(permission = "ats.parkour.color", description = "Shows value of parkour color")
	public String color(@NotNull final ParkourGame parkourGame) {
		return plugin.msg(M.Option.COLOR.get)
				.replace(Constants.VALUE, parkourGame.getOptions().getColor().name());
	}

	@NotNull
	@Argument(permission = "ats.parkour.fee", description = "Sets value of parkour entrance fee")
	public String fee(@NotNull final ParkourGame parkourGame, final double fee) {
		if (plugin.getFinancialProvider().isEmpty()) {
			return plugin.msg(M.Error.DEFAULT.noEconomy);
		}
		if (parkourGame.isRunning() && plugin.isEditLockActive()) {
			return plugin.msg(M.Error.DEFAULT.forbiddenModification);
		}
		if (fee < 0) {
			return plugin.msg(M.Error.DEFAULT.negativeNumber);
		}
		parkourGame.getOptions().setFee(fee);
		return plugin.msg(M.Option.FEE.set).replace(Constants.VALUE, plugin.formatMoney(fee));
	}

	@NotNull
	@Argument(permission = "ats.parkour.fee", description = "Shows value of parkour entrance fee")
	public String fee(@NotNull final ParkourGame parkourGame) {
		if (plugin.getFinancialProvider().isEmpty()) {
			return plugin.msg(M.Error.DEFAULT.noEconomy);
		}
		return plugin.msg(M.Option.FEE.get).replace(Constants.VALUE, plugin.formatMoney(parkourGame.getOptions().getFee()));
	}

	@NotNull
	@Argument(permission = "ats.parkour.type", description = "Shows parkour type")
	public String type(@NotNull final ParkourGame parkourGame) {
		return plugin.msg(M.Option.TYPE.get).replace(Constants.VALUE, parkourGame.getOptions().getType().name());
	}

	@NotNull
	@Argument(permission = "ats.parkour.type", description = "Sets parkour type")
	public String type(@NotNull final ParkourGame parkourGame, @NotNull final ParkourGame.Type type) {
		if (parkourGame.isRunning() && plugin.isEditLockActive()) {
			return plugin.msg(M.Error.DEFAULT.forbiddenModification);
		}
		parkourGame.getOptions().setType(type);
		return plugin.msg(M.Option.TYPE.set).replace(Constants.VALUE, type.name());
	}

	@Nullable
	@Argument(permission = "ats.parkour.teleportBlock", description = "Sets parkour teleportBlock")
	public String teleportBlock(@NotNull final ParkourGame parkourGame) {
		if (parkourGame.isRunning() && plugin.isEditLockActive()) {
			return plugin.msg(M.Error.DEFAULT.forbiddenModification);
		}
		Optional.of(sender)
				.map(Player.class::cast)
				.map(player -> player.getTargetBlockExact(5))
				.map(Block::getLocation)
				.ifPresentOrElse(location -> {
					parkourGame.setTeleportBlock(location);
					sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.msg(M.Executive.TELEPORT_BLOCK.success)
							.replace(Constants.NAME, parkourGame.getName())
							.replace(Constants.DISPLAY_NAME, parkourGame.getDisplayName())
							.replace(Constants.COORD_X, plugin.formatCoord(location.getX()))
							.replace(Constants.COORD_Y, plugin.formatCoord(location.getY()))
							.replace(Constants.COORD_Z, plugin.formatCoord(location.getZ()))
							.replace(Constants.COORD_YAW, plugin.formatCoord(location.getYaw()))
							.replace(Constants.COORD_PITCH, plugin.formatCoord(location.getPitch()))));
				}, () -> sender.sendMessage(plugin.msg(M.Error.DEFAULT.notBlock)));
		return null;
	}

	@Argument(permission = "ats.parkour.top", description = "Shows top players for parkour game")
	public void top(final ParkourGame parkourGame, final int count) {
		final FetchParkourBestScoreTask fetchParkourBestScoreTask = new FetchParkourBestScoreTask(plugin, parkourGame, count, result -> {
			if (result.isEmpty()) {
				sender.sendMessage(plugin.msg(M.List.TOP.empty));
				return;
			}
			sender.sendMessage(plugin.msg(M.List.TOP.header));
			for (int i = 0; i < result.size(); i++) {
				final FetchParkourBestScoreTask.ParkourScore parkourScore = result.get(i);
				final String name = plugin.getServer().getOfflinePlayer(parkourScore.uuid()).getName();
				if (name == null) {
					continue;
				}
				sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.msg(M.List.TOP.item)
						.replace(Constants.NUMBER, String.valueOf(i + 1))
						.replace(Constants.PLAYER, name)
						.replace(Constants.PERSONAL_TIME, plugin.formatTime(parkourScore.time()))));
			}
		});
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, fetchParkourBestScoreTask);
	}

	@Nullable
	@Argument(permission = "ats.parkour.recordsBlock", description = "Sets recordsBlock sign location")
	public String recordsBlock(@NotNull final ParkourGame parkourGame) {
		if (parkourGame.isRunning() && plugin.isEditLockActive()) {
			return plugin.msg(M.Error.DEFAULT.forbiddenModification);
		}
		final Location location = ((Player) sender).getTargetBlock(null, 5).getLocation();
		final Material material = location.getBlock().getType();
		if (!Data.SIGNS.contains(material)) {
			return plugin.msg(M.Error.DEFAULT.notSign);
		}
		parkourGame.setRecordsBlock(location);
		sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.msg(M.Executive.RECORDS_BLOCK.success)
				.replace(Constants.NAME, parkourGame.getName())
				.replace(Constants.DISPLAY_NAME, parkourGame.getDisplayName())
				.replace(Constants.COORD_X, plugin.formatCoord(location.getX()))
				.replace(Constants.COORD_Y, plugin.formatCoord(location.getY()))
				.replace(Constants.COORD_Z, plugin.formatCoord(location.getZ()))
				.replace(Constants.COORD_YAW, plugin.formatCoord(location.getYaw()))
				.replace(Constants.COORD_PITCH, plugin.formatCoord(location.getPitch()))));

		final FetchParkourBestScoreTask fetchParkourBestScoreTask = new FetchParkourBestScoreTask(plugin, parkourGame, 1, data ->
				data.stream().findFirst()
						.ifPresentOrElse(plugin::updateSyncSign, () -> plugin.updateSyncSign(parkourGame)));
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, fetchParkourBestScoreTask);
		return null;
	}

	// === MEDALS ===
	@Argument(permission = "ats.parkour.medal", description = "Sets parkour medal data")
	public String medal(@NotNull final ParkourGame parkourGame, @NotNull final Medal medal,
						@NotNull final MedalSetupOption option, final double value) {
		if (option.isEconomyRequired() && plugin.getFinancialProvider().isEmpty()) {
			return plugin.msg(M.Error.DEFAULT.noEconomy);
		}
		if (!plugin.getMedals().contains(medal)) {
			return plugin.msg(M.Error.DEFAULT.noMedal)
					.replace(Constants.MEDALS, plugin.getFormattedMedals("&c, "));
		}
		if (value < 0) {
			return plugin.msg(M.Error.DEFAULT.negativeNumber);
		}
		final List<ParkourMedal> medals = parkourGame.getMedals();
		final ParkourMedal medalData = medals.stream()
				.filter(element -> element.getMedal().equals(medal))
				.findAny()
				.orElse(new ParkourMedal(medal));

		if (!verify(medalData, parkourGame, value, option.getGetterFunction(), option.getValuesRelation())) {
			return plugin.msg(M.Error.DEFAULT.boundsExceeded);
		}
		option.set(medalData, value);

		final Player player = (Player) sender;
		if (option.equals(MedalSetupOption.TIME)) {
			plugin.getFinancialProvider().ifPresentOrElse(
					ignored -> executeTutorial(player, 7, false),
					() -> {
						if (plugin.getTutorialManager().hasPlayer(player)) {
							final TutorialPlayer tutorialPlayer = plugin.getTutorialManager().getPlayer(player);
							if (tutorialPlayer.done(7, false)) {
								tutorialPlayer.next().next().sendMessage();
							}
						}
					});
		}
		if (option.equals(MedalSetupOption.REWARD)) {
			executeTutorial(player, 8, false);
		}

		final DoubleFunction<String> resultFunction = option.equals(MedalSetupOption.TIME) ? plugin::formatTime : plugin::formatMoney;

		medals.stream()
				.filter(element -> element.getMedal().equals(medal))
				.findAny()
				.ifPresentOrElse(parkourMedal -> {
					parkourMedal.setTime(medalData.getTime());
					parkourMedal.setReward(medalData.getReward());
				}, () -> medals.add(medalData));
		return plugin.msg(M.Option.MEDAL.set)
				.replace(Constants.OPTION, option.toString().toLowerCase())
				.replace(Constants.MEDAL, medal.displayName())
				.replace(Constants.VALUE, resultFunction.apply(value));
	}

	@Argument(permission = "ats.parkour.medal", description = "Gets specified medal data for specified parkour")
	public String medal(@NotNull final ParkourGame parkourGame, @NotNull final Medal medal,
						@NotNull final MedalSetupOption option) {
		if (option.isEconomyRequired() && plugin.getFinancialProvider().isEmpty()) {
			return plugin.msg(M.Error.DEFAULT.noEconomy);
		}
		if (!plugin.getMedals().contains(medal)) {
			return plugin.msg(M.Error.DEFAULT.noMedal).replace(Constants.MEDALS, plugin.getFormattedMedals("&c, "));
		}
		final @NotNull List<ParkourMedal> medals = parkourGame.getMedals();
		final Optional<ParkourMedal> parkourMedalData = medals.stream()
				.filter(medalData -> medalData.getMedal().equals(medal))
				.findAny();
		if (parkourMedalData.isEmpty()) {
			return plugin.msg(M.Error.DEFAULT.noMedal);
		}
		final ParkourMedal data = parkourMedalData.get();
		final DoubleFunction<String> resultFunction = option.equals(MedalSetupOption.TIME) ? plugin::formatTime : plugin::formatMoney;
		return plugin.msg(M.Option.MEDAL.get)
				.replace(Constants.OPTION, option.toString().toLowerCase())
				.replace(Constants.MEDAL, medal.displayName())
				.replace(Constants.VALUE, resultFunction.apply(option.get(data)));
	}

	@Argument(permission = "ats.parkour.medal", description = "Gets medal data for specified parkour")
	public String medal(@NotNull final ParkourGame parkourGame, final Medal medal) {
		if (!plugin.getMedals().contains(medal)) {
			return plugin.msg(M.Error.DEFAULT.noMedal).replace(Constants.MEDALS, plugin.getFormattedMedals("&c, "));
		}
		final List<ParkourMedal> medalsData = parkourGame.getMedals();
		final Optional<ParkourMedal> medalData = medalsData.stream().filter(m -> m.getMedal().equals(medal)).findFirst();
		if (medalData.isEmpty()) {
			return plugin.msg(M.Error.DEFAULT.noMedal);
		}
		final ParkourMedal parkourMedal = medalData.get();
		return plugin.msg(M.List.MEDAL.item)
				.replace(Constants.MEDAL, medal.displayName())
				.replace(Constants.TIME, plugin.formatTime(parkourMedal.getTime()))
				.replace(Constants.VALUE, plugin.formatMoney(parkourMedal.getReward()));
	}

	@NotNull
	@Argument(permission = "ats.parkour.medal", description = "Gets all medal data for parkour")
	public List<String> medal(@NotNull final ParkourGame parkourGame) {
		final @NotNull List<ParkourMedal> medals = parkourGame.getMedals();
		if (medals.isEmpty()) {
			return List.of(plugin.msg(M.List.MEDAL.empty));
		}
		sender.sendMessage(plugin.msg(M.List.MEDAL.header));
		return medals.stream()
				.sorted((o1, o2) -> o2.getMedal().compareTo(o1.getMedal()))
				.map(medalEntry -> plugin.msg(M.List.MEDAL.item)
						.replace(Constants.MEDAL, medalEntry.getMedal().displayName())
						.replace(Constants.TIME, plugin.formatTime(medalEntry.getTime()))
						.replace(Constants.VALUE, plugin.formatMoney(medalEntry.getReward())))
				.map(text -> ChatColor.translateAlternateColorCodes('&', text))
				.toList();
	}

	// === FALLBACKS ===

	@NotNull
	@TypeFallback(ParkourGame.class)
	public String parkourGameFallback(@NotNull final String parkourGame) {
		return plugin.msg(M.Error.DEFAULT.invalidGame).replace("%GAME%", parkourGame);
	}

	@NotNull
	@TypeFallback(PotionEffectType.class)
	public String effectFallback(@NotNull final String value) {
		return plugin.msg(M.Error.DEFAULT.invalidEffect).replace("%EFFECT%", value);
	}

	@NotNull
	@TypeFallback(DyeColor.class)
	public String colorFallback(@NotNull final String value) {
		return plugin.msg(M.Error.DEFAULT.invalidColor).replace("%COLOR%", value);
	}

	@NotNull
	@TypeFallback(ParkourGame.Type.class)
	public String typeFallback(@NotNull final String value) {
		return plugin.msg(M.Error.DEFAULT.invalidType).replace("%TYPE%", value);
	}

	@NotNull
	@TypeFallback(MedalSetupOption.class)
	public String medalSetupOptionFallback(final String value) {
		return plugin.msg(M.Error.DEFAULT.invalidMedalOption);
	}

	// === UTILITIES ===

	@Nullable
	private CuboidRegion getSelectionRegion(@NotNull final Player player) {
		final LocalSession session = plugin.getWorldEdit().getSession(player);
		try {
			final com.sk89q.worldedit.world.World world = BukkitAdapter.adapt(player.getWorld());
			final Region region = session.getSelection(world);
			if (region == null) {
				return null;
			}
			return new CuboidRegion(world, region.getMaximumPoint(), region.getMinimumPoint());
		} catch (final IncompleteRegionException e) {
			// Do nothing
		}
		return null;
	}

	private boolean executeTutorial(@NotNull final Player player, final int id) {
		return executeTutorial(player, id, true);
	}

	private boolean executeTutorial(@NotNull final Player player, final int id, final boolean mistakeInformation) {
		if (!plugin.getTutorialManager().hasPlayer(player)) {
			return false;
		}
		final TutorialPlayer tutorialPlayer = plugin.getTutorialManager().getPlayer(player);
		if (!tutorialPlayer.done(id, mistakeInformation)) {
			return false;
		}
		tutorialPlayer.next().sendMessage();
		return true;
	}

	private boolean verify(@NotNull final ParkourMedal medalData,
						   @NotNull final ParkourGame parkourGame,
						   final double value,
						   @NotNull final ToDoubleFunction<ParkourMedal> function,
						   @NotNull final BiPredicate<Double, Double> relation) {
		final boolean betterIsOk = parkourGame.getMedals().stream()
				.filter(parkourMedal -> parkourMedal.getMedal().importance() < medalData.getMedal().importance())
				.findAny()
				.filter(data -> function.applyAsDouble(data) > 0 && relation.test(function.applyAsDouble(data), value))
				.isEmpty();
		final boolean worseIsOk = parkourGame.getMedals().stream()
				.filter(parkourMedal -> parkourMedal.getMedal().importance() > medalData.getMedal().importance())
				.findAny()
				.filter(data -> function.applyAsDouble(data) > 0 && !relation.test(function.applyAsDouble(data), value))
				.isEmpty();
		return betterIsOk && worseIsOk;
	}

	private boolean verifyMedals(@NotNull final ParkourGame parkourGame) {
		final MedalRequirement medalRequirement = plugin.getMedalRequirement();
		if (medalRequirement.equals(MedalRequirement.NONE)) {
			return true;
		}
		if (medalRequirement.equals(MedalRequirement.ALL) && plugin.getFinancialProvider().isPresent()) {
			return verifyMedalsCount(parkourGame) && parkourGame.getMedals().stream()
					.allMatch(m -> m.getTime() > EPSILON && m.getReward() > EPSILON);
		}
		return verifyMedalsCount(parkourGame);
	}

	private boolean verifyMedalsCount(@NotNull final ParkourGame parkourGame) {
		return parkourGame.getMedals().size() == plugin.getMedals().size();
	}
}
