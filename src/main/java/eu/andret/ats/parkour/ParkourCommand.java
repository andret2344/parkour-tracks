/*
 * Copyright Andret (c) 2019. Copying and modifying allowed only keeping git link reference.
 */
package eu.andret.ats.parkour;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.AbstractWorld;
import eu.andret.arguments.AnnotatedCommandExecutor;
import eu.andret.arguments.api.annotation.Argument;
import eu.andret.arguments.api.annotation.BaseCommand;
import eu.andret.arguments.api.annotation.Fallback;
import eu.andret.arguments.api.annotation.Param;
import eu.andret.arguments.api.entity.ExecutorType;
import eu.andret.ats.parkour.event.game.GameStartEvent;
import eu.andret.ats.parkour.event.game.GameStopEvent;
import eu.andret.ats.parkour.parkour.Parkour;
import eu.andret.ats.parkour.parkour.ParkourGame;
import eu.andret.ats.parkour.parkour.ParkourManager;
import eu.andret.ats.parkour.player.ParkourPlayer;
import eu.andret.ats.parkour.player.PlayerManager;
import eu.andret.ats.parkour.region.AbstractRegion;
import eu.andret.ats.parkour.region.DirectionalRegion;
import eu.andret.ats.parkour.tasks.RepairSignTask;
import eu.andret.ats.parkour.tasks.TopPlayersDataOperations;
import eu.andret.ats.parkour.util.Data;
import lombok.EqualsAndHashCode;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

@BaseCommand("parkour")
@EqualsAndHashCode(callSuper = true)
public class ParkourCommand extends AnnotatedCommandExecutor<ParkourPlugin> {
	public ParkourCommand(final CommandSender sender, final ParkourPlugin plugin) {
		super(sender, plugin);
	}

	// === UNIVERSAL ===

	@Argument(permission = "ats.parkour.lobby", executorType = ExecutorType.PLAYER, description = "Sets lobby location to executor location")
	public String lobby() {
		final Location l = ((Player) sender).getLocation();
		ParkourManager.setLobbyLocation(l);
		return msg("setLobby", false)
				.replace("%COORD_X%", String.valueOf(l.getX()))
				.replace("%COORD_Y%", String.valueOf(l.getY()))
				.replace("%COORD_Z%", String.valueOf(l.getZ()));
	}

	@Argument(permission = "ats.parkour.list", description = "Lists all parkour games", aliases = "ls")
	public String list() {
		if (ParkourManager.getAllGames().isEmpty()) {
			return msg("noParkours", false);
		}
		sender.sendMessage(msg("listHeader", false).replace("%COUNT%", String.valueOf(ParkourManager.getAllGames().size())));
		final List<ParkourGame> allGames = ParkourManager.getAllGames();
		for (int i = 1; i <= allGames.size(); i++) {
			final ParkourGame parkourGame = allGames.get(i - 1);
			final String on;
			if (parkourGame.isRunning()) {
				on = ChatColor.GREEN + "" + ChatColor.ITALIC + "[Started]";
			} else {
				on = ChatColor.RED + "" + ChatColor.ITALIC + "[Stopped]";
			}
			sender.sendMessage(String.format("%d. %s %s", i, parkourGame.getName(), on));
		}
		return null;
	}

	@Argument(permission = "ats.parkour.fix", description = "Fixes signs after database connection troubles.")
	public void fix() {
		plugin.getConnection().ifPresentOrElse(connection -> {
			ParkourManager.getAllGames()
					.stream()
					.map(parkourGame -> new RepairSignTask(connection, parkourGame, (s, f) -> plugin.updateSign(s, f, parkourGame)))
					.forEach(repairSignTask -> plugin.getServer().getScheduler().runTaskAsynchronously(plugin, repairSignTask));
			sender.sendMessage(msg("successFix", false));
		}, () -> {
			sender.sendMessage(msg("noDatabase", false));
		});
	}

	@Argument(permission = "ats.parkour.ignore", executorType = ExecutorType.PLAYER, description = "Allows sender to ignore parkour regions interaction", aliases = "i")
	public String ignore() {
		// FIXME: Nor working!
		final Player player = (Player) sender;
		final ParkourPlayer parkourPlayer = PlayerManager.getParkourSinglePlayer(player);
		if (parkourPlayer.isIgnoring()) {
			parkourPlayer.setIgnoring(false);
			ParkourManager.getAllGames().stream()
					.filter(p -> p.getAllRegions().stream().filter(Objects::nonNull).anyMatch(x -> x.contains(player.getLocation())))
					.forEach(p -> p.addPlayer(player));
			return msg("ignoreStop", false);
		}
		parkourPlayer.setIgnoring(true);
		final ParkourGame parkour = ParkourManager.getParkour(player);
		if (parkour != null) {
			parkour.removePlayer(player);
		}
		return msg("ignoreStart", false);
	}

	@Argument(permission = "ats.parkour.spectate", executorType = ExecutorType.PLAYER, description = "Allows sender to spectate")
	public String spectate() {
		// FIXME: Not working!
		final Player player = (Player) sender;
		final ParkourPlayer parkourPlayer = PlayerManager.getParkourSinglePlayer(player);
		if (parkourPlayer.isSpectating()) {
			parkourPlayer.setSpectating(false);
			plugin.getServer().getOnlinePlayers().forEach(p -> p.showPlayer(plugin, player));
			ParkourManager.getAllGames().stream()
					.filter(p -> p.getAllRegions().stream().anyMatch(x -> x.contains(player.getLocation())))
					.forEach(p -> p.addPlayer(player));
			return msg("spectateStop", false);
		} else {
			parkourPlayer.setSpectating(true);
			plugin.getServer().getOnlinePlayers().forEach(p -> p.hidePlayer(plugin, player));
			final ParkourGame parkour = ParkourManager.getParkour(player);
			if (parkour != null) {
				parkour.removePlayer(player);
			}
			return msg("spectateStart", false);
		}
	}

	@Argument(permission = "ats.parkour.help", description = "Shows help page", aliases = "?")
	public void help() {
		help(1);
	}

	@Argument(permission = "ats.parkour.help", description = "Shows help page", aliases = "?")
	public void help(final int page) {
		final Map<String, String> messages = plugin.getMessages();
		final int maxPages = (int) Math.ceil(messages.size() / 5.);
		final int skip = 5 * (page - 1);
		if (page > maxPages) {
			return;
		}
		sender.sendMessage(msg("currentPage", false)
				.replace("%PAGE%", String.valueOf(page))
				.replace("%PAGES%", String.valueOf(maxPages)));
		messages.entrySet()
				.stream()
				.skip(skip)
				.limit(5)
				.map(command -> String.format("%s/parkour %s%s - %s", ChatColor.GREEN.toString(), command.getKey(), ChatColor.RESET.toString(), command.getValue()))
				.forEach(sender::sendMessage);
	}

	// === GENERAL ===

	@Argument(permission = "ats.parkour.create", executorType = ExecutorType.PLAYER, description = "Creates parkour game", aliases = "c")
	public String create(final String name) {
		if (ParkourManager.getLobbyLocation() == null) {
			return msg("lobbyFirst", true);
		}
		final CuboidRegion selection = getRegionSelection((Player) sender);
		if (selection == null) {
			return msg("wrongSel", true);
		}
		if (!name.matches("[a-zA-Z0-9_-]+")) {
			return msg("wrongName", true);
		}
		final ParkourGame parkour = ParkourManager.getParkour(name);
		if (parkour != null) {
			return msg("gameExists", true).replace("%NAME%", name);
		}
		final Parkour parkourGame = new Parkour(name, new AbstractRegion(selection), ((Player) sender).getLocation().getWorld());
		ParkourManager.addParkour(parkourGame);
		return msg("gameCreated", false).replace("%NAME%", parkourGame.getName());
	}

	@Argument(permission = "ats.parkour.remove", description = "Removes parkour game", aliases = "r")
	public String remove(@Param("parkourGame") final ParkourGame parkourGame) {
		ParkourManager.removeParkour(parkourGame);
		return msg("gameRemoved", false).replace("%NAME%", parkourGame.getName());
	}

	@Fallback
	public String remove() {
		return msg("noGame", true);
	}

	@Argument(permission = "ats.parkour.start", description = "Starts parkour game")
	public String start(@Param("parkourGame") final ParkourGame parkourGame) {
		if (parkourGame.getSpawn() == null) {
			return msg("noSpawn", true);
		}
		final ParkourGame.Options options = parkourGame.getOptions();
		if (options.getBronze() == 0 || options.getSilver() == 0 || options.getGold() == 0 || options.getPlatinum() == 0) {
			return msg("medalsFirst", true);
		}
		if (parkourGame.isRunning()) {
			return msg("alreadyStarted", true);
		}
		if (parkourGame.getCheckpoints().isEmpty()) {
			return msg("noCheckpoint", true);
		}
		parkourGame.setRunning(true);
		plugin.getServer().getPluginManager().callEvent(new GameStartEvent(parkourGame));
		ParkourManager.sortGames();
		return msg("gameStarted", false).replace("%NAME%", parkourGame.getName());
	}

	@Fallback
	public String start() {
		return msg("noGame", true);
	}

	@Argument(permission = "ats.parkour.stop", description = "Stops parkour game")
	public String stop(@Param("parkourGame") final ParkourGame parkourGame) {
		if (!parkourGame.isRunning()) {
			return msg("alreadyStopped", true);
		}
		parkourGame.setRunning(false);
		plugin.getServer().getPluginManager().callEvent(new GameStopEvent(parkourGame));
		ParkourManager.sortGames();
		return msg("gameStopped", false).replace("%NAME%", parkourGame.getName());
	}

	@Fallback
	public String stop() {
		return msg("noGame", true);
	}

	@Argument(permission = "ats.parkour.recreate", executorType = ExecutorType.PLAYER, description = "Sets new region for parkour game")
	public String recreate(@Param("parkourGame") final ParkourGame parkourGame) {
		final CuboidRegion selection = getRegionSelection((Player) sender);
		if (selection == null) {
			return msg("wrongSel", true);
		}
		parkourGame.setGameRegion(new AbstractRegion(selection));
		return msg("gameRecreated", false);
	}

	@Fallback
	public String recreate() {
		return msg("noGame", true);
	}

	@Argument(permission = "ats.parkour.rename", description = "Sets new name for parkour game")
	public String rename(@Param("parkourGame") final ParkourGame parkourGame, final String name) {
		if (ParkourManager.getParkour(name) != null) {
			return msg("gameExists", true).replace("%NAME%", name);
		}
		parkourGame.setName(name);
		return msg("gameRenamed", false).replace("%OLD_NAME%", parkourGame.getName()).replace("%NEW_NAME%", name);
	}

	@Fallback
	public String rename() {
		return msg("noGame", true);
	}

	@Argument(permission = "ats.parkour.teleport", executorType = ExecutorType.PLAYER, description = "Teleports sender to parkours spawn region")
	public String teleport(@Param("parkourGame") final ParkourGame parkourGame) {
		parkourGame.addPlayer((Player) sender);
		final ParkourPlayer parkourPlayer = PlayerManager.getParkourSinglePlayer((Player) sender);
		parkourPlayer.reset();
		if (!parkourPlayer.teleportToSpawn()) {
			return msg("noSpawn", true);
		}
		return msg("teleported", false).replace("%NAME%", parkourGame.getName());
	}

	@Fallback
	public String teleport() {
		return msg("noGame", true);
	}

	// === REGIONS ===

	@Argument(permission = "ats.parkour.setSpawn", description = "Sets parkour spawn region", executorType = ExecutorType.PLAYER, aliases = "ss")
	public String setSpawn(@Param("parkourGame") final ParkourGame parkourGame) {
		final CuboidRegion selection = getRegionSelection((Player) sender);
		if (selection == null) {
			return msg("wrongSel", true);
		}
		final Location location = ((Player) sender).getLocation();
		parkourGame.setSpawn(new DirectionalRegion(selection, location.getYaw(), location.getPitch()));
		return msg("setSpawn", false);
	}

	@Fallback
	public String setSpawn() {
		return msg("noGame", true);
	}

	@Argument(permission = "ats.parkour.addCheckpoint", description = "Adds checkpoint to parkour game", executorType = ExecutorType.PLAYER, aliases = "ac")
	public String addCheckpoint(@Param("parkourGame") final ParkourGame parkourGame) {
		final CuboidRegion selection = getRegionSelection((Player) sender);
		if (selection == null) {
			return msg("wrongSel", true);
		}
		if (parkourGame.getSpawn() == null) {
			return msg("noSpawn", true);
		}
		final Location l = ((Player) sender).getLocation();
		parkourGame.getCheckpoints().add(new DirectionalRegion(selection, l.getYaw(), l.getPitch()));
		return msg("setCheckpoint", false).replace("%CHECKPOINT_ID%", String.valueOf(parkourGame.getCheckpoints().size() - 1));
	}

	@Fallback
	public String addCheckpoint() {
		return msg("noGame", true);
	}

	@Argument(permission = "ats.parkour.setCheckpoint", description = "Sets specified parkour game checkpoint", executorType = ExecutorType.PLAYER, aliases = "sc")
	public String setCheckpoint(@Param("parkourGame") final ParkourGame parkourGame, final int id) {
		final CuboidRegion selection = getRegionSelection((Player) sender);
		if (selection == null) {
			return msg("wrongSel", true);
		}
		if (id <= 0) {
			return msg("negativeNumber", true);
		}
		if (id > parkourGame.getLastCheckpointId()) {
			return msg("tooLargeNumber", true);
		}
		final Location location = ((Player) sender).getLocation();
		parkourGame.getCheckpoints().add(id, new DirectionalRegion(selection, location.getYaw(), location.getPitch()));
		return msg("setCheckpoint", false).replace("%CHECKPOINT_ID%", String.valueOf(id));
	}

	@Fallback
	public String setCheckpoint() {
		return msg("noGame", true);
	}

	@Argument(permission = "ats.parkour.addWall", description = "Adds wall to parkour game", executorType = ExecutorType.PLAYER, aliases = "aw")
	public String addWall(@Param("parkourGame") final ParkourGame parkourGame) {
		final CuboidRegion selection = getRegionSelection((Player) sender);
		if (selection == null) {
			return msg("wrongSel", true);
		}
		parkourGame.getWalls().add(new AbstractRegion(new CuboidRegion(selection.getMaximumPoint(), selection.getMinimumPoint())));
		return msg("setWall", false).replace("%WALL_ID%", String.valueOf(parkourGame.getWalls().size()));
	}

	@Fallback
	public String addWall() {
		return msg("noGame", true);
	}

	@Argument(permission = "ats.parkour.setWall", description = "Sets specified parkour game wall", executorType = ExecutorType.PLAYER, aliases = "sw")
	public String setWall(@Param("parkourGame") final ParkourGame parkourGame, final int id) {
		final CuboidRegion selection = getRegionSelection((Player) sender);
		if (selection == null) {
			return msg("wrongSel", true);
		}
		if (id <= 1) {
			return msg("negativeNumber", true);
		}
		if (id > parkourGame.getLastCheckpointId()) {
			return msg("tooLargeNumber", true);
		}
		parkourGame.getWalls().set(id - 1, new AbstractRegion(selection));
		return msg("setWall", false).replace("%WALL_ID%", String.valueOf(id));
	}

	@Fallback
	public String setWall() {
		return msg("noGame", true);
	}

	@Argument(permission = "ats.parkour.effects", description = "Shows current parkour effects")
	public String effects(@Param("parkourGame") final ParkourGame parkourGame) {
		final List<String> effects = parkourGame.getOptions().getEffects().entrySet()
				.stream()
				.map(entry -> msg("oneEffect", false).replace("%EFFECT%", entry.getKey().getName()).replace("%AMPLIFIER%", String.valueOf(entry.getValue())))
				.collect(Collectors.toList());
		if (effects.isEmpty()) {
			return null;
		}
		return String.join("\n", effects);
	}

	@Fallback
	public String effects() {
		return msg("noGame", true);
	}

	@Argument(permission = "ats.parkour.setEffect", description = "Sets parkour game effect", aliases = "se")
	public String setEffect(@Param("parkourGame") final ParkourGame parkourGame, @Param("potion") final PotionEffectType type, final int power) {
		if (power == 0) {
			parkourGame.getOptions().removeEffect(type);
			return msg("effectRemoved", false).replace("%EFFECT%", type.getName());
		}
		parkourGame.getOptions().setEffect(type, power);
		return msg("setEffect", false).replace("%EFFECT%", type.getName()).replace("%AMPLIFIER%", String.valueOf(power));
	}

	@Fallback
	public String setEffect() {
		return msg("noGame", true);
	}

	// === OPTIONS ===

	@Argument(permission = "ats.parkour.sprintForced", description = "Shows value of sprintForced flag")
	public String sprintForced(@Param("parkourGame") final ParkourGame parkourGame) {
		return msg("currentSprintForced", false).replace("%SPRINT_FORCED%", String.valueOf(parkourGame.getOptions().isSprintForced()));
	}

	@Argument(permission = "ats.parkour.sprintForced", description = "Sets value of sprintForced flag")
	public String sprintForced(@Param("parkourGame") final ParkourGame parkourGame, final boolean sprintForced) {
		parkourGame.getOptions().setSprintForced(sprintForced);
		return msg("setSprintForced", false).replace("%SPRINT_FORCED%", String.valueOf(sprintForced));
	}

	@Fallback
	public String sprintForced() {
		return msg("noGame", true);
	}

	@Argument(permission = "ats.parkour.alwaysSpawn", description = "Shows value of alwaysSpawn flag")
	public String alwaysSpawn(@Param("parkourGame") final ParkourGame parkourGame) {
		return msg("currentAlwaysSpawn", false).replace("%ALWAYS_SPAWN%", String.valueOf(parkourGame.getOptions().isAlwaysSpawn()));
	}

	@Argument(permission = "ats.parkour.alwaysSpawn", description = "Sets value of alwaysSpawn flag")
	public String alwaysSpawn(@Param("parkourGame") final ParkourGame parkourGame, final boolean alwaysSpawn) {
		parkourGame.getOptions().setAlwaysSpawn(alwaysSpawn);
		return msg("setAlwaysSpawn", false).replace("%ALWAYS_SPAWN%", String.valueOf(alwaysSpawn));
	}

	@Fallback
	public String alwaysSpawn() {
		return msg("noGame", true);
	}

	@Argument(permission = "ats.parkour.recordCounting", description = "Shows value of recordCounting flag")
	public String recordCounting(@Param("parkourGame") final ParkourGame parkourGame) {
		return msg("currentRecordCounting", false).replace("%RECORD_COUNTING%", String.valueOf(parkourGame.getOptions().isRecordsCounting()));
	}

	@Argument(permission = "ats.parkour.recordCounting", description = "Sets value of recordCounting flag")
	public String recordCounting(@Param("parkourGame") final ParkourGame parkourGame, final boolean recordCounting) {
		parkourGame.getOptions().setRecordsCounting(recordCounting);
		return msg("setRecordCounting", false).replace("%RECORD_COUNTING%", String.valueOf(recordCounting));
	}

	@Fallback
	public String recordCounting() {
		return msg("noGame", true);
	}

	@Argument(permission = "ats.parkour.vipOnly", description = "Shows value of vipOnly flag")
	public String vipOnly(@Param("parkourGame") final ParkourGame parkourGame) {
		return msg("currentVipOnly", false).replace("%VIP_ONLY%", String.valueOf(parkourGame.getOptions().isVipOnly()));
	}

	@Argument(permission = "ats.parkour.vipOnly", description = "Sets value of vipOnly flag")
	public String vip(@Param("parkourGame") final ParkourGame parkourGame, final boolean vip) {
		parkourGame.getOptions().setVipOnly(vip);
		return msg("setVip", false).replace("%VIP%", String.valueOf(vip));
	}

	@Fallback
	public String vip() {
		return msg("noGame", true);
	}

	@Argument(permission = "ats.parkour.damageAllowed", description = "Shows value of damageAllowed flag")
	public String damageAllowed(@Param("parkourGame") final ParkourGame parkourGame) {
		return msg("currentDamageAllowed", false).replace("%DAMAGE_ALLOWED%", String.valueOf(parkourGame.getOptions().isDamageAllowed()));
	}

	@Argument(permission = "ats.parkour.damageAllowed", description = "Sets value of damageAllowed flag")
	public String damageAllowed(@Param("parkourGame") final ParkourGame parkourGame, final boolean damageAllowed) {
		parkourGame.getOptions().setDamageAllowed(damageAllowed);
		return msg("setDamageAllowed", false).replace("%DAMAGE_ALLOWED%", String.valueOf(damageAllowed));
	}

	@Fallback
	public String damageAllowed() {
		return msg("noGame", true);
	}

	@Argument(permission = "ats.parkour.available", description = "Shows value of available flag")
	public String available(@Param("parkourGame") final ParkourGame parkourGame) {
		return msg("currentAvailable", false).replace("%AVAILABLE%", String.valueOf(parkourGame.getOptions().isAvailable()));
	}

	@Argument(permission = "ats.parkour.available", description = "Sets value of available flag")
	public String available(@Param("parkourGame") final ParkourGame parkourGame, final boolean available) {
		parkourGame.getOptions().setAvailable(available);
		return msg("setAvailable", false).replace("%AVAILABLE%", String.valueOf(available));
	}

	@Fallback
	public String available() {
		return msg("noGame", true);
	}

	@Argument(permission = "ats.parkour.boat", description = "Shows value of boat flag")
	public String boat(@Param("parkourGame") final ParkourGame parkourGame) {
		return msg("currentBoat", false).replace("%BOAT%", String.valueOf(parkourGame.getOptions().isBoat()));
	}

	@Argument(permission = "ats.parkour.boat", description = "Sets value of boat flag")
	public String boat(@Param("parkourGame") final ParkourGame parkourGame, final boolean boat) {
		parkourGame.getOptions().setBoat(boat);
		return msg("setBoat", false).replace("%BOAT%", String.valueOf(boat));
	}

	@Fallback
	public String boat() {
		return msg("noGame", true);
	}

	@Argument(permission = "ats.parkour.enabled", description = "Shows value of enabled flag")
	public String enabled(@Param("parkourGame") final ParkourGame parkourGame) {
		return msg("currentEnabled", false).replace("%ENABLED%", String.valueOf(parkourGame.getOptions().isEnabled()));
	}

	@Argument(permission = "ats.parkour.enabled", description = "Sets value of enabled flag")
	public String enabled(@Param("parkourGame") final ParkourGame parkourGame, final boolean enabled) {
		parkourGame.getOptions().setEnabled(enabled);
		return msg("setEnabled", false).replace("%ENABLED%", String.valueOf(enabled));
	}

	@Fallback
	public String enabled() {
		return msg("noGame", true);
	}

	@Argument(permission = "ats.parkour.modifyInventory", description = "Shows value of modifyInventory flag")
	public String modifyInventory(@Param("parkourGame") final ParkourGame parkourGame) {
		return msg("currentModifyInventory", false).replace("%MODIFY_INVENTORY%", String.valueOf(parkourGame.getOptions().isModifyInventory()));
	}

	@Argument(permission = "ats.parkour.modifyInventory", description = "Sets value of modifyInventory flag")
	public String modifyInventory(@Param("parkourGame") final ParkourGame parkourGame, final boolean modifyInventory) {
		parkourGame.getOptions().setModifyInventory(modifyInventory);
		return msg("setModifyInventory", false).replace("%MODIFY_INVENTORY%", String.valueOf(modifyInventory));
	}

	@Fallback
	public String modifyInventory() {
		return msg("noGame", true);
	}

	@Argument(permission = "ats.parkour.info", description = "Shows information about parkour game")
	public String info(@Param("parkourGame") final ParkourGame parkourGame) {
		return parkourGame.toString();
	}

	@Argument(permission = "ats.parkour.difficulty", description = "Shows parkour game difficulty")
	public String difficulty(@Param("parkourGame") final ParkourGame parkourGame) {
		return msg("currentDifficulty", false).replace("%DIFFICULTY%", String.valueOf(parkourGame.getOptions().getDifficulty()));
	}

	@Argument(permission = "ats.parkour.difficulty", description = "Sets parkour game difficulty")
	public String difficulty(@Param("parkourGame") final ParkourGame parkourGame, final int difficulty) {
		parkourGame.getOptions().setDifficulty(difficulty);
		return msg("setDifficulty", false).replace("%DIFFICULTY%", String.valueOf(difficulty));
	}

	@Fallback
	public String difficulty() {
		return msg("noGame", true);
	}

	@Argument(permission = "ats.parkour.displayName", description = "Shows parkour game displayName")
	public String displayName(@Param("parkourGame") final ParkourGame parkourGame) {
		final String tmp = msg("currentDisplayName", false).split("%")[0];
		final String color = "\u00A7" + tmp.charAt(tmp.lastIndexOf('\u00A7') + 1);
		return msg("currentDisplayName", false)
				.replace("%DISPLAY_NAME%", "\u00A7r" + parkourGame.getDisplayName().replace('&', '\u00A7') + color);
	}

	@Argument(permission = "ats.parkour.displayName", description = "Sets parkour game displayName")
	public String displayName(@Param("parkourGame") final ParkourGame parkourGame, final String... newName) {
		final String name = String.join(" ", newName).replace('&', '\u00A7');
		final String tmp = msg("setDisplayName", false).split("%")[0];
		final String color = "\u00A7" + tmp.charAt(tmp.lastIndexOf('\u00A7') + 1);
		parkourGame.setDisplayName(name);
		return msg("setDisplayName", false).replace("%DISPLAY_NAME%", name + color);
	}

	@Fallback
	public String displayName() {
		return msg("noGame", true);
	}

	@Argument(permission = "ats.parkour.color", description = "Shows value of parkour color")
	public String color(@Param("parkourGame") final ParkourGame parkourGame) {
		return msg("currentColor", false).replace("%COLOR%", parkourGame.getOptions().getColor().name());
	}

	@Argument(permission = "ats.parkour.color", description = "Sets value of parkour color")
	public String color(@Param("parkourGame") final ParkourGame parkourGame, final DyeColor color) {
		parkourGame.getOptions().setColor(color);
		return msg("setColor", false).replace("%COLOR%", color.name());
	}

	@Fallback
	public String color() {
		return msg("noGame", true);
	}

	@Argument(permission = "ats.parkour.authors", description = "Shows parkour authors")
	public String authors(@Param("parkourGame") final ParkourGame parkourGame) {
		return msg("currentAuthors", false).replace("%AUTHORS%", parkourGame.getAuthors().toString());
	}

	@Argument(permission = "ats.parkour.authors", description = "Sets parkour authors")
	public String authors(@Param("parkourGame") final ParkourGame parkourGame, final String... authors) {
		parkourGame.getAuthors().clear();
		parkourGame.getAuthors().addAll(Arrays.asList(authors));
		return msg("setAuthors", false).replace("%AUTHORS%", Arrays.toString(authors));
	}

	@Fallback
	public String authors() {
		return msg("noGame", true);
	}

	@Argument(permission = "ats.parkour.type", description = "Shows parkour type")
	public String type(@Param("parkourGame") final ParkourGame parkourGame) {
		return msg("currentType", false).replace("%TYPE%", parkourGame.getOptions().getType().name());
	}

	@Argument(permission = "ats.parkour.type", description = "Sets parkour type")
	public String type(@Param("parkourGame") final ParkourGame parkourGame, final ParkourGame.ParkourType type) {
		parkourGame.getOptions().setType(type);
		return msg("setType", false).replace("%TYPE%", type.name());
	}

	@Fallback
	public String type() {
		return msg("noGame", true);
	}

	@Argument(permission = "ats.parkour.teleportBlock", description = "Sets parkour teleportBlock")
	public String teleportBlock(@Param("parkourGame") final ParkourGame parkourGame) {
		parkourGame.setTeleportBlock(((Player) sender).getTargetBlock(null, 5).getLocation());
		return msg("setTeleportBlock", false).replace("%PARKOUR%", parkourGame.getName());
	}

	@Fallback
	public String teleportBlock() {
		return msg("noGame", true);
	}

	@Argument(permission = "ats.parkour.top", description = "Shows top players for parkour game")
	public void top(@Param("parkourGame") final ParkourGame parkourGame, final int count) {
		plugin.getConnection()
				.map(x -> new TopPlayersDataOperations(x, parkourGame, count, result -> {
					int i = 1;
					for (final Entry<String, Float> entry : result.entrySet()) {
						sender.sendMessage(msg("topRecord", false)
								.replace("%NUMBER%", "" + i++)
								.replace("%PLAYER%", entry.getKey())
								.replace("%TIME%", String.valueOf(entry.getValue())));
					}
				}))
				.ifPresent(topPlayersDataOperations -> plugin.getServer().getScheduler().runTaskAsynchronously(plugin, topPlayersDataOperations));
	}

	@Fallback
	public String top() {
		return msg("noGame", true);
	}

	@Argument(permission = "ats.parkour.bestRecord", description = "Sets bestRecord sign location")
	public String bestRecord(@Param("parkourGame") final ParkourGame parkourGame) {
		final Location loc = ((Player) sender).getTargetBlock(null, 5).getLocation();
		final Material m = loc.getBlock().getType();
		if (!Data.SIGNS.contains(m)) {
			return msg("noSign", true);
		}
		parkourGame.setRecordsBlock(loc);
		sender.sendMessage(msg("setBestRecord", false).replace("%COORD_X%", "" + loc.getX()).replace("%COORD_Y%", "" + loc.getY()).replace("%COORD_Z%", "" + loc.getZ()));
		plugin.getConnection().ifPresent(connection -> {
			try (final PreparedStatement stat = connection.prepareStatement("SELECT `time`, `nick` FROM ats_parkour_records WHERE parkour=? ORDER BY time LIMIT 1")) {
				stat.setString(1, parkourGame.getName());
				final ResultSet rs = stat.executeQuery();
				if (rs.next()) {
					plugin.updateSign(rs.getString("nick"), rs.getDouble("time"), parkourGame);
				} else {
					plugin.updateSign("========", 0.00, parkourGame);
				}
			} catch (final SQLException ex) {
				ex.printStackTrace();
			}
		});
		return null;
	}

	@Fallback
	public String bestRecord() {
		return msg("noGame", true);
	}

	// === MEDALS ===

	@Argument(permission = "ats.parkour.bronze", description = "Shows bronze medal time")
	public String bronze(@Param("parkourGame") final ParkourGame parkourGame) {
		return msg("currentBronze", false).replace("%BRONZE%", String.valueOf(parkourGame.getOptions().getBronze()));
	}

	@Argument(permission = "ats.parkour.bronze", description = "Sets bronze medal time")
	public String bronze(@Param("parkourGame") final ParkourGame parkourGame, final double bronze) {
		parkourGame.getOptions().setBronze(bronze);
		return msg("setBronze", false).replace("%BRONZE%", String.valueOf(bronze));
	}

	@Fallback
	public String bronze() {
		return msg("noGame", true);
	}

	@Argument(permission = "ats.parkour.silver", description = "Shows silver medal time")
	public String silver(@Param("parkourGame") final ParkourGame parkourGame) {
		return msg("currentSilver", false).replace("%SILVER%", String.valueOf(parkourGame.getOptions().getSilver()));
	}

	@Argument(permission = "ats.parkour.silver", description = "Sets silver medal time")
	public String silver(@Param("parkourGame") final ParkourGame parkourGame, final double silver) {
		parkourGame.getOptions().setSilver(silver);
		return msg("setSilver", false).replace("%SILVER%", String.valueOf(silver));
	}

	@Fallback
	public String silver() {
		return msg("noGame", true);
	}

	@Argument(permission = "ats.parkour.gold", description = "Shows gold medal time")
	public String gold(@Param("parkourGame") final ParkourGame parkourGame) {
		return msg("currentGold", false).replace("%GOLD%", String.valueOf(parkourGame.getOptions().getGold()));
	}

	@Argument(permission = "ats.parkour.gold", description = "Sets gold medal time")
	public String gold(@Param("parkourGame") final ParkourGame parkourGame, final double gold) {
		parkourGame.getOptions().setGold(gold);
		return msg("setGold", false).replace("%GOLD%", String.valueOf(gold));
	}

	@Fallback
	public String gold() {
		return msg("noGame", true);
	}

	@Argument(permission = "ats.parkour.platinum", description = "Shows platinum medal time")
	public String platinum(@Param("parkourGame") final ParkourGame parkourGame) {
		return msg("currentPlatinum", false).replace("%PLATINUM%", String.valueOf(parkourGame.getOptions().getPlatinum()));
	}

	@Argument(permission = "ats.parkour.platinum", description = "Sets platinum medal time")
	public String platinum(@Param("parkourGame") final ParkourGame parkourGame, final double platinum) {
		parkourGame.getOptions().setPlatinum(platinum);
		return msg("setPlatinum", false).replace("%PLATINUM%", String.valueOf(platinum));
	}

	@Fallback
	public String platinum() {
		return msg("noGame", true);
	}

	// === UTILITIES ===

	private CuboidRegion getRegionSelection(final Player player) {
		final LocalSession session = plugin.getWorldEdit().getSession(player);
		try {
			final Region sel = session.getSelection(new BukkitWorld(((Player) sender).getWorld()));
			if (sel == null) {
				return null;
			}
			final AbstractWorld abstractWorld = new BukkitWorld(((Player) sender).getWorld());
			return new CuboidRegion(abstractWorld, sel.getMaximumPoint(), sel.getMinimumPoint());
		} catch (final IncompleteRegionException e) {
			// Do nothing
		}
		return null;
	}

	private String msg(final String arg, final boolean error) {
		return plugin.msg(arg, error);
	}
}
