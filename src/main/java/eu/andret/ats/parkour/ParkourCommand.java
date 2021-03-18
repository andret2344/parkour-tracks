/*
 * Copyright Andret (c) 2019. Copying and modifying allowed only keeping git link reference.
 */
package eu.andret.ats.parkour;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.AbstractWorld;
import eu.andret.arguments.AnnotatedCommandExecutor;
import eu.andret.arguments.api.annotation.Argument;
import eu.andret.arguments.api.annotation.BaseCommand;
import eu.andret.arguments.api.annotation.Fallback;
import eu.andret.arguments.api.annotation.Param;
import eu.andret.arguments.api.entity.ExecutorType;
import eu.andret.ats.parkour.parkour.Parkour;
import eu.andret.ats.parkour.parkour.ParkourGame;
import eu.andret.ats.parkour.parkour.ParkourManager;
import eu.andret.ats.parkour.player.ParkourPlayer;
import eu.andret.ats.parkour.player.PlayerManager;
import eu.andret.ats.parkour.region.Checkpoint;
import eu.andret.ats.parkour.region.GameRegion;
import eu.andret.ats.parkour.region.Wall;
import eu.andret.ats.parkour.tasks.DataBaseOperations;
import eu.andret.ats.parkour.tasks.RepairSignTask;
import eu.andret.ats.parkour.tasks.TopPlayersDataOperations;
import eu.andret.ats.parkour.util.Data;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

@BaseCommand("parkour")
@Value
@EqualsAndHashCode(callSuper = true)
public class ParkourCommand extends AnnotatedCommandExecutor<ParkourPlugin> {
	WorldEditPlugin wep;

	public ParkourCommand(final CommandSender sender, final ParkourPlugin plugin) {
		super(sender, plugin);
		wep = (WorldEditPlugin) plugin.getServer().getPluginManager().getPlugin("WorldEdit");
	}

	@Argument(executorType = ExecutorType.PLAYER, permission = "ats.parkour.create", aliases = {"c"})
	public String create(final String name) {
		if (ParkourManager.getLobbyLocation() == null) {
			return msg("lobbyFirst", true);
		}
		final CuboidRegion cr = getFromSelection((Player) sender);
		if (cr == null) {
			return msg("wrongSel", true);
		}
		if (!name.matches("[a-zA-Z0-9_-]+")) {
			return msg("wrongName", true);
		}
		final ParkourGame parkour = ParkourManager.getParkour(name);
		if (parkour != null) {
			return msg("gameExists", true).replace("%GAMENAME%", name);
		}
		final Parkour parkourGame = new Parkour(name, new GameRegion(cr), ((Player) sender).getLocation().getWorld());
		ParkourManager.addParkour(parkourGame);
		return msg("gameCreated", false).replace("%GAMENAME%", parkourGame.getName());
	}

	@Argument(permission = "ats.parkour.remove", aliases = {"r"})
	public String remove(@Param("parkourGame") final ParkourGame parkourGame) {
		ParkourManager.removeParkour(parkourGame);
		return msg("gameRemoved", false).replace("%GAMENAME%", parkourGame.getName());
	}

	@Fallback
	public String remove() {
		return msg("noGame", true);
	}

	@Argument(executorType = ExecutorType.PLAYER, permission = "ats.parkour.SetSpawn", aliases = {"ss"})
	public String setSpawn(@Param("parkourGame") final ParkourGame parkourGame) {
		final CuboidRegion selection = getFromSelection((Player) sender);
		if (selection == null) {
			return msg("wrongSel", true);
		}
		final Location location = ((Player) sender).getLocation();
		parkourGame.SetSpawn(new Checkpoint(selection, location.getYaw(), location.getPitch()));
		return msg("SetSpawn", false);
	}

	@Fallback
	public String SetSpawn() {
		return msg("noGame", true);
	}

	@Argument
	public String addCheckpoint(@Param("parkourGame") final ParkourGame parkourGame) {
		final CuboidRegion selection = getFromSelection((Player) sender);
		if (selection == null) {
			return msg("wrongSel", true);
		}
		if (parkourGame.getSpawn() == null) {
			return msg("noSpawn", true);
		}
		final Location l = ((Player) sender).getLocation();
		parkourGame.addCheckpoint(new Checkpoint(selection, l.getYaw(), l.getPitch()));
		return msg("setCheckpoint", false).replace("%CHECKPOINTID%", String.valueOf(parkourGame.getCheckpointList().size() - 1));
	}

	@Fallback
	public String addCheckpoint() {
		return msg("noGame", true);
	}

	@Argument
	public String setCheckpoint(final int id, final ParkourGame parkourGame) {
		final CuboidRegion selection = getFromSelection((Player) sender);
		if (selection == null) {
			return msg("wrongSel", true);
		}
		if (id <= 0) {
			return msg("negativeNumber", true);
		}
		final Location location = ((Player) sender).getLocation();
		parkourGame.setCheckpoint(id, new Checkpoint(selection, location.getYaw(), location.getPitch()));
		return msg("setCheckpoint", false).replace("%CHECKPOINTID%", String.valueOf(id));
	}

	@Fallback
	public String setCheckpoint() {
		return msg("noGame", true);
	}

	@Argument
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
		parkourGame.start();
		return msg("gameStarted", false).replace("%GAMENAME%", parkourGame.getName());
	}

	@Argument
	public String stop(@Param("parkourGame") final ParkourGame parkourGame) {
		if (!parkourGame.isRunning()) {
			return msg("alreadyStopped", true);
		}
		parkourGame.stop();
		return msg("gameStopped", false).replace("%GAMENAME%", parkourGame.getName());
	}

	@Argument
	public String recreate(@Param("parkourGame") final ParkourGame parkourGame) {
		final CuboidRegion cr = getFromSelection((Player) sender);
		if (cr == null) {
			return msg("wrongSel", true);
		}
		parkourGame.getGameRegion().setRegion(cr);
		return msg("gameRecreated", false);
	}

	@Argument
	public String fix() {
		ParkourManager.getAllGames()
				.stream()
				.map(parkourGame -> new RepairSignTask(plugin, parkourGame))
				.forEach(repairSignTask -> Bukkit.getScheduler().runTaskAsynchronously(plugin, repairSignTask));
		return msg("successFix", false);
	}

	@Argument
	public String ignore() {
		final Player pl = (Player) sender;
		final ParkourPlayer parkourPlayer = PlayerManager.getParkourSinglePlayer(pl);
		if (parkourPlayer.isIgnoring()) {
			parkourPlayer.setIgnoring(false);
			ParkourManager.getAllGames().stream()
					.filter(p -> p.inAnyRegion(pl.getLocation()))
					.forEach(p -> p.addPlayer(pl));
			return msg("ignoreStop", false);
		}
		parkourPlayer.setIgnoring(true);
		final ParkourGame parkour = ParkourManager.getParkour(pl);
		if (parkour != null) {
			parkour.removePlayer(pl);
		}
		return msg("ignoreStart", false);
	}

	@Argument
	public String spectator() {
		final Player player = (Player) sender;
		final ParkourPlayer parkourPlayer = PlayerManager.getParkourSinglePlayer(player);
		if (parkourPlayer.isSpectating()) {
			parkourPlayer.setSpectating(false);
			Bukkit.getOnlinePlayers().forEach(p -> p.showPlayer(plugin, player));
			ParkourManager.getAllGames()
					.stream().filter(p -> p.inAnyRegion(player.getLocation()))
					.forEach(p -> p.addPlayer(player));
			return msg("spectatorStop", false);
		} else {
			parkourPlayer.setSpectating(true);
			Bukkit.getOnlinePlayers().forEach(p -> p.hidePlayer(plugin, player));
			final ParkourGame parkour = ParkourManager.getParkour(player);
			if (parkour != null) {
				parkour.removePlayer(player);
			}
			return msg("spectatorStart", false);
		}
	}

	@Argument
	public String list() {
		if (ParkourManager.getAllGames().isEmpty()) {
			return msg("emptyList", false);
		}
		final StringBuilder builder = new StringBuilder();
		builder.append(msg("listHeader", false).replace("%COUNT%", String.valueOf(ParkourManager.getAllGames().size())));
		final List<ParkourGame> allGames = ParkourManager.getAllGames();
		for (int i = 1; i <= allGames.size(); i++) {
			final ParkourGame parkourGame = allGames.get(i - 1);
			final String on;
			if (parkourGame.isRunning()) {
				on = ChatColor.GREEN + "" + ChatColor.ITALIC + "[Started]";
			} else {
				on = ChatColor.RED + "" + ChatColor.ITALIC + "[Stopped]";
			}
			builder.append(i).append(". ").append(parkourGame.getName()).append(" ").append(on).append("\n");
		}
		return builder.toString();
	}

	@Argument
	public String addWall(@Param("parkourGame") final ParkourGame parkourGame) {
		final CuboidRegion selection = getFromSelection((Player) sender);
		if (selection == null) {
			sender.sendMessage(msg("wrongSel", true));
		}
		parkourGame.addWall(new Wall(new CuboidRegion(selection.getMaximumPoint(), selection.getMinimumPoint())));
		return msg("SetWall", false).replace("%WALLID%", String.valueOf(parkourGame.getWallList().size()));
	}

	@Argument
	public String setWall(final int id, @Param("parkourGame") final ParkourGame parkourGame) {
		final CuboidRegion selection = getFromSelection((Player) sender);
		if (selection == null) {
			return msg("wrongSel", true);
		}
		if (id <= 1) {
			return msg("negativeNumber", true);
		}
		parkourGame.SetWall(id - 1, selection);
		return msg("SetWall", false).replace("%WALLID%", String.valueOf(id));
	}

	@Argument
	public String effects(final int id, @Param("parkourGame") final ParkourGame parkourGame) {
		return parkourGame.getOptions().getEffects().entrySet()
				.stream()
				.map(entry -> msg("oneEffect", false).replace("%EFFECT%", entry.getKey().getName()).replace("%AMPLIFIER%", String.valueOf(entry.getValue())))
				.collect(Collectors.joining("\n"));
	}

	@Argument
	public String setEffect(@Param("parkourGame") final ParkourGame parkourGame, final PotionEffectType type, final int power) {
		if (power == 0) {
			parkourGame.getOptions().removeEffect(type);
			return msg("effectRemoved", false).replace("%EFFECT%", type.getName());
		}
		parkourGame.getOptions().setEffect(type, power);
		return msg("setEffect", false).replace("%EFFECT%", type.getName()).replace("%AMPLIFIER%", String.valueOf(power));
	}

	@Argument
	public String lobby() {
		final Location l = ((Player) sender).getLocation();
		ParkourManager.setLobbyLocation(l);
		return msg("setLobby", false)
				.replace("%COORDX%", String.valueOf(l.getX()))
				.replace("%COORDY%", String.valueOf(l.getY()))
				.replace("%COORDZ%", String.valueOf(l.getZ()));
	}

	@Argument
	public String rename(@Param("parkourGame") final ParkourGame parkourGame, final String name) {
		if (ParkourManager.getParkour(name) == null) {
			return msg("gameExists", true);
		}
		parkourGame.setName(name);
		return msg("gameRenamed", false).replace("%OLDNAME%", parkourGame.getName()).replace("%NEWNAME%", name);
	}

	@Argument
	public void teleport(@Param("parkourGame") final ParkourGame parkourGame) {
		parkourGame.addPlayer((Player) sender);
		final ParkourPlayer p = PlayerManager.getParkourSinglePlayer((Player) sender);
		p.teleportToSpawn();
		p.reset();
	}

	@Argument
	public String sprintForced(@Param("parkourGame") final ParkourGame parkourGame) {
		return msg("currentSprint", false).replace("%SPRINT%", String.valueOf(parkourGame.getOptions().isForcingSprint()));
	}

	@Argument
	public String sprintForced(@Param("parkourGame") final ParkourGame parkourGame, final boolean sprintForced) {
		parkourGame.getOptions().setForcingSprint(sprintForced);
		return msg("setSprint", false).replace("%SPRINT%", String.valueOf(sprintForced));
	}

	@Argument
	public String alwaysSpawn(@Param("parkourGame") final ParkourGame parkourGame) {
		return msg("currentAlwaysSpawn", false).replace("%ALWAYSSPAWN%", String.valueOf(parkourGame.getOptions().isAlwaysSpawn()));
	}

	@Argument
	public String alwaysSpawn(@Param("parkourGame") final ParkourGame parkourGame, final boolean alwaysSpawn) {
		parkourGame.getOptions().setAlwaysSpawn(alwaysSpawn);
		return msg("setAlwaysSpawn", false).replace("%ALWAYSSPAWN%", String.valueOf(alwaysSpawn));
	}

	@Argument
	public String recordCounting(@Param("parkourGame") final ParkourGame parkourGame) {
		return msg("currentRecordCounting", false).replace("%RECORDCOUNTING%", String.valueOf(parkourGame.getOptions().isRecordsCounting()));
	}

	@Argument
	public String recordCounting(@Param("parkourGame") final ParkourGame parkourGame, final boolean recordCounting) {
		parkourGame.getOptions().setRecordsCounting(recordCounting);
		return msg("setRecordCounting", false).replace("%RECORDCOUNTING%", String.valueOf(recordCounting));
	}

	@Argument
	public String vip(@Param("parkourGame") final ParkourGame parkourGame) {
		return msg("currentVip", false).replace("%VIP%", String.valueOf(parkourGame.getOptions().isVip()));
	}

	@Argument
	public String vip(@Param("parkourGame") final ParkourGame parkourGame, final boolean vip) {
		parkourGame.getOptions().setVip(vip);
		return msg("setVip", false).replace("%VIP%", String.valueOf(vip));
	}

	@Argument
	public String damageAllowed(@Param("parkourGame") final ParkourGame parkourGame) {
		return msg("currentDamageAllowed", false).replace("%DAMAGEALLOWED%", String.valueOf(parkourGame.getOptions().isDamageAllowed()));
	}

	@Argument
	public String damageAllowed(@Param("parkourGame") final ParkourGame parkourGame, final boolean damageAllowed) {
		parkourGame.getOptions().setDamageAllowed(damageAllowed);
		return msg("setDamageAllowed", false).replace("%DAMAGEALLOWED%", String.valueOf(damageAllowed));
	}

	@Argument
	public String available(@Param("parkourGame") final ParkourGame parkourGame) {
		return msg("currentAvailable", false).replace("%AVAILABLE%", String.valueOf(parkourGame.getOptions().isAvailable()));
	}

	@Argument
	public String available(@Param("parkourGame") final ParkourGame parkourGame, final boolean available) {
		parkourGame.getOptions().setAvailable(available);
		return msg("setAvailable", false).replace("%AVAILABLE%", String.valueOf(available));
	}

	@Argument
	public String boat(@Param("parkourGame") final ParkourGame parkourGame) {
		return msg("currentBoat", false).replace("%BOAT%", String.valueOf(parkourGame.getOptions().isBoat()));
	}

	@Argument
	public String boat(@Param("parkourGame") final ParkourGame parkourGame, final boolean boat) {
		parkourGame.getOptions().setBoat(boat);
		return msg("setBoat", false).replace("%BOAT%", String.valueOf(boat));
	}

	@Argument
	public String enabled(@Param("parkourGame") final ParkourGame parkourGame) {
		return msg("currentEnabled", false).replace("%ENABLED%", String.valueOf(parkourGame.getOptions().isEnabled()));
	}

	@Argument
	public String enabled(@Param("parkourGame") final ParkourGame parkourGame, final boolean enabled) {
		parkourGame.getOptions().setEnabled(enabled);
		return msg("setEnabled", false).replace("%ENABLED%", String.valueOf(enabled));
	}

	@Argument
	public String modifyInventory(@Param("parkourGame") final ParkourGame parkourGame) {
		return msg("currentModifyInventory", false).replace("%MODIFYINVENTORY%", String.valueOf(parkourGame.getOptions().isModifyInventory()));
	}

	@Argument
	public String modifyInventory(@Param("parkourGame") final ParkourGame parkourGame, final boolean modifyInventory) {
		parkourGame.getOptions().setModifyInventory(modifyInventory);
		return msg("setModifyInventory", false).replace("%MODIFYINVENTORY%", String.valueOf(modifyInventory));
	}

	@Argument
	public String info(@Param("parkourGame") final ParkourGame parkourGame) {
		return parkourGame.toString();
	}

	@Argument
	public String difficulty(@Param("parkourGame") final ParkourGame parkourGame) {
		return msg("currentDifficulty", false).replace("%DIFFICULTY%", String.valueOf(parkourGame.getOptions().getDifficulty()));
	}

	@Argument
	public String difficulty(@Param("parkourGame") final ParkourGame parkourGame, final int difficulty) {
		parkourGame.getOptions().setDifficulty(difficulty);
		return msg("setDifficulty", false).replace("%DIFFICULTY%", String.valueOf(difficulty));
	}

	@Argument
	public String displayName(@Param("parkourGame") final ParkourGame parkourGame) {
		final String tmp = msg("currentDisplayName", false).split("%")[0];
		final String color = "\u00A7" + tmp.charAt(tmp.lastIndexOf('\u00A7') + 1);
		return msg("currentDisplayName", false)
				.replace("%DISPLAYNAME%", "\u00A7r" + parkourGame.getOptions().getDisplayName().replace('&', '\u00A7') + color);
	}

	@Argument
	public String displayName(@Param("parkourGame") final ParkourGame parkourGame, final String... newName) {
		final String name = String.join(" ", newName).replace('&', '\u00A7');
		final String tmp = msg("setDisplayName", false).split("%")[0];
		final String color = "\u00A7" + tmp.charAt(tmp.lastIndexOf('\u00A7') + 1);
		parkourGame.getOptions().setDisplayName(name);
		return msg("setDisplayName", false).replace("%DISPLAYNAME%", name + color);
	}

	@Argument
	public String fair(@Param("parkourGame") final ParkourGame parkourGame) {
		return msg("currentFair", false).replace("%FAIR%", String.valueOf(parkourGame.getOptions().getFair()));
	}

	@Argument
	public String fair(@Param("parkourGame") final ParkourGame parkourGame, final double fair) {
		parkourGame.getOptions().setFair(fair);
		return msg("setFair", false).replace("%BRONZE%", String.valueOf(fair));
	}

	@Argument
	public String bronze(@Param("parkourGame") final ParkourGame parkourGame) {
		return msg("currentBronze", false).replace("%BRONZE%", String.valueOf(parkourGame.getOptions().getBronze()));
	}

	@Argument
	public String bronze(@Param("parkourGame") final ParkourGame parkourGame, final double bronze) {
		parkourGame.getOptions().setBronze(bronze);
		return msg("setBronze", false).replace("%BRONZE%", String.valueOf(bronze));
	}

	@Argument
	public String silver(@Param("parkourGame") final ParkourGame parkourGame) {
		return msg("currentSilver", false).replace("%SILVER%", String.valueOf(parkourGame.getOptions().getSilver()));
	}

	@Argument
	public String silver(@Param("parkourGame") final ParkourGame parkourGame, final double silver) {
		parkourGame.getOptions().setSilver(silver);
		return msg("setSilver", false).replace("%SILVER%", String.valueOf(silver));
	}

	@Argument
	public String gold(@Param("parkourGame") final ParkourGame parkourGame) {
		return msg("currentGold", false).replace("%GOLD%", String.valueOf(parkourGame.getOptions().getGold()));
	}

	@Argument
	public String gold(@Param("parkourGame") final ParkourGame parkourGame, final double gold) {
		parkourGame.getOptions().setGold(gold);
		return msg("setGold", false).replace("%GOLD%", String.valueOf(gold));
	}

	@Argument
	public String platinum(@Param("parkourGame") final ParkourGame parkourGame) {
		return msg("currentPlatinum", false).replace("%PLATINUM%", String.valueOf(parkourGame.getOptions().getPlatinum()));
	}

	@Argument
	public String platinum(@Param("parkourGame") final ParkourGame parkourGame, final double platinum) {
		parkourGame.getOptions().setPlatinum(platinum);
		return msg("setPlatinum", false).replace("%PLATINUM%", String.valueOf(platinum));
	}

	@Argument
	public String color(@Param("parkourGame") final ParkourGame parkourGame) {
		return msg("currentColor", false).replace("%COLOR%", parkourGame.getOptions().getColor().name());
	}

	@Argument
	public String color(@Param("parkourGame") final ParkourGame parkourGame, final DyeColor color) {
		parkourGame.getOptions().setColor(color);
		return msg("setColor", false).replace("%COLOR%", color.name());
	}

	@Argument
	public String authors(@Param("parkourGame") final ParkourGame parkourGame) {
		return msg("currentAuthors", false).replace("%AUTHORS%", parkourGame.getAuthors().toString());
	}

	@Argument
	public String authors(@Param("parkourGame") final ParkourGame parkourGame, final String... authors) {
		parkourGame.setAuthors(Arrays.asList(authors));
		return msg("setColor", false).replace("%AUTHORS%", Arrays.toString(authors));
	}

	@Argument
	public String type(@Param("parkourGame") final ParkourGame parkourGame) {
		return msg("currentType", false).replace("%TYPE%", parkourGame.getOptions().getType().name());
	}

	@Argument
	public String type(@Param("parkourGame") final ParkourGame parkourGame, final ParkourGame.ParkourType type) {
		parkourGame.getOptions().setType(type);
		return msg("setType", false).replace("%TYPE%", type.name());
	}

	@Argument
	public String help() {
		return help(1);
	}

	@Argument
	public String help(final int page) {
		final Map<String, String> messages = plugin.getMessages();
		final int maxPages = messages.size() / 5 + 1;
		final int skip = 5 * (page - 1);
		return msg("currentPage", false)
				.replace("%PAGE%", String.valueOf(page))
				.replace("%MAXPAGES%", String.valueOf(maxPages)) +
				messages.entrySet()
						.stream()
						.skip(skip)
						.limit(5)
						.map(x -> "§2/parkourGame " + x.getKey() + "§r - " + x.getValue())
						.collect(Collectors.joining("\n"));
	}

	@Argument
	public String teleportBlock(@Param("parkourGame") final ParkourGame parkourGame) {
		parkourGame.setTeleportBlock(((Player) sender).getTargetBlock(null, 5).getLocation());
		return msg("setTeleportBlock", false).replace("%PARKOUR%", parkourGame.getName());
	}

	@Argument
	public void top(@Param("parkourGame") final ParkourGame parkourGame, final int i) {
		Bukkit.getScheduler().runTaskAsynchronously(plugin, new TopPlayersDataOperations(plugin, parkourGame, i, result -> {
			int j = 1;
			for (final Entry<String, Float> entry : result.getResult().entrySet()) {
				sender.sendMessage(msg("topRecord", false).replace("%NUMBER%", "" + j++).replace("%PLAYER%", entry.getKey()).replace("%TIME%", "" + entry.getValue()));
			}
		}));
	}

	@Argument
	public void bestRecord(@Param("parkourGame") final ParkourGame parkourGame) {
		final Location loc = ((Player) sender).getTargetBlock(null, 5).getLocation();
		final Material m = loc.getBlock().getType();
		if (Data.SIGNS.contains(m)) {
			parkourGame.setBestRecord(loc);
			sender.sendMessage(msg("setBestrecord", false).replace("%COORDX%", "" + loc.getX()).replace("%COORDY%", "" + loc.getY()).replace("%COORDZ%", "" + loc.getZ()));
			try {
				final PreparedStatement stat = plugin.getConnection().prepareStatement("SELECT `time`, `nick` FROM ats_parkour_records WHERE parkour=? ORDER BY time LIMIT 1");
				stat.setString(1, parkourGame.getName());
				final ResultSet rs = stat.executeQuery();
				if (rs.next()) {
					DataBaseOperations.updateSign(plugin, rs.getString("nick"), rs.getDouble("time"), parkourGame);
				} else {
					DataBaseOperations.updateSign(plugin, "========", 0.00, parkourGame);
				}
			} catch (final Exception ex) {
				Bukkit.getServer().getLogger().throwing(getClass().getName(), "onCommand", ex);
			}
		} else {
			sender.sendMessage(msg("noSign", true));
		}
	}

	private CuboidRegion getFromSelection(final Player player) {
		final LocalSession session = wep.getSession(player);
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
