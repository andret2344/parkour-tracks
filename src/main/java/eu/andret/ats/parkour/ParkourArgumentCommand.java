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
import lombok.SneakyThrows;
import lombok.Value;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

@BaseCommand("parkour")
@Value
@EqualsAndHashCode(callSuper = true)
public class ParkourArgumentCommand extends AnnotatedCommandExecutor<ParkourPlugin> {
	WorldEditPlugin wep;

	public ParkourArgumentCommand(final CommandSender sender, final ParkourPlugin plugin) {
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
		final Parkour pk = new Parkour(name, new GameRegion(cr), ((Player) sender).getLocation().getWorld());
		ParkourManager.addParkour(pk);
		return msg("gameCreated", false).replace("%GAMENAME%", pk.getName());
	}

	@Argument(permission = "ats.parkour.remove", aliases = {"r"})
	public String remove(@Param("parkour") final Parkour parkour) {
		ParkourManager.removeParkour(parkour);
		return msg("gameRemoved", false).replace("%GAMENAME%", parkour.getName());
	}

	@Fallback
	public String remove() {
		return msg("noGame", true);
	}

	@Argument(executorType = ExecutorType.PLAYER, permission = "ats.parkour.setspawn", aliases = {"ss"})
	public String setSpawn(@Param("parkourGame") final ParkourGame parkourGame) {
		final CuboidRegion selection = getFromSelection((Player) sender);
		if (selection == null) {
			return msg("wrongSel", true);
		}
		final Location location = ((Player) sender).getLocation();
		parkourGame.setSpawn(new Checkpoint(selection, location.getYaw(), location.getPitch()));
		return msg("setSpawn", false);
	}

	@Fallback
	public String setSpawn() {
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
	public String recreate(@Param("parkourGame") final ParkourGame pk) {
		final CuboidRegion cr = getFromSelection((Player) sender);
		if (cr == null) {
			return msg("wrongSel", true);
		}
		pk.getGameRegion().setRegion(cr);
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
			final ParkourGame pk = allGames.get(i - 1);
			final String on;
			if (pk.isRunning()) {
				on = ChatColor.GREEN + "" + ChatColor.ITALIC + "[Started]";
			} else {
				on = ChatColor.RED + "" + ChatColor.ITALIC + "[Stopped]";
			}
			builder.append(i).append(". ").append(pk.getName()).append(" ").append(on).append("\n");
		}
		return builder.toString();
	}

	@Argument
	public String addWall(@Param("parkourGame") final ParkourGame pk) {
		final CuboidRegion selection = getFromSelection((Player) sender);
		if (selection == null) {
			sender.sendMessage(msg("wrongSel", true));
		}
		pk.addWall(new Wall(new CuboidRegion(selection.getMaximumPoint(), selection.getMinimumPoint())));
		return msg("setWall", false).replace("%WALLID%", String.valueOf(pk.getWallList().size()));
	}

	@Argument
	public String setWall(final int id, @Param("parkourGame") final ParkourGame pk) {
		final CuboidRegion selection = getFromSelection((Player) sender);
		if (selection == null) {
			return msg("wrongSel", true);
		}
		if (id <= 1) {
			return msg("negativeNumber", true);
		}
		pk.setWall(id - 1, selection);
		return msg("setWall", false).replace("%WALLID%", String.valueOf(id));
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

	@SneakyThrows
	public boolean onCommand(@NotNull final CommandSender sender, @NotNull final Command cmd, @NotNull final String label, @NotNull final String[] args) {
		if (cmd.getName().equalsIgnoreCase("parkour")) {
			if (args.length == 0) {
				if (!(sender instanceof Player) || sender.hasPermission("ats.parkour.main")) {
					plugin.getMessages().entrySet().stream()
							.map(entry -> "§2/pk " + entry.getKey() + "§r - " + entry.getValue())
							.forEach(sender::sendMessage);
				} else {
					sender.sendMessage(msg("noPerms", true));
				}
			} else {
				if (args[0].equalsIgnoreCase("sprint") || args[0].equalsIgnoreCase("sp")) {
					if (!(sender instanceof Player) || sender.hasPermission("ats.parkour.sprint")) {
						if (args.length == 1) {
							sender.sendMessage(msg("usageSprint", true));
						} else if (args.length == 2) {
							final ParkourGame parkour = ParkourManager.getParkour(args[1]);
							if (parkour != null) {
								sender.sendMessage(msg("currentSprint", false).replace("%SPRINT%", String.valueOf(parkour.getOptions().isForcingSprint())));
							} else {
								sender.sendMessage(msg("noGame", true));
							}
						} else {
							final ParkourGame parkour = ParkourManager.getParkour(args[1]);
							if (parkour != null) {
								parkour.getOptions().setForcingSprint(Boolean.parseBoolean(args[2]));
								sender.sendMessage(msg("setSprint", false).replace("%SPRINT%", String.valueOf(Boolean.parseBoolean(args[2]))));
							} else {
								sender.sendMessage(msg("noGame", true));
							}
						}
					} else {
						sender.sendMessage(msg("noPerms", true));
					}
				} else if (args[0].equalsIgnoreCase("alwaysSpawn") || args[0].equalsIgnoreCase("as")) {
					if (!(sender instanceof Player) || sender.hasPermission("ats.parkour.alwaysSpawn")) {
						if (args.length == 1) {
							sender.sendMessage(msg("usageAlwaysspawn", true));
						} else if (args.length == 2) {
							final ParkourGame parkour = ParkourManager.getParkour(args[1]);
							if (parkour != null) {
								sender.sendMessage(msg("currentAlwaysspawn", false).replace("%ALWAYSSPAWN%", String.valueOf(parkour.getOptions().isAlwaysSpawn())));
							} else {
								sender.sendMessage(msg("noGame", true));
							}
						} else {
							final ParkourGame parkour = ParkourManager.getParkour(args[1]);
							if (parkour != null) {
								parkour.getOptions().setAlwaysSpawn(Boolean.parseBoolean(args[2]));
								sender.sendMessage(msg("setAlwaysSpawn", false).replace("%ALWAYSSPAWN%", String.valueOf(Boolean.parseBoolean(args[2]))));
							} else {
								sender.sendMessage(msg("noGame", true));
							}
						}
					} else {
						sender.sendMessage(msg("noPerms", true));
					}
				} else if (args[0].equalsIgnoreCase("price") || args[0].equalsIgnoreCase("p")) {
					if (!(sender instanceof Player) || sender.hasPermission("ats.parkour.price")) {
						if (args.length == 1) {
							sender.sendMessage(msg("usagePrice", true));
						} else if (args.length == 2) {
							final ParkourGame parkour = ParkourManager.getParkour(args[1]);
							if (parkour != null) {
								sender.sendMessage(msg("currentPrice", false).replace("%PRICE%", String.valueOf(parkour.getOptions().getPrice())));
							} else {
								sender.sendMessage(msg("noGame", true));
							}
						} else {
							final ParkourGame parkour = ParkourManager.getParkour(args[1]);
							if (parkour != null) {
								parkour.getOptions().setPrice(Integer.parseInt(args[2]));
								sender.sendMessage(msg("setPrice", false).replace("%PRICE%", String.valueOf(Integer.parseInt(args[2]))));
							} else {
								sender.sendMessage(msg("noGame", true));
							}
						}
					} else {
						sender.sendMessage(msg("noPerms", true));
					}
				} else if (args[0].equalsIgnoreCase("xp")) {
					if (!(sender instanceof Player) || sender.hasPermission("ats.parkour.xp")) {
						if (args.length == 1) {
							sender.sendMessage(msg("usageXp", true));
						} else if (args.length == 2) {
							final ParkourGame parkour = ParkourManager.getParkour(args[1]);
							if (parkour != null) {
								sender.sendMessage(msg("currentXp", false).replace("%XP%", String.valueOf(parkour.getOptions().getXp())));
							} else {
								sender.sendMessage(msg("noGame", true));
							}
						} else {
							final ParkourGame parkour = ParkourManager.getParkour(args[1]);
							if (parkour != null) {
								parkour.getOptions().setXp(Integer.parseInt(args[2]));
								sender.sendMessage(msg("setXp", false).replace("%XP%", String.valueOf(Integer.parseInt(args[2]))));
							} else {
								sender.sendMessage(msg("noGame", true));
							}
						}
					} else {
						sender.sendMessage(msg("noPerms", true));
					}
				} else if (args[0].equalsIgnoreCase("countingRecords") || args[0].equalsIgnoreCase("cr")) {
					if (!(sender instanceof Player) || sender.hasPermission("ats.parkour.countingRecords")) {
						if (args.length == 1) {
							sender.sendMessage(msg("usageCountrecords", true));
						} else if (args.length == 2) {
							final ParkourGame parkour = ParkourManager.getParkour(args[1]);
							if (parkour != null) {
								sender.sendMessage(msg("currentCountrecords", false).replace("%COUNTRECORDS%", String.valueOf(parkour.getOptions().isCountingRecords())));
							} else {
								sender.sendMessage(msg("noGame", true));
							}
						} else {
							final ParkourGame parkour = ParkourManager.getParkour(args[1]);
							if (parkour != null) {
								parkour.getOptions().setCountingRecords(Boolean.parseBoolean(args[2]));
								sender.sendMessage(msg("setCountingRecords", false).replace("%COUNTRECORDS%", String.valueOf(Boolean.parseBoolean(args[2]))));
							} else {
								sender.sendMessage(msg("noGame", true));
							}
						}
					} else {
						sender.sendMessage(msg("noPerms", true));
					}
				} else if (args[0].equalsIgnoreCase("vip") || args[0].equalsIgnoreCase("v")) {
					if (!(sender instanceof Player) || sender.hasPermission("ats.parkour.vip")) {
						if (args.length == 1) {
							sender.sendMessage(msg("usageVip", true));
						} else if (args.length == 2) {
							final ParkourGame parkour = ParkourManager.getParkour(args[1]);
							if (parkour != null) {
								sender.sendMessage(msg("currentVip", false).replace("%VIP%", String.valueOf(parkour.getOptions().isVip())));
							} else {
								sender.sendMessage(msg("noGame", true));
							}
						} else {
							final ParkourGame parkour = ParkourManager.getParkour(args[1]);
							if (parkour != null) {
								parkour.getOptions().setVip(Boolean.parseBoolean(args[2]));
								sender.sendMessage(msg("setVip", false).replace("%VIP%", String.valueOf(Boolean.parseBoolean(args[2]))));
							} else {
								sender.sendMessage(msg("noGame", true));
							}
						}
					} else {
						sender.sendMessage(msg("noPerms", true));
					}
				} else if (args[0].equalsIgnoreCase("allowingDamage") || args[0].equalsIgnoreCase("dmg")) {
					if (!(sender instanceof Player) || sender.hasPermission("ats.parkour.allowingDamage")) {
						if (args.length == 1) {
							sender.sendMessage(msg("usageDamage", true));
						} else if (args.length == 2) {
							final ParkourGame parkour = ParkourManager.getParkour(args[1]);
							if (parkour != null) {
								sender.sendMessage(msg("currentDamage", false).replace("%DAMAGE%", String.valueOf(parkour.getOptions().isAllowingDamage())));
							} else {
								sender.sendMessage(msg("noGame", true));
							}
						} else {
							final ParkourGame parkour = ParkourManager.getParkour(args[1]);
							if (parkour != null) {
								parkour.getOptions().setAllowingDamage(Boolean.parseBoolean(args[2]));
								sender.sendMessage(msg("setAllowingDamage", false).replace("%DAMAGE%", String.valueOf(Boolean.parseBoolean(args[2]))));
							} else {
								sender.sendMessage(msg("noGame", true));
							}
						}
					} else {
						sender.sendMessage(msg("noPerms", true));
					}
				} else if (args[0].equalsIgnoreCase("available") || args[0].equalsIgnoreCase("a")) {
					if (!(sender instanceof Player) || sender.hasPermission("ats.parkour.available")) {
						if (args.length == 1) {
							sender.sendMessage(msg("usageAvailable", true));
						} else if (args.length == 2) {
							final ParkourGame parkour = ParkourManager.getParkour(args[1]);
							if (parkour != null) {
								sender.sendMessage(msg("currentAvailable", false).replace("%AVAILABLE%", String.valueOf(parkour.getOptions().isAvailable())));
							} else {
								sender.sendMessage(msg("noGame", true));
							}
						} else {
							final ParkourGame parkour = ParkourManager.getParkour(args[1]);
							if (parkour != null) {
								parkour.getOptions().setAvailable(Boolean.parseBoolean(args[2]));
								sender.sendMessage(msg("setAvailable", false).replace("%AVAILABLE%", String.valueOf(Boolean.parseBoolean(args[2]))));
							} else {
								sender.sendMessage(msg("noGame", true));
							}
						}
					} else {
						sender.sendMessage(msg("noPerms", true));
					}
				} else if (args[0].equalsIgnoreCase("boat") || args[0].equalsIgnoreCase("b")) {
					if (!(sender instanceof Player) || sender.hasPermission("ats.parkour.boat")) {
						if (args.length == 1) {
							sender.sendMessage(msg("usageBoats", true));
						} else if (args.length == 2) {
							final ParkourGame parkour = ParkourManager.getParkour(args[1]);
							if (parkour != null) {
								sender.sendMessage(msg("currentBoats", false).replace("%BOATS%", String.valueOf(parkour.getOptions().isBoat())));
							} else {
								sender.sendMessage(msg("noGame", true));
							}
						} else {
							final ParkourGame parkour = ParkourManager.getParkour(args[1]);
							if (parkour != null) {
								parkour.getOptions().setBoat(Boolean.parseBoolean(args[2]));
								sender.sendMessage(msg("setBoat", false).replace("%BOATS%", String.valueOf(Boolean.parseBoolean(args[2]))));
							} else {
								sender.sendMessage(msg("noGame", true));
							}
						}
					} else {
						sender.sendMessage(msg("noPerms", true));
					}
				} else if (args[0].equalsIgnoreCase("enabled") || args[0].equalsIgnoreCase("p")) {
					if (!(sender instanceof Player) || sender.hasPermission("ats.parkour.enabled")) {
						if (args.length == 1) {
							sender.sendMessage(msg("usageProceedable", true));
						} else if (args.length == 2) {
							final ParkourGame parkour = ParkourManager.getParkour(args[1]);
							if (parkour != null) {
								sender.sendMessage(msg("currentProceedable", false).replace("%PROCEEDABLE%", String.valueOf(parkour.getOptions().isEnabled())));
							} else {
								sender.sendMessage(msg("noGame", true));
							}
						} else {
							final ParkourGame parkour = ParkourManager.getParkour(args[1]);
							if (parkour != null) {
								parkour.getOptions().setEnabled(Boolean.parseBoolean(args[2]));
								sender.sendMessage(msg("setEnabled", false).replace("%PROCEEDABLE%", String.valueOf(Boolean.parseBoolean(args[2]))));
							} else {
								sender.sendMessage(msg("noGame", true));
							}
						}
					} else {
						sender.sendMessage(msg("noPerms", true));
					}
				} else if (args[0].equalsIgnoreCase("effect") || args[0].equalsIgnoreCase("e")) {
					if (!(sender instanceof Player) || sender.hasPermission("ats.parkour.effect")) {
						if (args.length == 1) {
							sender.sendMessage(msg("usageEffect", true));
						} else {
							final ParkourGame parkour = ParkourManager.getParkour(args[1]);
							if (parkour != null) {
								final ParkourGame.Options o = parkour.getOptions();
								if (args.length == 2) {
									for (final Entry<PotionEffectType, Integer> entry : o.getEffects().entrySet()) {
										sender.sendMessage(msg("oneEffect", false).replace("%EFFECT%", "" + entry.getKey().getName()).replace("%AMPLIFIER%", "" + entry.getValue()));
									}
								} else {
									final PotionEffectType p = PotionEffectType.getByName(args[2]);
									if (p != null) {
										if (Data.ALLOWED_EFFECTS.contains(p)) {
											if (args.length == 3) {
												sender.sendMessage(msg("currentEffect", false).replace("%EFFECT%", p.getName()).replace("%AMPLIFIER%", "" + o.getEffects().get(p)));
											} else {
												try {
													final int a = Integer.parseInt(args[3]);
													if (a == 0) {
														o.removeEffect(p);
														sender.sendMessage(msg("effectRemoved", false).replace("%EFFECT%", p.getName()));
													} else {
														o.setEffect(p, a);
														sender.sendMessage(msg("setEffect", false).replace("%EFFECT%", p.getName()).replace("%AMPLIFIER%", a + ""));
													}
												} catch (final Exception ex) {
													sender.sendMessage(msg("noNumber", true));
												}
											}
										} else {
											sender.sendMessage(msg("disallowedEffect", true));
										}
									} else {
										sender.sendMessage(msg("noEffect", true));
									}
								}
							} else {
								sender.sendMessage(msg("noGame", true));
							}
						}
					} else {
						sender.sendMessage(msg("noPerms", true));
					}
				} else if (args[0].equalsIgnoreCase("effectregion") || args[0].equalsIgnoreCase("er")) {
					if (!(sender instanceof Player) || sender.hasPermission("ats.parkour.effectregion")) {
						if (args.length == 1) {
							sender.sendMessage(msg("usageEffectregion", true));
						} else if (args.length == 2) {
							/*if (ParkourPlugin.gameExists(args[1])) {
								final ParkourPlugin pk = ParkourPlugin.getGame(args[1]);
							} else {
								sender.sendMessage(msgA("noGame", true));
							}*/
						}
					} else {
						sender.sendMessage(msg("noPerms", true));
					}
				} else if (args[0].equalsIgnoreCase("modifyInventory") || args[0].equalsIgnoreCase("eq")) {
					if (!(sender instanceof Player) || sender.hasPermission("ats.parkour.modifyInventory")) {
						if (args.length == 1) {
							sender.sendMessage(msg("usageModifyeq", true));
						} else if (args.length == 2) {
							final ParkourGame parkour = ParkourManager.getParkour(args[1]);
							if (parkour != null) {
								sender.sendMessage(msg("currentModifyeq", false).replace("%MODIFYEQ%", String.valueOf(parkour.getOptions().isModifyInventory())));
							} else {
								sender.sendMessage(msg("noGame", true));
							}
						} else {
							final ParkourGame parkour = ParkourManager.getParkour(args[1]);
							if (parkour != null) {
								parkour.getOptions().setModifyInventory(Boolean.parseBoolean(args[2]));
								sender.sendMessage(msg("setModifyInventory", false).replace("%MODIFYEQ%", String.valueOf(Boolean.parseBoolean(args[2]))));
							} else {
								sender.sendMessage(msg("noGame", true));
							}
						}
					} else {
						sender.sendMessage(msg("noPerms", true));
					}
				} else if (args[0].equalsIgnoreCase("difficulty") || args[0].equalsIgnoreCase("d")) {
					if (!(sender instanceof Player) || sender.hasPermission("ats.parkour.difficulty")) {
						if (args.length == 1) {
							sender.sendMessage(msg("usageDifficulty", true));
						} else if (args.length == 2) {
							final ParkourGame parkour = ParkourManager.getParkour(args[1]);
							if (parkour != null) {
								sender.sendMessage(msg("currentDifficulty", false).replace("%DIFFICULTY%", String.valueOf(parkour.getOptions().getDifficulty())));
							} else {
								sender.sendMessage(msg("noGame", true));
							}
						} else {
							try {
								final ParkourGame parkour = ParkourManager.getParkour(args[1]);
								if (parkour != null) {
									parkour.getOptions().setDifficulty(Integer.parseInt(args[2]));
									sender.sendMessage(msg("setDifficulty", false).replace("%DIFFICULTY%", String.valueOf(Integer.parseInt(args[2]))));
								} else {
									sender.sendMessage(msg("noGame", true));
								}
							} catch (final NumberFormatException e) {
								sender.sendMessage(msg("numberExpected", true));
							}
						}
					} else {
						sender.sendMessage(msg("noPerms", true));
					}
				} else if (args[0].equalsIgnoreCase("displayname") || args[0].equalsIgnoreCase("dn")) {
					if (!(sender instanceof Player) || sender.hasPermission("ats.parkour.displayname")) {
						if (args.length == 1) {
							sender.sendMessage(msg("usageDisplayname", true));
						} else if (args.length == 2) {
							final ParkourGame parkour = ParkourManager.getParkour(args[1]);
							if (parkour != null) {
								final String tmp = msg("currentDisplayname", false).split("%")[0];
								final String color = "\u00A7" + tmp.charAt(tmp.lastIndexOf('\u00A7') + 1);
								sender.sendMessage(msg("currentDisplayname", false).replace("%DISPLAYNAME%", "\u00A7r" + parkour.getOptions().getDisplayName().replace('&', '\u00A7') + color));
							} else {
								sender.sendMessage(msg("noGame", true));
							}
						} else {
							final ParkourGame parkour = ParkourManager.getParkour(args[1]);
							if (parkour != null) {
								final StringBuilder nameBuilder = new StringBuilder();
								for (int i = 2; i < args.length; i++) {
									nameBuilder.append(" ").append(args[i]);
								}
								String name = nameBuilder.substring(1);
								name = name.replace('&', '\u00A7');
								final String tmp = msg("setDisplayname", false).split("%")[0];
								final String color = "\u00A7" + tmp.charAt(tmp.lastIndexOf('\u00A7') + 1);
								parkour.getOptions().setDisplayName(name);
								sender.sendMessage(msg("setDisplayname", false).replace("%DISPLAYNAME%", name + color));
							} else {
								sender.sendMessage(msg("noGame", true));
							}
						}
					} else {
						sender.sendMessage(msg("noPerms", true));
					}
				} else if (args[0].equalsIgnoreCase("bronze")) {
					if (!(sender instanceof Player) || sender.hasPermission("ats.parkour.bronze")) {
						if (args.length == 1) {
							sender.sendMessage(msg("usageBronze", true));
						} else if (args.length == 2) {
							final ParkourGame parkour = ParkourManager.getParkour(args[1]);
							if (parkour != null) {
								sender.sendMessage(msg("currentBronze", false).replace("%BRONZE%", String.valueOf(parkour.getOptions().getBronze())));
							} else {
								sender.sendMessage(msg("noGame", true));
							}
						} else {
							try {
								final ParkourGame parkour = ParkourManager.getParkour(args[1]);
								if (parkour != null) {
									parkour.getOptions().setBronze(Integer.parseInt(args[2]));
									sender.sendMessage(msg("setBronze", false).replace("%BRONZE%", String.valueOf(Integer.parseInt(args[2]))));
								} else {
									sender.sendMessage(msg("noGame", true));
								}
							} catch (final NumberFormatException e) {
								sender.sendMessage(msg("numberExpected", true));
							}
						}
					} else {
						sender.sendMessage(msg("noPerms", true));
					}
				} else if (args[0].equalsIgnoreCase("fair")) {
					if (!(sender instanceof Player) || sender.hasPermission("ats.parkour.fair")) {
						if (args.length == 1) {
							sender.sendMessage(msg("usageFair", true));
						} else if (args.length == 2) {
							final ParkourGame parkour = ParkourManager.getParkour(args[1]);
							if (parkour != null) {
								sender.sendMessage(msg("currentFair", false).replace("%FAIR%", String.valueOf(parkour.getOptions().getFair())));
							} else {
								sender.sendMessage(msg("noGame", true));
							}
						} else {
							try {
								final ParkourGame parkour = ParkourManager.getParkour(args[1]);
								if (parkour != null) {
									parkour.getOptions().setFair(Float.parseFloat(args[2]));
									sender.sendMessage(msg("setFair", false).replace("%FAIR%", String.valueOf(Float.parseFloat(args[2]))));
								} else {
									sender.sendMessage(msg("noGame", true));
								}
							} catch (final NumberFormatException e) {
								sender.sendMessage(msg("numberExpected", true));
							}
						}
					} else {
						sender.sendMessage(msg("noPerms", true));
					}
				} else if (args[0].equalsIgnoreCase("silver")) {
					if (!(sender instanceof Player) || sender.hasPermission("ats.parkour.silver")) {
						if (args.length == 1) {
							sender.sendMessage(msg("usageSilver", true));
						} else if (args.length == 2) {
							final ParkourGame parkour = ParkourManager.getParkour(args[1]);
							if (parkour != null) {
								sender.sendMessage(msg("currentSilver", false).replace("%SILVER%", String.valueOf(parkour.getOptions().getSilver())));
							} else {
								sender.sendMessage(msg("noGame", true));
							}
						} else {
							try {
								final ParkourGame parkour = ParkourManager.getParkour(args[1]);
								if (parkour != null) {
									parkour.getOptions().setSilver(Integer.parseInt(args[2]));
									sender.sendMessage(msg("setSilver", false).replace("%SILVER%", String.valueOf(Integer.parseInt(args[2]))));
								} else {
									sender.sendMessage(msg("noGame", true));
								}
							} catch (final NumberFormatException e) {
								sender.sendMessage(msg("numberExpected", true));
							}
						}
					} else {
						sender.sendMessage(msg("noPerms", true));
					}
				} else if (args[0].equalsIgnoreCase("gold")) {
					if (!(sender instanceof Player) || sender.hasPermission("ats.parkour.gold")) {
						if (args.length == 1) {
							sender.sendMessage(msg("usageGold", true));
						} else if (args.length == 2) {
							final ParkourGame parkour = ParkourManager.getParkour(args[1]);
							if (parkour != null) {
								sender.sendMessage(msg("currentGold", false).replace("%GOLD%", String.valueOf(parkour.getOptions().getGold())));
							} else {
								sender.sendMessage(msg("noGame", true));
							}
						} else {
							try {
								final ParkourGame parkour = ParkourManager.getParkour(args[1]);
								if (parkour != null) {
									parkour.getOptions().setGold(Integer.parseInt(args[2]));
									sender.sendMessage(msg("setGold", false).replace("%GOLD%", String.valueOf(Integer.parseInt(args[2]))));
								} else {
									sender.sendMessage(msg("noGame", true));
								}
							} catch (final NumberFormatException e) {
								sender.sendMessage(msg("numberExpected", true));
							}
						}
					} else {
						sender.sendMessage(msg("noPerms", true));
					}
				} else if (args[0].equalsIgnoreCase("platinum")) {
					if (!(sender instanceof Player) || sender.hasPermission("ats.parkour.platinum")) {
						if (args.length == 1) {
							sender.sendMessage(msg("usagePlatinum", true));
						} else if (args.length == 2) {
							final ParkourGame parkour = ParkourManager.getParkour(args[1]);
							if (parkour != null) {
								sender.sendMessage(msg("currentPlatinum", false).replace("%PLATINUM%", String.valueOf(parkour.getOptions().getPlatinum())));
							} else {
								sender.sendMessage(msg("noGame", true));
							}
						} else {
							try {
								final ParkourGame parkour = ParkourManager.getParkour(args[1]);
								if (parkour != null) {
									parkour.getOptions().setPlatinum(Integer.parseInt(args[2]));
									sender.sendMessage(msg("setPlatnum", false).replace("%PLATINUM%", String.valueOf(Integer.parseInt(args[2]))));
								} else {
									sender.sendMessage(msg("noGame", true));
								}
							} catch (final NumberFormatException e) {
								sender.sendMessage(msg("numberExpected", true));
							}
						}
					} else {
						sender.sendMessage(msg("noPerms", true));
					}
				} else if (args[0].equalsIgnoreCase("color")) {
					if (!(sender instanceof Player) || sender.hasPermission("ats.parkour.color")) {
						if (args.length == 1) {
							sender.sendMessage(msg("usageColor", true));
						} else if (args.length == 2) {
							final ParkourGame parkour = ParkourManager.getParkour(args[1]);
							if (parkour != null) {
								sender.sendMessage(msg("currentColor", false).replace("%COLOR%", "" + parkour.getOptions().getColor().name()));
							} else {
								sender.sendMessage(msg("noGame", true));
							}
						} else {
							final ParkourGame parkour = ParkourManager.getParkour(args[1]);
							if (parkour != null) {
								try {
									args[2] = args[2].toUpperCase();
									parkour.getOptions().setColor(DyeColor.valueOf(args[2]));
									sender.sendMessage(msg("setColor", false).replace("%COLOR%", "" + DyeColor.valueOf(args[2]).name()));
								} catch (final IllegalArgumentException ex) {
									sender.sendMessage(msg("wrongColor", true).replace("%COLOR%", args[2]));
								}
							} else {
								sender.sendMessage(msg("noGame", true));
							}
						}
					} else {
						sender.sendMessage(msg("noPerms", true));
					}
				} else if (args[0].equalsIgnoreCase("authors")) {
					if (!(sender instanceof Player) || sender.hasPermission("ats.parkour.authors")) {
						if (args.length == 1 || args.length == 3) {
							sender.sendMessage(msg("usageAuthors", true));
						} else if (args.length == 2) {
							final ParkourGame parkour = ParkourManager.getParkour(args[1]);
							if (parkour != null) {
								sender.sendMessage(msg("currentAuthors", false).replace("%AUTHORS%", "" + parkour.getAuthors()));
							} else {
								sender.sendMessage(msg("noGame", true));
							}
						} else {
							final ParkourGame parkour = ParkourManager.getParkour(args[1]);
							if (parkour != null) {
								if (args[2].equalsIgnoreCase("add")) {
									parkour.addAuthor(args[3]);
									sender.sendMessage(msg("addedAuthor", false));
								} else if (args[2].equalsIgnoreCase("remove")) {
									if (parkour.removeAuthor(args[3])) {
										sender.sendMessage(msg("removedAuthor", false));
									} else {
										sender.sendMessage(msg("noAuthor", true));
									}
								} else {
									sender.sendMessage(msg("wrongArg", true));
								}
							} else {
								sender.sendMessage(msg("noGame", true));
							}
						}
					} else {
						sender.sendMessage(msg("noPerms", true));
					}
				} else if (args[0].equalsIgnoreCase("type")) {
					if (!(sender instanceof Player) || sender.hasPermission("ats.parkour.type")) {
						if (args.length == 1) {
							sender.sendMessage(msg("usageType", true));
						} else if (args.length == 2) {
							final ParkourGame parkour = ParkourManager.getParkour(args[1]);
							if (parkour != null) {
								sender.sendMessage(msg("currentType", false).replace("%TYPE%", "" + parkour.getOptions().getType().toString()));
							} else {
								sender.sendMessage(msg("noGame", true));
							}
						} else {
							final ParkourGame parkour = ParkourManager.getParkour(args[1]);
							if (parkour != null) {
								try {
									parkour.getOptions().setType(Parkour.ParkourType.valueOf(args[2]));
									sender.sendMessage(msg("setType", false).replace("%TYPE%", "" + Parkour.ParkourType.valueOf(args[2]).toString()));
								} catch (final Exception ex) {
									sender.sendMessage(msg("noType", true));
								}
							} else {
								sender.sendMessage(msg("noGame", true));
							}
						}
					} else {
						sender.sendMessage(msg("noPerms", true));
					}
				} else if (args[0].equalsIgnoreCase("help") || args[0].equalsIgnoreCase("?")) {
					if (!(sender instanceof Player) || sender.hasPermission("ats.parkour.help")) {
						int i = 0;
						if (args.length != 1) {
							try {
								i = 5 * (Integer.parseInt(args[1]) - 1);
							} catch (final NumberFormatException ex) {
								sender.sendMessage(ChatColor.DARK_RED + ex.getMessage());
								i = 0;
							}
						}
						final Map<String, String> list = plugin.getMessages();
						final int max = i + 5;
						final int value = list.size() / 5 + 1;
						final int p = i / 5 + 1;
						if (p <= value) {
							sender.sendMessage(msg("currentPage", false).replace("%PAGE%", String.valueOf(p)).replace("%MAXPAGES%", String.valueOf(value)));
							final List<?> l = new ArrayList<>(list.entrySet());
							for (; i < max && i < list.size(); i++) {
								final String arg = String.valueOf(l.get(i)).split("=")[0];
								final String desc = String.valueOf(l.get(i)).split("=")[1];
								sender.sendMessage("§2/pk " + arg + "§r - " + desc);
							}
						}
					} else {
						sender.sendMessage(msg("noPerms", true));
					}
				} else if (args[0].equalsIgnoreCase("info")) {
					if (!(sender instanceof Player) || sender.hasPermission("ats.parkour.info")) {
						if (args.length == 2) {
							final ParkourGame parkour = ParkourManager.getParkour(args[1]);
							if (parkour != null) {
								sender.sendMessage(parkour.toString());
							} else {
								sender.sendMessage(msg("noGame", true));
							}
						} else {
							sender.sendMessage(msg("usageInfo", true));
						}
					} else {
						sender.sendMessage(msg("noPerms", true));
					}
				} else if (args[0].equalsIgnoreCase("teleportblock") || args[0].equalsIgnoreCase("tb")) {
					if (!(sender instanceof Player) || sender.hasPermission("ats.parkour.teleportblock")) {
						if (args.length == 2) {
							final ParkourGame parkour = ParkourManager.getParkour(args[1]);
							if (parkour != null) {
								parkour.setTeleportBlock(((Player) sender).getTargetBlock(null, 5).getLocation());
								sender.sendMessage(msg("setTeleportblock", false).replace("%PARKOUR%", args[1]));
							} else {
								sender.sendMessage(msg("noGame", true));
							}
						} else {
							sender.sendMessage(msg("usageTeleportblock", true));
						}
					} else {
						sender.sendMessage(msg("noPerms", true));
					}
				} else if (args[0].equalsIgnoreCase("group") || args[0].equalsIgnoreCase("g")) {
					if (!(sender instanceof Player) || sender.hasPermission("ats.parkour.group")) {
						if (args.length == 1) {
							sender.sendMessage(msg("usageGroup", true));
						}
					} else {
						sender.sendMessage(msg("noPerms", true));
					}
				} else if (args[0].equalsIgnoreCase("top")) {
					if (!(sender instanceof Player) || sender.hasPermission("ats.parkour.top")) {
						int i;
						if (args.length == 1) {
							sender.sendMessage("usageTop");
						} else {
							if (args.length == 2) {
								i = 10;
							} else {
								try {
									i = Integer.parseInt(args[2]);
								} catch (final NumberFormatException ex) {
									i = 10;
								}
							}
							final ParkourGame pk = ParkourManager.getParkour(args[1]);
							if (pk != null) {
								Bukkit.getScheduler().runTaskAsynchronously(plugin, new TopPlayersDataOperations(plugin, pk, i, result -> {
									int j = 1;
									for (final Entry<String, Float> entry : result.getResult().entrySet()) {
										sender.sendMessage(msg("topRecord", false).replace("%NUMBER%", "" + j++).replace("%PLAYER%", entry.getKey()).replace("%TIME%", "" + entry.getValue()));
									}
								}));
							} else {
								sender.sendMessage(msg("noGame", true));
							}
						}
					} else {
						sender.sendMessage(msg("noPerms", true));
					}
				} else if (args[0].equalsIgnoreCase("bestrecord") || args[0].equalsIgnoreCase("br")) {
					if (!(sender instanceof Player) || sender.hasPermission("ats.parkour.bestrecord")) {
						if (args.length == 1) {
							sender.sendMessage(msg("usageBestrecord", true));
						} else {
							final ParkourGame parkour = ParkourManager.getParkour(args[1]);
							if (parkour != null) {
								final Location loc = ((Player) sender).getTargetBlock(null, 5).getLocation();
								final Material m = loc.getBlock().getType();
								if (Data.SIGNS.contains(m)) {
									parkour.setBestRecord(loc);
									sender.sendMessage(msg("setBestrecord", false).replace("%COORDX%", "" + loc.getX()).replace("%COORDY%", "" + loc.getY()).replace("%COORDZ%", "" + loc.getZ()));
									try {
										final PreparedStatement stat = plugin.getConnection().prepareStatement(String.format("SELECT time, nick FROM %s WHERE parkour=? ORDER BY time LIMIT 1", Data.TABLE_RECORDS));
										stat.setString(1, args[1]);
										final ResultSet rs = stat.executeQuery();
										if (rs.next()) {
											DataBaseOperations.updateSign(plugin, rs.getString("nick"), rs.getDouble("time"), parkour);
										} else {
											DataBaseOperations.updateSign(plugin, "========", 0.00, parkour);
										}
									} catch (final Exception ex) {
										Bukkit.getServer().getLogger().throwing(getClass().getName(), "onCommand", ex);
									}
								} else {
									sender.sendMessage(msg("noSign", true));
								}
							} else {
								sender.sendMessage(msg("noGame", true));
							}
						}
					} else {
						sender.sendMessage(msg("noPerms", true));
					}
				} else {
					sender.sendMessage(msg("wrongArg", true));
				}
			}
		}
		return true;
	}

	private String msg(final String arg, final boolean error) {
		return plugin.msg(arg, error);
	}
}
