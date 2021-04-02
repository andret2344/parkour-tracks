/*
 * Copyright Andret (c) 2019-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import eu.andret.arguments.AnnotatedCommandExecutor;
import eu.andret.arguments.api.annotation.Argument;
import eu.andret.arguments.api.annotation.BaseCommand;
import eu.andret.arguments.api.annotation.Completer;
import eu.andret.arguments.api.annotation.Fallback;
import eu.andret.arguments.api.annotation.Param;
import eu.andret.arguments.api.entity.ExecutorType;
import eu.andret.ats.parkour.event.game.GameStartEvent;
import eu.andret.ats.parkour.event.game.GameStopEvent;
import eu.andret.ats.parkour.event.player.PlayerQuitGameEvent;
import eu.andret.ats.parkour.parkour.ParkourGame;
import eu.andret.ats.parkour.parkour.ParkourMedal;
import eu.andret.ats.parkour.parkour.ParkourMedalData;
import eu.andret.ats.parkour.parkour.ParkourRecord;
import eu.andret.ats.parkour.player.ParkourPlayer;
import eu.andret.ats.parkour.region.BasicRegion;
import eu.andret.ats.parkour.region.DirectionalRegion;
import eu.andret.ats.parkour.tasks.database.FetchParkourBestRecordTask;
import eu.andret.ats.parkour.util.Data;
import eu.andret.ats.parkour.util.M;
import lombok.EqualsAndHashCode;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

@BaseCommand("parkour")
@EqualsAndHashCode(callSuper = true)
public final class ParkourCommand extends AnnotatedCommandExecutor<ParkourPlugin> {
	private static final String VALUE = "%VALUE%";
	private static final String NAME = "%NAME%";
	private static final String EFFECT = "%EFFECT%";
	private static final String AMPLIFIER = "%AMPLIFIER%";
	private static final String COORD_X = "%COORD_X%";
	private static final String COORD_Y = "%COORD_Y%";
	private static final String COORD_Z = "%COORD_Z%";
	private static final String MEDALS = "%MEDALS%";
	private static final String OPTION = "%OPTION%";
	private static final String MEDAL_PLACEHOLDER = "%MEDAL%";

	public ParkourCommand(final CommandSender sender, final ParkourPlugin plugin) {
		super(sender, plugin);
	}

	// === TUTORIAL ===

	@Argument(permission = "ats.parkour.tutorial", executorType = ExecutorType.PLAYER, description = "Runs tutorial")
	public void tutorial(@Completer("startStop") final String state) {
		final Player player = (Player) sender;
		if (state.equals("start")) {
			if (TutorialManager.hasPlayer(player)) {
				sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&dYou are already in tutorial... Be polite!"));
				return;
			}
			final TutorialPlayer tutorialPlayer = TutorialManager.getPlayer(player);
			tutorialPlayer.sendMessage();
			return;
		}
		if (state.equals("stop")) {
			if (!TutorialManager.hasPlayer(player)) {
				sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&dYou aren't in in tutorial... Be polite!"));
				return;
			}
			TutorialManager.removePlayer(player);
			sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&dOh, that's sad you don't want to learn anymore, but I appreciate your knowledge. Bye!"));
			return;
		}
		sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&dWhat to do with tutorial? Set it to true or false? Please, specify"));
	}

	// === UNIVERSAL ===

	@Argument(permission = "ats.parkour.lobby", executorType = ExecutorType.PLAYER, description = "Teleports to lobby")
	public String lobby() {
		final Player player = (Player) sender;
		plugin.getParkourManager().teleportToLobby(player);
		final Location location = plugin.getParkourManager().getLobbyLocation();
		if (location == null) {
			executeTutorial(player, 0);
			return "No lobby!";
		}
		if (TutorialManager.hasPlayer(player)) {
			final TutorialPlayer tutorialPlayer = TutorialManager.getPlayer(player);
			if (tutorialPlayer.done(0, true)) {
				tutorialPlayer.next().next().sendMessage().next().next().sendMessage();
			}
		}
		return plugin.msg(M.General.LOBBY.success)
				.replace(COORD_X, String.valueOf(location.getBlockX()))
				.replace(COORD_Y, String.valueOf(location.getBlockY()))
				.replace(COORD_Z, String.valueOf(location.getBlockZ()));
	}

	@Argument(permission = "ats.parkour.setLobby", executorType = ExecutorType.PLAYER, description = "Sets lobby location to player's location")
	public String setLobby() {
		final Player player = (Player) sender;
		final Location location = player.getLocation();
		plugin.getParkourManager().setLobbyLocation(location);
		if (TutorialManager.hasPlayer(player)) {
			final TutorialPlayer tutorialPlayer = TutorialManager.getPlayer(player);
			if (tutorialPlayer.done(1, true)) {
				tutorialPlayer.next().next().next().sendMessage().next().sendMessage();
			}
		}
		return plugin.msg(M.General.SET_LOBBY.success)
				.replace(COORD_X, String.valueOf(location.getBlockX()))
				.replace(COORD_Y, String.valueOf(location.getBlockY()))
				.replace(COORD_Z, String.valueOf(location.getBlockZ()));
	}

	@Argument(permission = "ats.parkour.list", description = "Lists all parkour games", aliases = "ls")
	public String list() {
		if (plugin.getParkourManager().getAllGames().isEmpty()) {
			return plugin.msg(M.List.GAMES.empty);
		}
		sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.msg(M.List.GAMES.header)
				.replace("%COUNT%", String.valueOf(plugin.getParkourManager().getAllGames().size()))));
		final List<ParkourGame> allGames = plugin.getParkourManager().getAllGames();
		for (int i = 0; i < allGames.size(); i++) {
			final ParkourGame parkourGame = allGames.get(i);
			final String running;
			if (parkourGame.isRunning()) {
				running = ChatColor.GREEN + "" + ChatColor.ITALIC + "[Started]";
			} else {
				running = ChatColor.RED + "" + ChatColor.ITALIC + "[Stopped]";
			}
			sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.msg(M.List.GAMES.item)
					.replace("%ID%", String.valueOf(i + 1))
					.replace(NAME, parkourGame.getName())
					.replace("%RUNNING%", running)));
		}
		return null;
	}

	@Argument(permission = "ats.parkour.fix", description = "Fixes signs after database connection troubles.")
	public void fix() {
		plugin.getConnection().ifPresentOrElse(connection -> {
			plugin.getParkourManager().getAllGames()
					.stream()
					.map(parkourGame -> new FetchParkourBestRecordTask(connection, parkourGame, 1, data -> {
						final ParkourRecord record = data.isEmpty() ? new ParkourRecord(null, parkourGame, 0) : data.get(0);
						plugin.updateSyncSign(record);
					}
					))
					.forEach(fetchParkourBestRecordTask -> plugin.getServer().getScheduler().runTaskAsynchronously(plugin, fetchParkourBestRecordTask));
			sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.msg(M.General.FIX.success)));
		}, () -> sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.msg(M.Error.DEFAULT.notConnected))));
	}

	@Argument(permission = "ats.parkour.ignore", executorType = ExecutorType.PLAYER, description = "Allows sender to ignore parkour regions interaction", aliases = "i")
	public String ignore() {
		final Player player = (Player) sender;
		final ParkourPlayer parkourPlayer = plugin.getPlayerManager().getParkourPlayer(player);
		if (parkourPlayer.isIgnoring()) {
			parkourPlayer.setIgnoring(false);
			plugin.getParkourManager().getAllGames().stream()
					.filter(parkourGame -> plugin.getParkourManager().inAnyRegion(parkourGame, player))
					.forEach(parkourGame -> plugin.getParkourManager().teleportToLobby(player));
			return plugin.msg(M.General.IGNORE.success).replace(VALUE, "false");
		}
		parkourPlayer.setIgnoring(true);
		final ParkourGame parkour = plugin.getParkourManager().getParkour(player);
		if (parkour != null) {
			plugin.getServer().getPluginManager().callEvent(new PlayerQuitGameEvent(parkour, parkourPlayer));
			parkour.removePlayer(parkourPlayer);
		}
		return plugin.msg(M.General.IGNORE.success).replace(VALUE, "true");
	}

	@Argument(permission = "ats.parkour.help", description = "Shows help page", aliases = "?")
	public void help() {
		help(1);
	}

	@Argument(permission = "ats.parkour.help", description = "Shows help page", aliases = "?")
	public void help(final int page) {
		final Map<String, String> messages = plugin.getHelpDescription();
		final int maxPages = (int) Math.ceil(messages.size() / 5.);
		final int skip = 5 * (page - 1);
		if (page > maxPages) {
			return;
		}
		sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.msg(M.List.HELP.header)
				.replace("%PAGE%", String.valueOf(page))
				.replace("%PAGES%", String.valueOf(maxPages))));
		messages.entrySet()
				.stream()
				.skip(skip)
				.limit(5)
				.map(command -> plugin.msg(M.List.HELP.item)
						.replace("%ARGUMENT%", command.getKey())
						.replace("%DESCRIPTION%", command.getValue()))
				.map(text -> ChatColor.translateAlternateColorCodes('&', text))
				.forEach(sender::sendMessage);
	}

	// === GENERAL ===

	@Argument(permission = "ats.parkour.create", executorType = ExecutorType.PLAYER, description = "Creates parkour game")
	public String create(final String name) {
		if (plugin.getParkourManager().getLobbyLocation() == null) {
			return plugin.msg(M.Error.DEFAULT.missingLobby);
		}
		final Player player = (Player) sender;
		final CuboidRegion selection = getRegionSelection(player);
		if (selection == null) {
			return plugin.msg(M.Error.DEFAULT.invalidSelection);
		}
		if (!name.matches("[a-zA-Z0-9_-]+")) {
			return plugin.msg(M.Error.DEFAULT.invalidName);
		}
		final ParkourGame parkour = plugin.getParkourManager().getParkour(name);
		if (parkour != null) {
			return plugin.msg(M.Error.DEFAULT.alreadyExists).replace(NAME, name);
		}
		executeTutorial(player, 4);
		final ParkourGame parkourGame = plugin.getParkourManager().createParkour(name, new BasicRegion(selection), player.getLocation().getWorld());
		plugin.getParkourManager().addParkour(parkourGame);
		return plugin.msg(M.Executive.CREATE.success).replace(NAME, parkourGame.getName());
	}

	@Argument(permission = "ats.parkour.remove", description = "Removes parkour game")
	public String remove(@Param("parkourGame") final ParkourGame parkourGame) {
		if (parkourGame.isRunning() && plugin.isEditLocked()) {
			return plugin.msg(M.Error.DEFAULT.forbiddenModification);
		}
		plugin.getParkourManager().removeParkour(parkourGame);
		return plugin.msg(M.Executive.REMOVE.success).replace(NAME, parkourGame.getName());
	}

	@Fallback
	public String remove() {
		return plugin.msg(M.Error.DEFAULT.invalidGame);
	}

	@Argument(permission = "ats.parkour.start", description = "Starts parkour game")
	public String start(@Param("parkourGame") final ParkourGame parkourGame) {
		if (parkourGame.getSpawn() == null) {
			return plugin.msg(M.Error.DEFAULT.missingSpawn);
		}
		if (parkourGame.isRunning()) {
			return plugin.msg(M.Error.DEFAULT.alreadyStarted);
		}
		if (parkourGame.getCheckpoints().isEmpty()) {
			return plugin.msg(M.Error.DEFAULT.missingCheckpoint);
		}
		if (sender instanceof Player) {
			final Player player = (Player) sender;
			if (executeTutorial(player, 11)) {
				TutorialManager.removePlayer(player);
			}
		}
		parkourGame.setRunning(true);
		plugin.getServer().getPluginManager().callEvent(new GameStartEvent(parkourGame));
		return plugin.msg(M.Executive.START.success).replace(NAME, parkourGame.getName());
	}

	@Fallback
	public String start() {
		return plugin.msg(M.Error.DEFAULT.invalidGame);
	}

	@Argument(permission = "ats.parkour.stop", description = "Stops parkour game")
	public String stop(@Param("parkourGame") final ParkourGame parkourGame) {
		if (!parkourGame.isRunning()) {
			return plugin.msg(M.Error.DEFAULT.alreadyStopped);
		}
		parkourGame.setRunning(false);
		plugin.getServer().getPluginManager().callEvent(new GameStopEvent(parkourGame));
		return plugin.msg(M.Executive.STOP.success).replace(NAME, parkourGame.getName());
	}

	@Fallback
	public String stop() {
		return plugin.msg(M.Error.DEFAULT.invalidGame);
	}

	@Argument(permission = "ats.parkour.recreate", executorType = ExecutorType.PLAYER, description = "Sets new region for parkour game")
	public String recreate(@Param("parkourGame") final ParkourGame parkourGame) {
		if (parkourGame.isRunning() && plugin.isEditLocked()) {
			return plugin.msg(M.Error.DEFAULT.forbiddenModification);
		}
		final CuboidRegion selection = getRegionSelection((Player) sender);
		if (selection == null) {
			return plugin.msg(M.Error.DEFAULT.invalidSelection);
		}
		parkourGame.setRegion(new BasicRegion(selection));
		return plugin.msg(M.Executive.RECREATE.success);
	}

	@Fallback
	public String recreate() {
		return plugin.msg(M.Error.DEFAULT.invalidGame);
	}

	@Argument(permission = "ats.parkour.rename", description = "Sets new name for parkour game")
	public String rename(@Param("parkourGame") final ParkourGame parkourGame, final String name) {
		if (parkourGame.isRunning() && plugin.isEditLocked()) {
			return plugin.msg(M.Error.DEFAULT.forbiddenModification);
		}
		if (plugin.getParkourManager().getParkour(name) != null) {
			return plugin.msg(M.Error.DEFAULT.alreadyExists).replace(NAME, name);
		}
		parkourGame.setName(name);
		return plugin.msg(M.Executive.RENAME.success)
				.replace("%OLD_NAME%", parkourGame.getName())
				.replace("%NEW_NAME%", name);
	}

	@Fallback
	public String rename() {
		return plugin.msg(M.Error.DEFAULT.invalidGame);
	}

	@Argument(permission = "ats.parkour.teleport", executorType = ExecutorType.PLAYER, description = "Teleports sender to parkours spawn region", aliases = "tp")
	public String teleport(@Param("parkourGame") final ParkourGame parkourGame) {
		final ParkourPlayer parkourPlayer = plugin.getPlayerManager().getParkourPlayer((Player) sender);
		parkourGame.addPlayer(parkourPlayer);
		parkourPlayer.reset();
		if (parkourGame.getSpawn() == null) {
			return plugin.msg(M.Error.DEFAULT.missingSpawn);
		}
		plugin.getPlayerManager().teleportToRegion(parkourPlayer, parkourGame.getSpawn());
		return plugin.msg(M.Executive.TELEPORT.success).replace(NAME, parkourGame.getName());
	}

	@Fallback
	public String teleport() {
		return plugin.msg(M.Error.DEFAULT.invalidGame);
	}

	// === REGIONS ===

	@Argument(permission = "ats.parkour.setSpawn", description = "Sets parkour spawn region", executorType = ExecutorType.PLAYER)
	public String setSpawn(@Param("parkourGame") final ParkourGame parkourGame) {
		if (parkourGame.isRunning() && plugin.isEditLocked()) {
			return plugin.msg(M.Error.DEFAULT.forbiddenModification);
		}
		final Player player = (Player) sender;
		final CuboidRegion selection = getRegionSelection(player);
		if (selection == null) {
			return plugin.msg(M.Error.DEFAULT.invalidSelection);
		}
		executeTutorial(player, 5);
		final Location location = (player).getLocation();
		parkourGame.setSpawn(new DirectionalRegion(selection, location.getYaw(), location.getPitch()));
		return plugin.msg(M.Executive.SPAWN.success);
	}

	@Fallback
	public String setSpawn() {
		return plugin.msg(M.Error.DEFAULT.invalidGame);
	}

	@Argument(permission = "ats.parkour.addCheckpoint", description = "Adds checkpoint to parkour game", executorType = ExecutorType.PLAYER)
	public String addCheckpoint(@Param("parkourGame") final ParkourGame parkourGame) {
		if (parkourGame.isRunning() && plugin.isEditLocked()) {
			return plugin.msg(M.Error.DEFAULT.forbiddenModification);
		}
		final Player player = (Player) sender;
		final CuboidRegion selection = getRegionSelection(player);
		if (selection == null) {
			return plugin.msg(M.Error.DEFAULT.invalidSelection);
		}
		if (parkourGame.getSpawn() == null) {
			return plugin.msg(M.Error.DEFAULT.missingSpawn);
		}
		executeTutorial(player, 6, false);
		final Location location = player.getLocation();
		parkourGame.getCheckpoints().add(new DirectionalRegion(selection, location.getYaw(), location.getPitch()));
		return plugin.msg(M.Region.Checkpoint.ADD.success).replace("%ID%", String.valueOf(parkourGame.getCheckpoints().size()));
	}

	@Fallback
	public String addCheckpoint() {
		return plugin.msg(M.Error.DEFAULT.invalidGame);
	}

	@Argument(permission = "ats.parkour.setCheckpoint", description = "Sets specified parkour game checkpoint", executorType = ExecutorType.PLAYER)
	public String setCheckpoint(@Param("parkourGame") final ParkourGame parkourGame, final int id) {
		if (parkourGame.isRunning() && plugin.isEditLocked()) {
			return plugin.msg(M.Error.DEFAULT.forbiddenModification);
		}
		final CuboidRegion selection = getRegionSelection((Player) sender);
		if (selection == null) {
			return plugin.msg(M.Error.DEFAULT.invalidSelection);
		}
		if (id <= 0) {
			return plugin.msg(M.Error.DEFAULT.negativeNumber);
		}
		if (id > parkourGame.getCheckpoints().size()) {
			return plugin.msg(M.Error.DEFAULT.tooLargeNumber);
		}
		final Location location = ((Player) sender).getLocation();
		parkourGame.getCheckpoints().add(id - 1, new DirectionalRegion(selection, location.getYaw(), location.getPitch()));
		return plugin.msg(M.Region.Checkpoint.SET.success).replace("%ID%", String.valueOf(id));
	}

	@Fallback
	public String setCheckpoint() {
		return plugin.msg(M.Error.DEFAULT.invalidGame);
	}

	@Argument(permission = "ats.parkour.addWall", description = "Adds wall to parkour game", executorType = ExecutorType.PLAYER)
	public String addWall(@Param("parkourGame") final ParkourGame parkourGame) {
		if (parkourGame.isRunning() && plugin.isEditLocked()) {
			return plugin.msg(M.Error.DEFAULT.forbiddenModification);
		}
		final Player player = (Player) sender;
		final CuboidRegion selection = getRegionSelection(player);
		if (selection == null) {
			return plugin.msg(M.Error.DEFAULT.invalidSelection);
		}
		parkourGame.getWalls().add(new BasicRegion(selection));
		return plugin.msg(M.Region.Wall.ADD.success).replace("%ID%", String.valueOf(parkourGame.getWalls().size()));
	}

	@Fallback
	public String addWall() {
		return plugin.msg(M.Error.DEFAULT.invalidGame);
	}

	@Argument(permission = "ats.parkour.setWall", description = "Sets specified parkour game wall", executorType = ExecutorType.PLAYER)
	public String setWall(@Param("parkourGame") final ParkourGame parkourGame, final int id) {
		if (parkourGame.isRunning() && plugin.isEditLocked()) {
			return plugin.msg(M.Error.DEFAULT.forbiddenModification);
		}
		final CuboidRegion selection = getRegionSelection((Player) sender);
		if (selection == null) {
			return plugin.msg(M.Error.DEFAULT.invalidSelection);
		}
		if (id <= 0) {
			return plugin.msg(M.Error.DEFAULT.negativeNumber);
		}
		if (id > parkourGame.getWalls().size()) {
			return plugin.msg(M.Error.DEFAULT.tooLargeNumber);
		}
		parkourGame.getWalls().set(id - 1, new BasicRegion(selection));
		return plugin.msg(M.Region.Wall.SET.success).replace("%ID%", String.valueOf(id));
	}

	@Fallback
	public String setWall() {
		return plugin.msg(M.Error.DEFAULT.invalidGame);
	}

	@Argument(permission = "ats.parkour.effects", description = "Shows current parkour effects")
	public String effects(@Param("parkourGame") final ParkourGame parkourGame) {
		if (parkourGame.getEffects().isEmpty()) {
			return plugin.msg(M.List.EFFECT.empty);
		}
		sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.msg(M.List.EFFECT.header)));
		parkourGame.getEffects().entrySet()
				.stream()
				.map(entry -> plugin.msg(M.List.EFFECT.item)
						.replace(EFFECT, entry.getKey().getName())
						.replace(AMPLIFIER, String.valueOf(entry.getValue())))
				.map(text -> ChatColor.translateAlternateColorCodes('&', text))
				.forEach(sender::sendMessage);
		return null;
	}

	@Fallback
	public String effects() {
		return plugin.msg(M.Error.DEFAULT.invalidGame);
	}

	@Argument(permission = "ats.parkour.setEffect", description = "Sets parkour game effect", aliases = "se")
	public String setEffect(@Param("parkourGame") final ParkourGame parkourGame, @Param("potion") final PotionEffectType type, final int power) {
		if (parkourGame.isRunning() && plugin.isEditLocked()) {
			return plugin.msg(M.Error.DEFAULT.forbiddenModification);
		}
		if (power <= 0) {
			parkourGame.getEffects().remove(type);
			return plugin.msg(M.Amplifier.EFFECT.removed).replace(EFFECT, type.getName());
		}
		parkourGame.getEffects().put(type, power);
		return plugin.msg(M.Amplifier.EFFECT.added).replace(EFFECT, type.getName()).replace(AMPLIFIER, String.valueOf(power));
	}

	@Fallback
	public String setEffect() {
		return plugin.msg(M.Error.DEFAULT.invalidGame);
	}

	// === OPTIONS ===

	@Argument(permission = "ats.parkour.sprintForced", description = "Shows value of sprintForced flag")
	public String sprintForced(@Param("parkourGame") final ParkourGame parkourGame) {
		return plugin.msg(M.Option.SPRINT_FORCED.get).replace(VALUE, String.valueOf(parkourGame.getOptions().isSprintForced()));
	}

	@Argument(permission = "ats.parkour.sprintForced", description = "Sets value of sprintForced flag")
	public String sprintForced(@Param("parkourGame") final ParkourGame parkourGame, final boolean sprintForced) {
		if (parkourGame.isRunning() && plugin.isEditLocked()) {
			return plugin.msg(M.Error.DEFAULT.forbiddenModification);
		}
		parkourGame.getOptions().setSprintForced(sprintForced);
		return plugin.msg(M.Option.SPRINT_FORCED.set).replace(VALUE, String.valueOf(sprintForced));
	}

	@Fallback
	public String sprintForced() {
		return plugin.msg(M.Error.DEFAULT.invalidGame);
	}

	@Argument(permission = "ats.parkour.alwaysSpawn", description = "Shows value of alwaysSpawn flag")
	public String alwaysSpawn(@Param("parkourGame") final ParkourGame parkourGame) {
		return plugin.msg(M.Option.ALWAYS_SPAWN.get).replace(VALUE, String.valueOf(parkourGame.getOptions().isAlwaysSpawn()));
	}

	@Argument(permission = "ats.parkour.alwaysSpawn", description = "Sets value of alwaysSpawn flag")
	public String alwaysSpawn(@Param("parkourGame") final ParkourGame parkourGame, final boolean alwaysSpawn) {
		if (parkourGame.isRunning() && plugin.isEditLocked()) {
			return plugin.msg(M.Error.DEFAULT.forbiddenModification);
		}
		parkourGame.getOptions().setAlwaysSpawn(alwaysSpawn);
		return plugin.msg(M.Option.ALWAYS_SPAWN.set).replace(VALUE, String.valueOf(alwaysSpawn));
	}

	@Fallback
	public String alwaysSpawn() {
		return plugin.msg(M.Error.DEFAULT.invalidGame);
	}

	@Argument(permission = "ats.parkour.savingResults", description = "Shows value of savingResults flag")
	public String savingResults(@Param("parkourGame") final ParkourGame parkourGame) {
		return plugin.msg(M.Option.SAVING_RESULTS.get).replace(VALUE, String.valueOf(parkourGame.getOptions().isSavingResults()));
	}

	@Argument(permission = "ats.parkour.savingResults", description = "Sets value of savingResults flag")
	public String savingResults(@Param("parkourGame") final ParkourGame parkourGame, final boolean savingResults) {
		if (parkourGame.isRunning() && plugin.isEditLocked()) {
			return plugin.msg(M.Error.DEFAULT.forbiddenModification);
		}
		parkourGame.getOptions().setSavingResults(savingResults);
		return plugin.msg(M.Option.SAVING_RESULTS.set).replace(VALUE, String.valueOf(savingResults));
	}

	@Fallback
	public String savingResults() {
		return plugin.msg(M.Error.DEFAULT.invalidGame);
	}

	@Argument(permission = "ats.parkour.vipOnly", description = "Shows value of vipOnly flag")
	public String vipOnly(@Param("parkourGame") final ParkourGame parkourGame) {
		return plugin.msg(M.Option.VIP_ONLY.get).replace("%VIP_ONLY%", String.valueOf(parkourGame.getOptions().isVipOnly()));
	}

	@Argument(permission = "ats.parkour.vipOnly", description = "Sets value of vipOnly flag")
	public String vipOnly(@Param("parkourGame") final ParkourGame parkourGame, final boolean vipOnly) {
		if (parkourGame.isRunning() && plugin.isEditLocked()) {
			return plugin.msg(M.Error.DEFAULT.forbiddenModification);
		}
		parkourGame.getOptions().setVipOnly(vipOnly);
		return plugin.msg(M.Option.VIP_ONLY.set).replace(VALUE, String.valueOf(vipOnly));
	}

	@Fallback
	public String vipOnly() {
		return plugin.msg(M.Error.DEFAULT.invalidGame);
	}

	@Argument(permission = "ats.parkour.damageAllowed", description = "Shows value of damageAllowed flag")
	public String damageAllowed(@Param("parkourGame") final ParkourGame parkourGame) {
		return plugin.msg(M.Option.DAMAGE_ALLOWED.get).replace(VALUE, String.valueOf(parkourGame.getOptions().isDamageAllowed()));
	}

	@Argument(permission = "ats.parkour.damageAllowed", description = "Sets value of damageAllowed flag")
	public String damageAllowed(@Param("parkourGame") final ParkourGame parkourGame, final boolean damageAllowed) {
		if (parkourGame.isRunning() && plugin.isEditLocked()) {
			return plugin.msg(M.Error.DEFAULT.forbiddenModification);
		}
		parkourGame.getOptions().setDamageAllowed(damageAllowed);
		return plugin.msg(M.Option.DAMAGE_ALLOWED.set).replace(VALUE, String.valueOf(damageAllowed));
	}

	@Fallback
	public String damageAllowed() {
		return plugin.msg(M.Error.DEFAULT.invalidGame);
	}

	@Argument(permission = "ats.parkour.boat", description = "Shows value of boat flag")
	public String boat(@Param("parkourGame") final ParkourGame parkourGame) {
		return plugin.msg(M.Option.BOAT.get).replace(VALUE, String.valueOf(parkourGame.getOptions().isBoat()));
	}

	@Argument(permission = "ats.parkour.boat", description = "Sets value of boat flag")
	public String boat(@Param("parkourGame") final ParkourGame parkourGame, final boolean boat) {
		if (parkourGame.isRunning() && plugin.isEditLocked()) {
			return plugin.msg(M.Error.DEFAULT.forbiddenModification);
		}
		parkourGame.getOptions().setBoat(boat);
		return plugin.msg(M.Option.BOAT.set).replace(VALUE, String.valueOf(boat));
	}

	@Fallback
	public String boat() {
		return plugin.msg(M.Error.DEFAULT.invalidGame);
	}

	@Argument(permission = "ats.parkour.enabled", description = "Shows value of enabled flag")
	public String enabled(@Param("parkourGame") final ParkourGame parkourGame) {
		return plugin.msg(M.Option.ENABLED.get).replace("%ENABLED%", String.valueOf(parkourGame.getOptions().isEnabled()));
	}

	@Argument(permission = "ats.parkour.enabled", description = "Sets value of enabled flag")
	public String enabled(@Param("parkourGame") final ParkourGame parkourGame, final boolean enabled) {
		if (parkourGame.isRunning() && plugin.isEditLocked()) {
			return plugin.msg(M.Error.DEFAULT.forbiddenModification);
		}
		parkourGame.getOptions().setEnabled(enabled);
		return plugin.msg(M.Option.ENABLED.set).replace("%ENABLED%", String.valueOf(enabled));
	}

	@Fallback
	public String enabled() {
		return plugin.msg(M.Error.DEFAULT.invalidGame);
	}

	@Argument(permission = "ats.parkour.modifyInventory", description = "Shows value of modifyInventory flag")
	public String modifyInventory(@Param("parkourGame") final ParkourGame parkourGame) {
		return plugin.msg(M.Option.MODIFY_INVENTORY.get).replace(VALUE, String.valueOf(parkourGame.getOptions().isModifyInventory()));
	}

	@Argument(permission = "ats.parkour.modifyInventory", description = "Sets value of modifyInventory flag")
	public String modifyInventory(@Param("parkourGame") final ParkourGame parkourGame, final boolean modifyInventory) {
		if (parkourGame.isRunning() && plugin.isEditLocked()) {
			return plugin.msg(M.Error.DEFAULT.forbiddenModification);
		}
		parkourGame.getOptions().setModifyInventory(modifyInventory);
		return plugin.msg(M.Option.MODIFY_INVENTORY.set).replace(VALUE, String.valueOf(modifyInventory));
	}

	@Fallback
	public String modifyInventory() {
		return plugin.msg(M.Error.DEFAULT.invalidGame);
	}

	@Argument(permission = "ats.parkour.info", description = "Shows information about parkour game")
	public String info(@Param("parkourGame") final ParkourGame parkourGame) {
		return parkourGame.toString();
	}

	@Fallback
	public String info(final String parkourGame) {
		return plugin.msg(M.Error.DEFAULT.invalidGame);
	}

	@Argument(permission = "ats.parkour.difficulty", description = "Shows parkour game difficulty")
	public String difficulty(@Param("parkourGame") final ParkourGame parkourGame) {
		return plugin.msg(M.Option.DIFFICULTY.get).replace(VALUE, String.valueOf(parkourGame.getOptions().getDifficulty()));
	}

	@Argument(permission = "ats.parkour.difficulty", description = "Sets parkour game difficulty")
	public String difficulty(@Param("parkourGame") final ParkourGame parkourGame, final int difficulty) {
		if (parkourGame.isRunning() && plugin.isEditLocked()) {
			return plugin.msg(M.Error.DEFAULT.forbiddenModification);
		}
		parkourGame.getOptions().setDifficulty(difficulty);
		return plugin.msg(M.Option.DIFFICULTY.set).replace(VALUE, String.valueOf(difficulty));
	}

	@Fallback
	public String difficulty() {
		return plugin.msg(M.Error.DEFAULT.invalidGame);
	}

	@Argument(permission = "ats.parkour.displayName", description = "Shows parkour game displayName")
	public String displayName(@Param("parkourGame") final ParkourGame parkourGame) {
		return plugin.msg(M.Parkour.DISPLAY_NAME.get).replace(VALUE, parkourGame.getDisplayName());
	}

	@Argument(permission = "ats.parkour.displayName", description = "Sets parkour game displayName")
	public String displayName(@Param("parkourGame") final ParkourGame parkourGame, final String... newName) {
		if (parkourGame.isRunning() && plugin.isEditLocked()) {
			return plugin.msg(M.Error.DEFAULT.forbiddenModification);
		}
		final String name = String.join(" ", newName);
		parkourGame.setDisplayName(name);
		return plugin.msg(M.Parkour.DISPLAY_NAME.set).replace(VALUE, name);
	}

	@Fallback
	public String displayName() {
		return plugin.msg(M.Error.DEFAULT.invalidGame);
	}

	@Argument(permission = "ats.parkour.color", description = "Sets value of parkour color")
	public String color(@Param("parkourGame") final ParkourGame parkourGame, @Param("color") final DyeColor color) {
		if (parkourGame.isRunning() && plugin.isEditLocked()) {
			return plugin.msg(M.Error.DEFAULT.forbiddenModification);
		}
		parkourGame.getOptions().setColor(color);
		return plugin.msg(M.Option.COLOR.set).replace(VALUE, color.name());
	}

	@Argument(permission = "ats.parkour.color", description = "Shows value of parkour color")
	public String color(@Param("parkourGame") final ParkourGame parkourGame) {
		return plugin.msg(M.Option.COLOR.get).replace(VALUE, parkourGame.getOptions().getColor().name());
	}

	@Fallback
	public String color() {
		return plugin.msg(M.Error.DEFAULT.invalidGame);
	}

	@Argument(permission = "ats.parkour.fee", description = "Sets value of parkour entrance fee")
	public String fee(@Param("parkourGame") final ParkourGame parkourGame, final double fee) {
		if (parkourGame.isRunning() && plugin.isEditLocked()) {
			return plugin.msg(M.Error.DEFAULT.forbiddenModification);
		}
		if (fee < 0) {
			return plugin.msg(M.Error.DEFAULT.negativeNumber);
		}
		if (plugin.getFinancialProvider().isEmpty()) {
			return plugin.msg(M.Error.DEFAULT.noEconomy);
		}
		parkourGame.getOptions().setFee(fee);
		return plugin.msg(M.Option.FEE.set).replace(VALUE, String.valueOf(fee));
	}

	@Argument(permission = "ats.parkour.fee", description = "Shows value of parkour entrance fee")
	public String fee(@Param("parkourGame") final ParkourGame parkourGame) {
		return plugin.msg(M.Option.FEE.get).replace(VALUE, String.valueOf(parkourGame.getOptions().getFee()));
	}

	@Fallback
	public String fee() {
		return plugin.msg(M.Error.DEFAULT.invalidGame);
	}

	@Argument(permission = "ats.parkour.authors", description = "Shows parkour authors")
	public String authors(@Param("parkourGame") final ParkourGame parkourGame) {
		return plugin.msg(M.Parkour.AUTHORS.get).replace(VALUE, parkourGame.getAuthors().toString());
	}

	@Argument(permission = "ats.parkour.authors", description = "Sets parkour authors")
	public String authors(@Param("parkourGame") final ParkourGame parkourGame, final String... authors) {
		if (parkourGame.isRunning() && plugin.isEditLocked()) {
			return plugin.msg(M.Error.DEFAULT.forbiddenModification);
		}
		parkourGame.getAuthors().clear();
		parkourGame.getAuthors().addAll(Arrays.asList(authors));
		return plugin.msg(M.Parkour.AUTHORS.set).replace(VALUE, Arrays.toString(authors));
	}

	@Fallback
	public String authors() {
		return plugin.msg(M.Error.DEFAULT.invalidGame);
	}

	@Argument(permission = "ats.parkour.type", description = "Shows parkour type")
	public String type(@Param("parkourGame") final ParkourGame parkourGame) {
		return plugin.msg(M.Option.TYPE.get).replace(VALUE, parkourGame.getOptions().getType().name());
	}

	@Argument(permission = "ats.parkour.type", description = "Sets parkour type")
	public String type(@Param("parkourGame") final ParkourGame parkourGame, final ParkourGame.Type type) {
		if (parkourGame.isRunning() && plugin.isEditLocked()) {
			return plugin.msg(M.Error.DEFAULT.forbiddenModification);
		}
		parkourGame.getOptions().setType(type);
		return plugin.msg(M.Option.TYPE.set).replace(VALUE, type.name());
	}

	@Fallback
	public String type() {
		return plugin.msg(M.Error.DEFAULT.invalidGame);
	}

	@Argument(permission = "ats.parkour.teleportBlock", description = "Sets parkour teleportBlock")
	public String teleportBlock(@Param("parkourGame") final ParkourGame parkourGame) {
		if (parkourGame.isRunning() && plugin.isEditLocked()) {
			return plugin.msg(M.Error.DEFAULT.forbiddenModification);
		}
		Optional.of(sender)
				.map(Player.class::cast)
				.map(player -> player.getTargetBlockExact(5))
				.map(Block::getLocation)
				.ifPresentOrElse(location -> {
					parkourGame.setTeleportBlock(location);
					sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.msg(M.Executive.TELEPORT_BLOCK.success).replace("%PARKOUR%", parkourGame.getName())));
				}, () -> sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.msg(M.Error.DEFAULT.notBlock))));
		return null;
	}

	@Fallback
	public String teleportBlock() {
		return plugin.msg(M.Error.DEFAULT.invalidGame);
	}

	@Argument(permission = "ats.parkour.top", description = "Shows top players for parkour game")
	public void top(@Param("parkourGame") final ParkourGame parkourGame, final int count) {
		plugin.getConnection()
				.map(connection -> new FetchParkourBestRecordTask(connection, parkourGame, count, result -> {
					if (result.isEmpty()) {
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.msg(M.List.TOP.empty)));
						return;
					}
					sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.msg(M.List.TOP.header)));
					for (int i = 0; i < result.size(); i++) {
						final ParkourRecord parkourRecord = result.get(i);
						final String name = plugin.getServer().getOfflinePlayer(parkourRecord.getUuid()).getName();
						if (name == null) {
							continue;
						}
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.msg(M.List.TOP.item)
								.replace("%NUMBER%", String.valueOf(i + 1))
								.replace("%PLAYER%", name)
								.replace("%PERSONAL_TIME%", plugin.formatTime(parkourRecord.getTime()))));
					}
				}))
				.ifPresent(topPlayersDataOperations -> plugin.getServer().getScheduler().runTaskAsynchronously(plugin, topPlayersDataOperations));
	}

	@Fallback
	public String top() {
		return plugin.msg(M.Error.DEFAULT.invalidGame);
	}

	@Argument(permission = "ats.parkour.recordsBlock", description = "Sets recordsBlock sign location")
	public String recordsBlock(@Param("parkourGame") final ParkourGame parkourGame) {
		if (parkourGame.isRunning() && plugin.isEditLocked()) {
			return plugin.msg(M.Error.DEFAULT.forbiddenModification);
		}
		final Location location = ((Player) sender).getTargetBlock(null, 5).getLocation();
		final Material material = location.getBlock().getType();
		if (!Data.SIGNS.contains(material)) {
			return plugin.msg(M.Error.DEFAULT.notSign);
		}
		parkourGame.setRecordsBlock(location);
		sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.msg(M.Executive.RECORDS_BLOCK.success)
				.replace(COORD_X, String.valueOf(location.getX()))
				.replace(COORD_Y, String.valueOf(location.getY()))
				.replace(COORD_Z, String.valueOf(location.getZ()))));

		plugin.getConnection()
				.map(connection -> new FetchParkourBestRecordTask(connection, parkourGame, 1, data -> {
					final ParkourRecord record = data.isEmpty() ? new ParkourRecord(null, parkourGame, 0) : data.get(0);
					plugin.updateSyncSign(record);
				}))
				.ifPresentOrElse(
						fetchParkourBestRecordTask -> plugin.getServer().getScheduler().runTaskAsynchronously(plugin, fetchParkourBestRecordTask),
						() -> sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.msg(M.Error.DEFAULT.notConnected))));
		return null;
	}

	@Fallback
	public String recordsBlock() {
		return plugin.msg(M.Error.DEFAULT.invalidGame);
	}

	// === MEDALS ===

	@Argument(permission = "ats.parkour.medal", description = "Sets parkour medal data")
	public String medal(@Param("parkourGame") final ParkourGame parkourGame, @Completer("medal") @Param("medal") final ParkourMedal medal, @Completer("medalOption") final String option, final double value) {
		if (!plugin.getMedals().contains(medal)) {
			return plugin.msg(M.Error.DEFAULT.noMedal).replace(MEDALS, plugin.getMedals().stream().map(ParkourMedal::getDisplay).collect(Collectors.joining("&r, ")));
		}
		if (value < 0) {
			return plugin.msg(M.Error.DEFAULT.negativeNumber);
		}
		final Map<ParkourMedal, ParkourMedalData> medals = parkourGame.getMedals();
		final ParkourMedalData medalData = medals.getOrDefault(medal, new ParkourMedalData());
		if (option.equals("reward")) {
			if (plugin.getFinancialProvider().isEmpty()) {
				return plugin.msg(M.Error.DEFAULT.noEconomy);
			}
			if (!verify(medal, parkourGame, value, ParkourMedalData::getReward)) {
				return plugin.msg(M.Error.DEFAULT.boundsExceeded);
			}
			medalData.setReward(value);
		} else if (option.equals("time")) {
			if (!verify(medal, parkourGame, value, ParkourMedalData::getTime)) {
				return plugin.msg(M.Error.DEFAULT.boundsExceeded);
			}
			medalData.setTime(value);
		} else {
			return plugin.msg(M.Error.DEFAULT.noMedalData);
		}

		medals.put(medal, medalData);
		return plugin.msg(M.Option.MEDAL.set)
				.replace(OPTION, option)
				.replace(MEDAL_PLACEHOLDER, medal.getDisplay())
				.replace(VALUE, String.valueOf(value));
	}

	@Argument(permission = "ats.parkour.medal", description = "Gets specified medal data for specified parkour")
	public String medal(@Param("parkourGame") final ParkourGame parkourGame, @Completer("medal") @Param("medal") final ParkourMedal medal, @Completer("medalOption") final String option) {
		if (!plugin.getMedals().contains(medal)) {
			return plugin.msg(M.Error.DEFAULT.noMedal).replace(MEDALS, plugin.getMedals().stream().map(ParkourMedal::getDisplay).collect(Collectors.joining("&r, ")));
		}
		final Map<ParkourMedal, ParkourMedalData> medals = parkourGame.getMedals();
		if (!medals.containsKey(medal)) {
			return plugin.msg(M.Error.DEFAULT.noMedal);
		}
		final ParkourMedalData data = medals.get(medal);
		if (option.equals("reward")) {
			return plugin.msg(M.Option.MEDAL.get)
					.replace(OPTION, option)
					.replace(MEDAL_PLACEHOLDER, medal.getDisplay())
					.replace(VALUE, String.valueOf(data.getReward()));
		}
		if (option.equals("time")) {
			return plugin.msg(M.Option.MEDAL.get)
					.replace(OPTION, option)
					.replace(MEDAL_PLACEHOLDER, medal.getDisplay())
					.replace(VALUE, String.valueOf(data.getTime()));
		}
		return plugin.msg(M.Error.DEFAULT.noMedalData);
	}

	@Argument(permission = "ats.parkour.medal", description = "Gets medal data for specified parkour")
	public String medal(@Param("parkourGame") final ParkourGame parkourGame, @Completer("medal") @Param("medal") final ParkourMedal medal) {
		if (!plugin.getMedals().contains(medal)) {
			return plugin.msg(M.Error.DEFAULT.noMedal).replace(MEDALS, plugin.getMedals().stream().map(ParkourMedal::getDisplay).collect(Collectors.joining("&r, ")));
		}
		final Map<ParkourMedal, ParkourMedalData> medals = parkourGame.getMedals();
		if (!medals.containsKey(medal)) {
			return plugin.msg(M.Error.DEFAULT.noMedal);
		}
		final ParkourMedalData data = medals.get(medal);
		return plugin.msg(M.List.MEDAL.item)
				.replace(MEDAL_PLACEHOLDER, medal.getDisplay())
				.replace("%TIME%", String.valueOf(data.getTime()))
				.replace("%REWARD%", String.valueOf(data.getReward()));
	}

	@Argument(permission = "ats.parkour.medal", description = "Gets all medal data for parkour")
	public String medal(@Param("parkourGame") final ParkourGame parkourGame) {
		final Map<ParkourMedal, ParkourMedalData> medals = parkourGame.getMedals();
		if (medals.isEmpty()) {
			return plugin.msg(M.List.MEDAL.empty);
		}
		sender.sendMessage(plugin.msg(M.List.MEDAL.header));
		medals.entrySet().stream()
				.sorted((o1, o2) -> o2.getKey().compareTo(o1.getKey()))
				.map(medalEntry -> plugin.msg(M.List.MEDAL.item)
						.replace(MEDAL_PLACEHOLDER, medalEntry.getKey().getDisplay())
						.replace("%TIME%", String.valueOf(medalEntry.getValue().getTime()))
						.replace("%REWARD%", String.valueOf(medalEntry.getValue().getReward())))
				.forEach(sender::sendMessage);
		return null;
	}

	@Fallback
	public String medal() {
		return plugin.msg(M.Error.DEFAULT.invalidGame);
	}

	// === UTILITIES ===

	private CuboidRegion getRegionSelection(final Player player) {
		final LocalSession session = plugin.getWorldEdit().getSession(player);
		try {
			final World world = BukkitAdapter.adapt(player.getWorld());
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

	private boolean executeTutorial(final Player player, final int i) {
		return executeTutorial(player, i, true);
	}

	private boolean executeTutorial(final Player player, final int i, final boolean mistakeInformation) {
		if (!TutorialManager.hasPlayer(player)) {
			return false;
		}
		final TutorialPlayer tutorialPlayer = TutorialManager.getPlayer(player);
		if (!tutorialPlayer.done(i, mistakeInformation)) {
			return false;
		}
		tutorialPlayer.next().sendMessage();
		return true;
	}

	private boolean verify(final ParkourMedal medal, final ParkourGame parkourGame, final double value, final ToDoubleFunction<ParkourMedalData> function) {
		final Optional<ParkourMedal> better = parkourGame.getMedals().keySet().stream().filter(parkourMedal -> parkourMedal.getImportance() < medal.getImportance()).findAny();
		final Optional<ParkourMedal> worse = parkourGame.getMedals().keySet().stream().filter(parkourMedal -> parkourMedal.getImportance() > medal.getImportance()).findAny();
		final boolean betterIsOk = better.map(parkourGame.getMedals()::get).filter(data -> function.applyAsDouble(data) > value).isEmpty();
		final boolean worseIsOk = worse.map(parkourGame.getMedals()::get).filter(data -> function.applyAsDouble(data) < value).isEmpty();
		return betterIsOk && worseIsOk;
	}
}
