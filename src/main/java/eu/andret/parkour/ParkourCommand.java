///*
// * Copyright Andret (c) 2019. Copying and modifying allowed only keeping git link reference.
// */
//package eu.andret.parkour;
//
//import com.sk89q.worldedit.bukkit.BukkitWorld;
//import com.sk89q.worldedit.bukkit.WorldEditPlugin;
//import com.sk89q.worldedit.bukkit.selections.Selection;
//import com.sk89q.worldedit.regions.CuboidRegion;
//import com.sk89q.worldedit.world.AbstractWorld;
//import eu.andret.parkour.parkour.Parkour;
//import eu.andret.parkour.parkour.ParkourGame;
//import eu.andret.parkour.parkour.ParkourGame.Options;
//import eu.andret.parkour.parkour.ParkourManager;
//import eu.andret.parkour.player.ParkourPlayer;
//import eu.andret.parkour.player.PlayerManager;
//import eu.andret.parkour.region.AbstractRegion;
//import eu.andret.parkour.region.Checkpoint;
//import eu.andret.parkour.region.GameRegion;
//import eu.andret.parkour.region.Wall;
//import eu.andret.parkour.tasks.DataBaseOperations;
//import eu.andret.parkour.tasks.RepairSignTask;
//import eu.andret.parkour.tasks.TopPlayersDataOperations;
//import eu.andret.parkour.util.Data;
//import org.bukkit.Bukkit;
//import org.bukkit.ChatColor;
//import org.bukkit.DyeColor;
//import org.bukkit.Location;
//import org.bukkit.Material;
//import org.bukkit.command.Command;
//import org.bukkit.command.CommandExecutor;
//import org.bukkit.command.CommandSender;
//import org.bukkit.entity.Player;
//import org.bukkit.potion.PotionEffectType;
//
//import java.sql.PreparedStatement;
//import java.sql.ResultSet;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//import java.util.Map;
//import java.util.Map.Entry;
//import java.util.Set;
//
//public class ParkourCommand implements CommandExecutor {
//	private final WorldEditPlugin wep;
//	private final ParkourPlugin plugin;
//
//	public ParkourCommand(ParkourPlugin plugin) {
//		this.plugin = plugin;
//		wep = (WorldEditPlugin) plugin.getServer().getPluginManager().getPlugin("WorldEdit");
//	}
//
//	@Override
//	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
//		if (cmd.getName().equalsIgnoreCase("parkour")) {
//			if (args.length == 0) {
//				if (!(sender instanceof Player) || sender.hasPermission("ats.parkour.main")) {
//					List<Object> l = Arrays.asList(plugin.getMessages().entrySet().toArray());
//					for (int i = 0; i < 5 && i < l.size(); i++) {
//						String arg = String.valueOf(l.get(i)).split("=")[0];
//						String desc = String.valueOf(l.get(i)).split("=")[1];
//						sender.sendMessage("§2/pk " + arg + "§r - " + desc);
//					}
//				} else {
//					sender.sendMessage(msg("noPerms", true));
//				}
//			} else {
//				if (args[0].equalsIgnoreCase("create") || args[0].equalsIgnoreCase("c")) {
//					if (sender instanceof Player && sender.hasPermission("ats.parkour.create")) {
//						if (args.length == 1) {
//							sender.sendMessage(msg("usageCreate", true));
//						} else {
//							if (ParkourManager.getLobbyLocation() != null) {
//								Selection sel = wep.getSelection((Player) sender);
//								if (sel != null) {
//									AbstractWorld lw = new BukkitWorld(((Player) sender).getWorld());
//									CuboidRegion cr = new CuboidRegion(lw, sel.getNativeMaximumPoint(), sel.getNativeMinimumPoint());
//									String name = args[1];
//									if (name.matches("[a-zA-Z0-9_-]+")) {
//										ParkourGame parkour = ParkourManager.getParkour(name);
//										if (parkour == null) {
//											Parkour pk = new Parkour(name, new GameRegion(cr), ((Player) sender).getLocation().getWorld());
//											sender.sendMessage(msg("gameCreated", false).replace("%GAMENAME%", pk.getName()));
//										} else {
//											sender.sendMessage(msg("gameExists", true).replace("%GAMENAME%", name));
//										}
//									} else {
//										sender.sendMessage(msg("wrongName", true));
//									}
//								} else {
//									sender.sendMessage(msg("wrongSel", true));
//								}
//							} else {
//								sender.sendMessage(msg("lobbyFirst", true));
//							}
//						}
//					} else {
//						sender.sendMessage(msg("noPerms", true));
//					}
//				} else if (args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("r")) {
//					if (!(sender instanceof Player) || sender.hasPermission("ats.parkour.remove")) {
//						if (args.length == 1) {
//							sender.sendMessage(msg("usageRemove", true));
//						} else {
//							ParkourGame parkour = ParkourManager.getParkour(args[1]);
//							if (parkour != null) {
//								ParkourManager.removeParkour(parkour);
//								sender.sendMessage(msg("gameRemoved", false).replace("%GAMENAME%", args[1]));
//							} else {
//								sender.sendMessage(msg("noGame", true));
//							}
//						}
//					} else {
//						sender.sendMessage(msg("noPerms", true));
//					}
//				} else if (args[0].equalsIgnoreCase("setspawn") || args[0].equalsIgnoreCase("ss")) {
//					if (sender instanceof Player && sender.hasPermission("ats.parkour.setspawn")) {
//						if (args.length == 1) {
//							sender.sendMessage(msg("usageSetspawn", true));
//						} else {
//							Selection sel = wep.getSelection((Player) sender);
//							if (sel != null) {
//								AbstractWorld lw = new BukkitWorld(((Player) sender).getWorld());
//								CuboidRegion cr = new CuboidRegion(lw, sel.getNativeMaximumPoint(), sel.getNativeMinimumPoint());
//								ParkourGame pk = ParkourManager.getParkour(args[1]);
//								if (pk != null) {
//									Location l = ((Player) sender).getLocation();
//									pk.setSpawn(new Checkpoint(cr, l.getYaw(), l.getPitch()));
//									sender.sendMessage(msg("setSpawn", false));
//								} else {
//									sender.sendMessage(msg("noGame", true));
//								}
//							} else {
//								sender.sendMessage(msg("wrongSel", true));
//							}
//						}
//					} else {
//						sender.sendMessage(msg("noPerms", true));
//					}
//				} else if (args[0].equalsIgnoreCase("addcheckpoint") || args[0].equalsIgnoreCase("ac")) {
//					if (sender instanceof Player && sender.hasPermission("ats.parkour.addcheckpoint")) {
//						if (args.length == 1) {
//							sender.sendMessage(msg("usageAddcheckpoint", true));
//						} else {
//							Selection sel = wep.getSelection((Player) sender);
//							if (sel != null) {
//								AbstractWorld lw = new BukkitWorld(((Player) sender).getWorld());
//								CuboidRegion cr = new CuboidRegion(lw, sel.getNativeMaximumPoint(), sel.getNativeMinimumPoint());
//								ParkourGame pk = ParkourManager.getParkour(args[1]);
//								if (pk != null) {
//									if (pk.getSpawn() != null) {
//										Location l = ((Player) sender).getLocation();
//										pk.addCheckpoint(new Checkpoint(cr, l.getYaw(), l.getPitch()));
//										sender.sendMessage(msg("setCheckpoint", false).replace("%CHECKPOINTID%", String.valueOf(pk.getCheckpointList().size() - 1)));
//									} else {
//										sender.sendMessage(msg("noSpawn", true));
//									}
//								} else {
//									sender.sendMessage(msg("noGame", true));
//								}
//							} else {
//								sender.sendMessage(msg("wrongSel", true));
//							}
//						}
//					} else {
//						sender.sendMessage(msg("noPerms", true));
//					}
//				} else if (args[0].equalsIgnoreCase("setcheckpoint") || args[0].equalsIgnoreCase("sc")) {
//					if (sender instanceof Player && sender.hasPermission("ats.parkour.setcheckpoint")) {
//						if (args.length == 1 || args.length == 2) {
//							sender.sendMessage(msg("usageSetcheckpoint", true));
//						} else {
//							try {
//								Selection sel = wep.getSelection((Player) sender);
//								if (sel != null) {
//									AbstractWorld lw = new BukkitWorld(((Player) sender).getWorld());
//									CuboidRegion cr = new CuboidRegion(lw, sel.getNativeMaximumPoint(), sel.getNativeMinimumPoint());
//									int id = Integer.parseInt(args[1]);
//									if (id > 0) {
//										ParkourGame pk = ParkourManager.getParkour(args[2]);
//										if (pk != null) {
//											Location l = ((Player) sender).getLocation();
//											pk.setCheckpoint(id, new Checkpoint(cr, l.getYaw(), l.getPitch()));
//											sender.sendMessage(msg("setCheckpoint", false).replace("%CHECKPOINTID%", args[1]));
//										} else {
//											sender.sendMessage(msg("noGame", true));
//										}
//									} else {
//										sender.sendMessage(msg("negativeNumber", true));
//									}
//								} else {
//									sender.sendMessage(msg("wrongSel", true));
//								}
//							} catch (NumberFormatException ex) {
//								sender.sendMessage(msg("noNumber", true));
//							}
//						}
//					} else {
//						sender.sendMessage(msg("noPerms", true));
//					}
//				} else if (args[0].equalsIgnoreCase("start") || args[0].equalsIgnoreCase("s")) {
//					if (!(sender instanceof Player) || sender.hasPermission("ats.parkour.start")) {
//						if (args.length == 1) {
//							sender.sendMessage(msg("usageStart", true));
//						} else {
//							ParkourGame pk = ParkourManager.getParkour(args[1]);
//							if (pk != null) {
//								if (pk.getSpawn() != null) {
//									Options o = pk.getOptions();
//									if (o.getBronze() != 0 && o.getSilver() != 0 && o.getGold() != 0 && o.getPlatinum() != 0) {
//										if (!pk.isRunning()) {
//											sender.sendMessage(msg("gameStarted", false).replace("%GAMENAME%", args[1]));
//											pk.start();
//										} else {
//											sender.sendMessage(msg("alreadyStarted", true));
//										}
//									} else {
//										sender.sendMessage(msg("medalsFirst", true));
//									}
//								} else {
//									sender.sendMessage(msg("noSpawn", true));
//								}
//							} else {
//								sender.sendMessage(msg("noGame", true));
//							}
//						}
//					} else {
//						sender.sendMessage(msg("noPerms", true));
//					}
//				} else if (args[0].equalsIgnoreCase("recreate") || args[0].equalsIgnoreCase("rc")) {
//					if (sender instanceof Player && sender.hasPermission("ats.parkour.recreate")) {
//						if (args.length == 1) {
//							sender.sendMessage(msg("usageRecreate", true));
//						} else {
//							ParkourGame pk = ParkourManager.getParkour(args[1]);
//							if (pk != null) {
//								Selection sel = wep.getSelection((Player) sender);
//								if (sel != null) {
//									AbstractWorld lw = new BukkitWorld(((Player) sender).getWorld());
//									CuboidRegion cr = new CuboidRegion(lw, sel.getNativeMaximumPoint(), sel.getNativeMinimumPoint());
//									pk.getGameRegion().setRegion(cr);
//									sender.sendMessage(msg("gameRecreated", false));
//								} else {
//									sender.sendMessage(msg("wrongSel", true));
//								}
//							} else {
//								sender.sendMessage(msg("noGame", true));
//							}
//						}
//					} else {
//						sender.sendMessage(msg("noPerms", true));
//					}
//				} else if (args[0].equalsIgnoreCase("fix")) {
//					if (sender instanceof Player && sender.hasPermission("ats.parkour.fix")) {
//						for (ParkourGame p : ParkourManager.getAllGames()) {
//							Bukkit.getScheduler().runTaskAsynchronously(plugin, new RepairSignTask(plugin, p));
//						}
//						sender.sendMessage(msg("successFix", false));
//					} else {
//						sender.sendMessage(msg("noPerms", true));
//					}
//				} else if (args[0].equalsIgnoreCase("stop")) {
//					if (!(sender instanceof Player) || sender.hasPermission("ats.parkour.stop")) {
//						if (args.length == 1) {
//							sender.sendMessage(msg("usageStop", true));
//						} else {
//							ParkourGame pk = ParkourManager.getParkour(args[1]);
//							if (pk != null) {
//								if (pk.isRunning()) {
//									pk.stop();
//									sender.sendMessage(msg("gameStopped", false).replace("%GAMENAME%", args[1]));
//								} else {
//									sender.sendMessage(msg("alreadyStopped", true));
//								}
//							} else {
//								sender.sendMessage(msg("noGame", true));
//							}
//						}
//					} else {
//						sender.sendMessage(msg("noPerms", true));
//					}
//				} else if (args[0].equalsIgnoreCase("top")) {
//					if (!(sender instanceof Player) || sender.hasPermission("ats.parkour.top")) {
//						int i;
//						if (args.length == 1) {
//							sender.sendMessage("usageTop");
//						} else {
//							if (args.length == 2) {
//								i = 10;
//							} else {
//								try {
//									i = Integer.parseInt(args[2]);
//								} catch (NumberFormatException ex) {
//									i = 10;
//								}
//							}
//							ParkourGame pk = ParkourManager.getParkour(args[1]);
//							if (pk != null) {
//								Bukkit.getScheduler().runTaskAsynchronously(plugin, new TopPlayersDataOperations(plugin, pk, i, result -> {
//									int j = 1;
//									for (Entry<String, Float> entry : result.getResult().entrySet()) {
//										sender.sendMessage(msg("topRecord", false).replace("%NUMBER%", "" + j++).replace("%PLAYER%", entry.getKey()).replace("%TIME%", "" + entry.getValue()));
//									}
//								}));
//							} else {
//								sender.sendMessage(msg("noGame", true));
//							}
//						}
//					} else {
//						sender.sendMessage(msg("noPerms", true));
//					}
//				} else if (args[0].equalsIgnoreCase("ignore") || args[0].equalsIgnoreCase("i")) {
//					if (sender instanceof Player && sender.hasPermission("ats.parkour.ignore")) {
//						Player pl = (Player) sender;
//						ParkourPlayer lp = PlayerManager.getParkourSinglePlayer(pl);
//						if (lp.isIgnoring()) {
//							lp.setIgnoring(false);
//							sender.sendMessage(msg("ignoreStop", false));
//							for (ParkourGame p : ParkourManager.getAllGames()) {
//								if (p.inAnyRegion(pl.getLocation())) {
//									p.addPlayer(pl);
//								}
//							}
//						} else {
//							lp.setIgnoring(true);
//							sender.sendMessage(msg("ignoreStart", false));
//							ParkourGame parkour = ParkourManager.getParkour(pl);
//							if (parkour != null) {
//								parkour.removePlayer(pl);
//							}
//						}
//					} else {
//						sender.sendMessage(msg("noPerms", true));
//					}
//				} else if (args[0].equalsIgnoreCase("spectator") || args[0].equalsIgnoreCase("spec")) {
//					if (sender instanceof Player && sender.hasPermission("ats.parkour.spectator")) {
//						Player pl = (Player) sender;
//						ParkourPlayer lp = PlayerManager.getParkourSinglePlayer(pl);
//						if (lp.isSpectating()) {
//							lp.setSpectating(false);
//							sender.sendMessage(msg("spectatorStop", false));
//							for (ParkourGame p : ParkourManager.getAllGames()) {
//								if (p.inAnyRegion(pl.getLocation())) {
//									p.addPlayer(pl);
//								}
//							}
//						} else {
//							lp.setSpectating(true);
//							sender.sendMessage(msg("spectatorStart", false));
//							ParkourGame parkour = ParkourManager.getParkour(pl);
//							if (parkour != null) {
//								parkour.removePlayer(pl);
//							}
//						}
//					} else {
//						sender.sendMessage(msg("noPerms", true));
//					}
//				} else if (args[0].equalsIgnoreCase("list") || args[0].equalsIgnoreCase("ls")) {
//					if (sender instanceof Player && sender.hasPermission("ats.parkour.list")) {
//						if (ParkourManager.getAllGames().size() == 0) {
//							sender.sendMessage(msg("emptyList", false));
//						} else {
//							sender.sendMessage(msg("listHeader", false).replace("%COUNT%", String.valueOf(ParkourManager.getAllGames().size())));
//							int i = 1;
//							for (ParkourGame pk : ParkourManager.getAllGames()) {
//								String on;
//								if (pk.isRunning()) {
//									on = ChatColor.GREEN + "" + ChatColor.ITALIC + "[Started]";
//								} else {
//									on = ChatColor.RED + "" + ChatColor.ITALIC + "[Stopped]";
//								}
//								sender.sendMessage(i + ". " + pk.getName() + " " + on);
//								i++;
//							}
//						}
//					} else {
//						sender.sendMessage(msg("noPerms", true));
//					}
//				} else if (args[0].equalsIgnoreCase("addwall") || args[0].equalsIgnoreCase("aw")) {
//					if (sender instanceof Player && sender.hasPermission("ats.parkour.addwall")) {
//						if (args.length == 1) {
//							sender.sendMessage(msg("usageAddwall", true));
//						} else {
//							Selection sel = wep.getSelection((Player) sender);
//							if (sel != null) {
//								AbstractWorld lw = new BukkitWorld(((Player) sender).getWorld());
//								CuboidRegion cr = new CuboidRegion(lw, sel.getNativeMaximumPoint(), sel.getNativeMinimumPoint());
//								ParkourGame pk = ParkourManager.getParkour(args[1]);
//								if (pk != null) {
//									pk.addWall(new Wall(cr));
//									sender.sendMessage(msg("setWall", false).replace("%WALLID%", String.valueOf(pk.getWallList().size())));
//								} else {
//									sender.sendMessage(msg("noGame", true));
//								}
//							} else {
//								sender.sendMessage(msg("wrongSel", true));
//							}
//						}
//					} else {
//						sender.sendMessage(msg("noPerms", true));
//					}
//				} else if (args[0].equalsIgnoreCase("setwall") || args[0].equalsIgnoreCase("sw")) {
//					if (sender instanceof Player && sender.hasPermission("ats.parkour.setwall")) {
//						if (args.length < 3) {
//							sender.sendMessage(msg("usageSetwall", true));
//						} else {
//							try {
//								Selection sel = wep.getSelection((Player) sender);
//								if (sel != null) {
//									AbstractWorld lw = new BukkitWorld(((Player) sender).getWorld());
//									CuboidRegion cr = new CuboidRegion(lw, sel.getNativeMaximumPoint(), sel.getNativeMinimumPoint());
//									int id = Integer.parseInt(args[1]) - 1;
//									if (id > 0) {
//										ParkourGame pk = ParkourManager.getParkour(args[2]);
//										if (pk != null) {
//											pk.setWall(id, cr);
//											sender.sendMessage(msg("setWall", false).replace("%WALLID%", args[1]));
//										} else {
//											sender.sendMessage(msg("noGame", true));
//										}
//									} else {
//										sender.sendMessage(msg("negativeNumber", true));
//									}
//								} else {
//									sender.sendMessage(msg("wrongSel", true));
//								}
//							} catch (NumberFormatException ex) {
//								sender.sendMessage(msg("noNumber", true));
//							}
//						}
//					} else {
//						sender.sendMessage(msg("noPerms", true));
//					}
//				} else if (args[0].equalsIgnoreCase("reload") || args[0].equalsIgnoreCase("rl")) {
//					if (!(sender instanceof Player) || sender.hasPermission("ats.parkour.reload")) {
//						plugin.onDisable();
//						plugin.onEnable();
//						plugin.reloadConfig();
//						sender.sendMessage(msg("reloadedConfig", false));
//					} else {
//						sender.sendMessage(msg("noPerms", true));
//					}
//				} else if (args[0].equalsIgnoreCase("sprint") || args[0].equalsIgnoreCase("sp")) {
//					if (!(sender instanceof Player) || sender.hasPermission("ats.parkour.sprint")) {
//						if (args.length == 1) {
//							sender.sendMessage(msg("usageSprint", true));
//						} else if (args.length == 2) {
//							ParkourGame parkour = ParkourManager.getParkour(args[1]);
//							if (parkour != null) {
//								sender.sendMessage(msg("currentSprint", false).replace("%SPRINT%", String.valueOf(parkour.getOptions().isForcingSprint())));
//							} else {
//								sender.sendMessage(msg("noGame", true));
//							}
//						} else {
//							ParkourGame parkour = ParkourManager.getParkour(args[1]);
//							if (parkour != null) {
//								parkour.getOptions().setForcingSprint(Boolean.valueOf(args[2]));
//								sender.sendMessage(msg("setSprint", false).replace("%SPRINT%", String.valueOf(Boolean.valueOf(args[2]))));
//							} else {
//								sender.sendMessage(msg("noGame", true));
//							}
//						}
//					} else {
//						sender.sendMessage(msg("noPerms", true));
//					}
//				} else if (args[0].equalsIgnoreCase("alwaysSpawn") || args[0].equalsIgnoreCase("as")) {
//					if (!(sender instanceof Player) || sender.hasPermission("ats.parkour.alwaysSpawn")) {
//						if (args.length == 1) {
//							sender.sendMessage(msg("usageAlwaysspawn", true));
//						} else if (args.length == 2) {
//							ParkourGame parkour = ParkourManager.getParkour(args[1]);
//							if (parkour != null) {
//								sender.sendMessage(msg("currentAlwaysspawn", false).replace("%ALWAYSSPAWN%", String.valueOf(parkour.getOptions().isAlwaysSpawn())));
//							} else {
//								sender.sendMessage(msg("noGame", true));
//							}
//						} else {
//							ParkourGame parkour = ParkourManager.getParkour(args[1]);
//							if (parkour != null) {
//								parkour.getOptions().setAlwaysSpawn(Boolean.valueOf(args[2]));
//								sender.sendMessage(msg("setAlwaysSpawn", false).replace("%ALWAYSSPAWN%", String.valueOf(Boolean.valueOf(args[2]))));
//							} else {
//								sender.sendMessage(msg("noGame", true));
//							}
//						}
//					} else {
//						sender.sendMessage(msg("noPerms", true));
//					}
//				} else if (args[0].equalsIgnoreCase("price") || args[0].equalsIgnoreCase("p")) {
//					if (!(sender instanceof Player) || sender.hasPermission("ats.parkour.price")) {
//						if (args.length == 1) {
//							sender.sendMessage(msg("usagePrice", true));
//						} else if (args.length == 2) {
//							ParkourGame parkour = ParkourManager.getParkour(args[1]);
//							if (parkour != null) {
//								sender.sendMessage(msg("currentPrice", false).replace("%PRICE%", String.valueOf(parkour.getOptions().getPrice())));
//							} else {
//								sender.sendMessage(msg("noGame", true));
//							}
//						} else {
//							ParkourGame parkour = ParkourManager.getParkour(args[1]);
//							if (parkour != null) {
//								parkour.getOptions().setPrice(Integer.valueOf(args[2]));
//								sender.sendMessage(msg("setPrice", false).replace("%PRICE%", String.valueOf(Integer.valueOf(args[2]))));
//							} else {
//								sender.sendMessage(msg("noGame", true));
//							}
//						}
//					} else {
//						sender.sendMessage(msg("noPerms", true));
//					}
//				} else if (args[0].equalsIgnoreCase("xp")) {
//					if (!(sender instanceof Player) || sender.hasPermission("ats.parkour.xp")) {
//						if (args.length == 1) {
//							sender.sendMessage(msg("usageXp", true));
//						} else if (args.length == 2) {
//							ParkourGame parkour = ParkourManager.getParkour(args[1]);
//							if (parkour != null) {
//								sender.sendMessage(msg("currentXp", false).replace("%XP%", String.valueOf(parkour.getOptions().getXp())));
//							} else {
//								sender.sendMessage(msg("noGame", true));
//							}
//						} else {
//							ParkourGame parkour = ParkourManager.getParkour(args[1]);
//							if (parkour != null) {
//								parkour.getOptions().setXp(Integer.valueOf(args[2]));
//								sender.sendMessage(msg("setXp", false).replace("%XP%", String.valueOf(Integer.valueOf(args[2]))));
//							} else {
//								sender.sendMessage(msg("noGame", true));
//							}
//						}
//					} else {
//						sender.sendMessage(msg("noPerms", true));
//					}
//				} else if (args[0].equalsIgnoreCase("clone")) {
//					if (!(sender instanceof Player) || sender.hasPermission("ats.parkour.clone")) {
//						if (args.length < 3) {
//							sender.sendMessage(msg("usageClone", true));
//						} else {
//							ParkourGame parkour1 = ParkourManager.getParkour(args[1]);
//							ParkourGame parkour2 = ParkourManager.getParkour(args[2]);
//							if (parkour1 != null && parkour2 != null) {
//								parkour2.setOptions(new Options(parkour1.getOptions()));
//								sender.sendMessage(msg("successClone", false).replace("%PARKOURFROM%", args[1]).replace("%PARKOURTO%", args[2]));
//							} else {
//								sender.sendMessage(msg("noGame", true));
//							}
//						}
//					} else {
//						sender.sendMessage(msg("noPerms", true));
//					}
//				} else if (args[0].equalsIgnoreCase("countingRecords") || args[0].equalsIgnoreCase("cr")) {
//					if (!(sender instanceof Player) || sender.hasPermission("ats.parkour.countingRecords")) {
//						if (args.length == 1) {
//							sender.sendMessage(msg("usageCountrecords", true));
//						} else if (args.length == 2) {
//							ParkourGame parkour = ParkourManager.getParkour(args[1]);
//							if (parkour != null) {
//								sender.sendMessage(msg("currentCountrecords", false).replace("%COUNTRECORDS%", String.valueOf(parkour.getOptions().isCountingRecords())));
//							} else {
//								sender.sendMessage(msg("noGame", true));
//							}
//						} else {
//							ParkourGame parkour = ParkourManager.getParkour(args[1]);
//							if (parkour != null) {
//								parkour.getOptions().setCountingRecords(Boolean.valueOf(args[2]));
//								sender.sendMessage(msg("setCountingRecords", false).replace("%COUNTRECORDS%", String.valueOf(Boolean.valueOf(args[2]))));
//							} else {
//								sender.sendMessage(msg("noGame", true));
//							}
//						}
//					} else {
//						sender.sendMessage(msg("noPerms", true));
//					}
//				} else if (args[0].equalsIgnoreCase("vip") || args[0].equalsIgnoreCase("v")) {
//					if (!(sender instanceof Player) || sender.hasPermission("ats.parkour.vip")) {
//						if (args.length == 1) {
//							sender.sendMessage(msg("usageVip", true));
//						} else if (args.length == 2) {
//							ParkourGame parkour = ParkourManager.getParkour(args[1]);
//							if (parkour != null) {
//								sender.sendMessage(msg("currentVip", false).replace("%VIP%", String.valueOf(parkour.getOptions().isVip())));
//							} else {
//								sender.sendMessage(msg("noGame", true));
//							}
//						} else {
//							ParkourGame parkour = ParkourManager.getParkour(args[1]);
//							if (parkour != null) {
//								parkour.getOptions().setVip(Boolean.valueOf(args[2]));
//								sender.sendMessage(msg("setVip", false).replace("%VIP%", String.valueOf(Boolean.valueOf(args[2]))));
//							} else {
//								sender.sendMessage(msg("noGame", true));
//							}
//						}
//					} else {
//						sender.sendMessage(msg("noPerms", true));
//					}
//				} else if (args[0].equalsIgnoreCase("allowingDamage") || args[0].equalsIgnoreCase("dmg")) {
//					if (!(sender instanceof Player) || sender.hasPermission("ats.parkour.allowingDamage")) {
//						if (args.length == 1) {
//							sender.sendMessage(msg("usageDamage", true));
//						} else if (args.length == 2) {
//							ParkourGame parkour = ParkourManager.getParkour(args[1]);
//							if (parkour != null) {
//								sender.sendMessage(msg("currentDamage", false).replace("%DAMAGE%", String.valueOf(parkour.getOptions().isAllowingDamage())));
//							} else {
//								sender.sendMessage(msg("noGame", true));
//							}
//						} else {
//							ParkourGame parkour = ParkourManager.getParkour(args[1]);
//							if (parkour != null) {
//								parkour.getOptions().setAllowingDamage(Boolean.valueOf(args[2]));
//								sender.sendMessage(msg("setAllowingDamage", false).replace("%DAMAGE%", String.valueOf(Boolean.valueOf(args[2]))));
//							} else {
//								sender.sendMessage(msg("noGame", true));
//							}
//						}
//					} else {
//						sender.sendMessage(msg("noPerms", true));
//					}
//				} else if (args[0].equalsIgnoreCase("available") || args[0].equalsIgnoreCase("a")) {
//					if (!(sender instanceof Player) || sender.hasPermission("ats.parkour.available")) {
//						if (args.length == 1) {
//							sender.sendMessage(msg("usageAvailable", true));
//						} else if (args.length == 2) {
//							ParkourGame parkour = ParkourManager.getParkour(args[1]);
//							if (parkour != null) {
//								sender.sendMessage(msg("currentAvailable", false).replace("%AVAILABLE%", String.valueOf(parkour.getOptions().isAvailable())));
//							} else {
//								sender.sendMessage(msg("noGame", true));
//							}
//						} else {
//							ParkourGame parkour = ParkourManager.getParkour(args[1]);
//							if (parkour != null) {
//								parkour.getOptions().setAvailable(Boolean.valueOf(args[2]));
//								sender.sendMessage(msg("setAvailable", false).replace("%AVAILABLE%", String.valueOf(Boolean.valueOf(args[2]))));
//							} else {
//								sender.sendMessage(msg("noGame", true));
//							}
//						}
//					} else {
//						sender.sendMessage(msg("noPerms", true));
//					}
//				} else if (args[0].equalsIgnoreCase("boat") || args[0].equalsIgnoreCase("b")) {
//					if (!(sender instanceof Player) || sender.hasPermission("ats.parkour.boat")) {
//						if (args.length == 1) {
//							sender.sendMessage(msg("usageBoats", true));
//						} else if (args.length == 2) {
//							ParkourGame parkour = ParkourManager.getParkour(args[1]);
//							if (parkour != null) {
//								sender.sendMessage(msg("currentBoats", false).replace("%BOATS%", String.valueOf(parkour.getOptions().isBoat())));
//							} else {
//								sender.sendMessage(msg("noGame", true));
//							}
//						} else {
//							ParkourGame parkour = ParkourManager.getParkour(args[1]);
//							if (parkour != null) {
//								parkour.getOptions().setBoat(Boolean.valueOf(args[2]));
//								sender.sendMessage(msg("setBoat", false).replace("%BOATS%", String.valueOf(Boolean.valueOf(args[2]))));
//							} else {
//								sender.sendMessage(msg("noGame", true));
//							}
//						}
//					} else {
//						sender.sendMessage(msg("noPerms", true));
//					}
//				} else if (args[0].equalsIgnoreCase("enabled") || args[0].equalsIgnoreCase("p")) {
//					if (!(sender instanceof Player) || sender.hasPermission("ats.parkour.enabled")) {
//						if (args.length == 1) {
//							sender.sendMessage(msg("usageProceedable", true));
//						} else if (args.length == 2) {
//							ParkourGame parkour = ParkourManager.getParkour(args[1]);
//							if (parkour != null) {
//								sender.sendMessage(msg("currentProceedable", false).replace("%PROCEEDABLE%", String.valueOf(parkour.getOptions().isEnabled())));
//							} else {
//								sender.sendMessage(msg("noGame", true));
//							}
//						} else {
//							ParkourGame parkour = ParkourManager.getParkour(args[1]);
//							if (parkour != null) {
//								parkour.getOptions().setEnabled(Boolean.valueOf(args[2]));
//								sender.sendMessage(msg("setEnabled", false).replace("%PROCEEDABLE%", String.valueOf(Boolean.valueOf(args[2]))));
//							} else {
//								sender.sendMessage(msg("noGame", true));
//							}
//						}
//					} else {
//						sender.sendMessage(msg("noPerms", true));
//					}
//				} else if (args[0].equalsIgnoreCase("effect") || args[0].equalsIgnoreCase("e")) {
//					if (!(sender instanceof Player) || sender.hasPermission("ats.parkour.effect")) {
//						if (args.length == 1) {
//							sender.sendMessage(msg("usageEffect", true));
//						} else {
//							ParkourGame parkour = ParkourManager.getParkour(args[1]);
//							if (parkour != null) {
//								Options o = parkour.getOptions();
//								if (args.length == 2) {
//									for (Entry<PotionEffectType, Integer> entry : o.getEffects().entrySet()) {
//										sender.sendMessage(msg("oneEffect", false).replace("%EFFECT%", "" + entry.getKey().getName()).replace("%AMPLIFIER%", "" + entry.getValue()));
//									}
//								} else {
//									PotionEffectType p = PotionEffectType.getByName(args[2]);
//									if (p != null) {
//										if (Data.ALLOWED_EFFECTS.contains(p)) {
//											if (args.length == 3) {
//												sender.sendMessage(msg("currentEffect", false).replace("%EFFECT%", p.getName()).replace("%AMPLIFIER%", "" + o.getEffects().get(p)));
//											} else {
//												try {
//													int a = Integer.parseInt(args[3]);
//													if (a == 0) {
//														o.removeEffect(p);
//														sender.sendMessage(msg("effectRemoved", false).replace("%EFFECT%", p.getName()));
//													} else {
//														o.setEffect(p, a);
//														sender.sendMessage(msg("setEffect", false).replace("%EFFECT%", p.getName()).replace("%AMPLIFIER%", a + ""));
//													}
//												} catch (Exception ex) {
//													sender.sendMessage(msg("noNumber", true));
//												}
//											}
//										} else {
//											sender.sendMessage(msg("disallowedEffect", true));
//										}
//									} else {
//										sender.sendMessage(msg("noEffect", true));
//									}
//								}
//							} else {
//								sender.sendMessage(msg("noGame", true));
//							}
//						}
//					} else {
//						sender.sendMessage(msg("noPerms", true));
//					}
//					// TODO:
//					// } else if (args[0].equalsIgnoreCase("effectregion") ||
//					// args[0].equalsIgnoreCase("er")) {
//					// if (!(sender instanceof Player) || ((Player)
//					// sender).hasPermission("ats.parkour.effectregion")) {
//					// if (args.length==1) {
//					// sender.sendMessage(msgA("usageEffectregion", true));
//					// } else if (args.length==2) {
//					// if (ParkourPlugin.gameExists(args[1])) {
//					// ParkourPlugin pk = ParkourPlugin.getGame(args[1]);
//					//
//					// } else {
//					// sender.sendMessage(msgA("noGame", true));
//					// }
//					// }
//					// } else {
//					// sender.sendMessage(msgA("noPerms", true));
//					// }
//				} else if (args[0].equalsIgnoreCase("modifyInventory") || args[0].equalsIgnoreCase("eq")) {
//					if (!(sender instanceof Player) || sender.hasPermission("ats.parkour.modifyInventory")) {
//						if (args.length == 1) {
//							sender.sendMessage(msg("usageModifyeq", true));
//						} else if (args.length == 2) {
//							ParkourGame parkour = ParkourManager.getParkour(args[1]);
//							if (parkour != null) {
//								sender.sendMessage(msg("currentModifyeq", false).replace("%MODIFYEQ%", String.valueOf(parkour.getOptions().isModifyInventory())));
//							} else {
//								sender.sendMessage(msg("noGame", true));
//							}
//						} else {
//							ParkourGame parkour = ParkourManager.getParkour(args[1]);
//							if (parkour != null) {
//								parkour.getOptions().setModifyInventory(Boolean.valueOf(args[2]));
//								sender.sendMessage(msg("setModifyInventory", false).replace("%MODIFYEQ%", String.valueOf(Boolean.valueOf(args[2]))));
//							} else {
//								sender.sendMessage(msg("noGame", true));
//							}
//						}
//					} else {
//						sender.sendMessage(msg("noPerms", true));
//					}
//				} else if (args[0].equalsIgnoreCase("difficulty") || args[0].equalsIgnoreCase("d")) {
//					if (!(sender instanceof Player) || sender.hasPermission("ats.parkour.difficulty")) {
//						if (args.length == 1) {
//							sender.sendMessage(msg("usageDifficulty", true));
//						} else if (args.length == 2) {
//							ParkourGame parkour = ParkourManager.getParkour(args[1]);
//							if (parkour != null) {
//								sender.sendMessage(msg("currentDifficulty", false).replace("%DIFFICULTY%", String.valueOf(parkour.getOptions().getDifficulty())));
//							} else {
//								sender.sendMessage(msg("noGame", true));
//							}
//						} else {
//							try {
//								ParkourGame parkour = ParkourManager.getParkour(args[1]);
//								if (parkour != null) {
//									parkour.getOptions().setDifficulty(Integer.valueOf(args[2]));
//									sender.sendMessage(msg("setDifficulty", false).replace("%DIFFICULTY%", String.valueOf(Integer.valueOf(args[2]))));
//								} else {
//									sender.sendMessage(msg("noGame", true));
//								}
//							} catch (NumberFormatException e) {
//								sender.sendMessage(msg("numberExpected", true));
//							}
//						}
//					} else {
//						sender.sendMessage(msg("noPerms", true));
//					}
//				} else if (args[0].equalsIgnoreCase("displayname") || args[0].equalsIgnoreCase("dn")) {
//					if (!(sender instanceof Player) || sender.hasPermission("ats.parkour.displayname")) {
//						if (args.length == 1) {
//							sender.sendMessage(msg("usageDisplayname", true));
//						} else if (args.length == 2) {
//							ParkourGame parkour = ParkourManager.getParkour(args[1]);
//							if (parkour != null) {
//								String tmp = msg("currentDisplayname", false).split("%")[0];
//								String color = "\u00A7" + tmp.charAt(tmp.lastIndexOf('\u00A7') + 1);
//								sender.sendMessage(msg("currentDisplayname", false).replace("%DISPLAYNAME%", "\u00A7r" + parkour.getOptions().getDisplayName().replace('&', '\u00A7') + color));
//							} else {
//								sender.sendMessage(msg("noGame", true));
//							}
//						} else {
//							ParkourGame parkour = ParkourManager.getParkour(args[1]);
//							if (parkour != null) {
//								StringBuilder nameBuilder = new StringBuilder();
//								for (int i = 2; i < args.length; i++) {
//									nameBuilder.append(" ").append(args[i]);
//								}
//								String name = nameBuilder.substring(1);
//								name = name.replace('&', '\u00A7');
//								String tmp = msg("setDisplayname", false).split("%")[0];
//								String color = "\u00A7" + tmp.charAt(tmp.lastIndexOf('\u00A7') + 1);
//								parkour.getOptions().setDisplayName(name);
//								sender.sendMessage(msg("setDisplayname", false).replace("%DISPLAYNAME%", name + color));
//							} else {
//								sender.sendMessage(msg("noGame", true));
//							}
//						}
//					} else {
//						sender.sendMessage(msg("noPerms", true));
//					}
//				} else if (args[0].equalsIgnoreCase("bronze")) {
//					if (!(sender instanceof Player) || sender.hasPermission("ats.parkour.bronze")) {
//						if (args.length == 1) {
//							sender.sendMessage(msg("usageBronze", true));
//						} else if (args.length == 2) {
//							ParkourGame parkour = ParkourManager.getParkour(args[1]);
//							if (parkour != null) {
//								sender.sendMessage(msg("currentBronze", false).replace("%BRONZE%", String.valueOf(parkour.getOptions().getBronze())));
//							} else {
//								sender.sendMessage(msg("noGame", true));
//							}
//						} else {
//							try {
//								ParkourGame parkour = ParkourManager.getParkour(args[1]);
//								if (parkour != null) {
//									parkour.getOptions().setBronze(Integer.valueOf(args[2]));
//									sender.sendMessage(msg("setBronze", false).replace("%BRONZE%", String.valueOf(Integer.valueOf(args[2]))));
//								} else {
//									sender.sendMessage(msg("noGame", true));
//								}
//							} catch (NumberFormatException e) {
//								sender.sendMessage(msg("numberExpected", true));
//							}
//						}
//					} else {
//						sender.sendMessage(msg("noPerms", true));
//					}
//				} else if (args[0].equalsIgnoreCase("fair")) {
//					if (!(sender instanceof Player) || sender.hasPermission("ats.parkour.fair")) {
//						if (args.length == 1) {
//							sender.sendMessage(msg("usageFair", true));
//						} else if (args.length == 2) {
//							ParkourGame parkour = ParkourManager.getParkour(args[1]);
//							if (parkour != null) {
//								sender.sendMessage(msg("currentFair", false).replace("%FAIR%", String.valueOf(parkour.getOptions().getFair())));
//							} else {
//								sender.sendMessage(msg("noGame", true));
//							}
//						} else {
//							try {
//								ParkourGame parkour = ParkourManager.getParkour(args[1]);
//								if (parkour != null) {
//									parkour.getOptions().setFair(Float.valueOf(args[2]));
//									sender.sendMessage(msg("setFair", false).replace("%FAIR%", String.valueOf(Float.valueOf(args[2]))));
//								} else {
//									sender.sendMessage(msg("noGame", true));
//								}
//							} catch (NumberFormatException e) {
//								sender.sendMessage(msg("numberExpected", true));
//							}
//						}
//					} else {
//						sender.sendMessage(msg("noPerms", true));
//					}
//				} else if (args[0].equalsIgnoreCase("silver")) {
//					if (!(sender instanceof Player) || sender.hasPermission("ats.parkour.silver")) {
//						if (args.length == 1) {
//							sender.sendMessage(msg("usageSilver", true));
//						} else if (args.length == 2) {
//							ParkourGame parkour = ParkourManager.getParkour(args[1]);
//							if (parkour != null) {
//								sender.sendMessage(msg("currentSilver", false).replace("%SILVER%", String.valueOf(parkour.getOptions().getSilver())));
//							} else {
//								sender.sendMessage(msg("noGame", true));
//							}
//						} else {
//							try {
//								ParkourGame parkour = ParkourManager.getParkour(args[1]);
//								if (parkour != null) {
//									parkour.getOptions().setSilver(Integer.valueOf(args[2]));
//									sender.sendMessage(msg("setSilver", false).replace("%SILVER%", String.valueOf(Integer.valueOf(args[2]))));
//								} else {
//									sender.sendMessage(msg("noGame", true));
//								}
//							} catch (NumberFormatException e) {
//								sender.sendMessage(msg("numberExpected", true));
//							}
//						}
//					} else {
//						sender.sendMessage(msg("noPerms", true));
//					}
//				} else if (args[0].equalsIgnoreCase("gold")) {
//					if (!(sender instanceof Player) || sender.hasPermission("ats.parkour.gold")) {
//						if (args.length == 1) {
//							sender.sendMessage(msg("usageGold", true));
//						} else if (args.length == 2) {
//							ParkourGame parkour = ParkourManager.getParkour(args[1]);
//							if (parkour != null) {
//								sender.sendMessage(msg("currentGold", false).replace("%GOLD%", String.valueOf(parkour.getOptions().getGold())));
//							} else {
//								sender.sendMessage(msg("noGame", true));
//							}
//						} else {
//							try {
//								ParkourGame parkour = ParkourManager.getParkour(args[1]);
//								if (parkour != null) {
//									parkour.getOptions().setGold(Integer.valueOf(args[2]));
//									sender.sendMessage(msg("setGold", false).replace("%GOLD%", String.valueOf(Integer.valueOf(args[2]))));
//								} else {
//									sender.sendMessage(msg("noGame", true));
//								}
//							} catch (NumberFormatException e) {
//								sender.sendMessage(msg("numberExpected", true));
//							}
//						}
//					} else {
//						sender.sendMessage(msg("noPerms", true));
//					}
//				} else if (args[0].equalsIgnoreCase("platinium")) {
//					if (!(sender instanceof Player) || sender.hasPermission("ats.parkour.platinium")) {
//						if (args.length == 1) {
//							sender.sendMessage(msg("usagePlatinium", true));
//						} else if (args.length == 2) {
//							ParkourGame parkour = ParkourManager.getParkour(args[1]);
//							if (parkour != null) {
//								sender.sendMessage(msg("currentPlatinium", false).replace("%PLATINUM%", String.valueOf(parkour.getOptions().getPlatinum())));
//							} else {
//								sender.sendMessage(msg("noGame", true));
//							}
//						} else {
//							try {
//								ParkourGame parkour = ParkourManager.getParkour(args[1]);
//								if (parkour != null) {
//									parkour.getOptions().setPlatinum(Integer.valueOf(args[2]));
//									sender.sendMessage(msg("setPlatnum", false).replace("%PLATINUM%", String.valueOf(Integer.valueOf(args[2]))));
//								} else {
//									sender.sendMessage(msg("noGame", true));
//								}
//							} catch (NumberFormatException e) {
//								sender.sendMessage(msg("numberExpected", true));
//							}
//						}
//					} else {
//						sender.sendMessage(msg("noPerms", true));
//					}
//				} else if (args[0].equalsIgnoreCase("color")) {
//					if (!(sender instanceof Player) || sender.hasPermission("ats.parkour.color")) {
//						if (args.length == 1) {
//							sender.sendMessage(msg("usageColor", true));
//						} else if (args.length == 2) {
//							ParkourGame parkour = ParkourManager.getParkour(args[1]);
//							if (parkour != null) {
//								sender.sendMessage(msg("currentColor", false).replace("%COLOR%", "" + parkour.getOptions().getColor().name()));
//							} else {
//								sender.sendMessage(msg("noGame", true));
//							}
//						} else {
//							ParkourGame parkour = ParkourManager.getParkour(args[1]);
//							if (parkour != null) {
//								try {
//									args[2] = args[2].toUpperCase();
//									parkour.getOptions().setColor(DyeColor.valueOf(args[2]));
//									sender.sendMessage(msg("setColor", false).replace("%COLOR%", "" + DyeColor.valueOf(args[2]).name()));
//								} catch (IllegalArgumentException ex) {
//									sender.sendMessage(msg("wrongColor", true).replace("%COLOR%", args[2]));
//								}
//							} else {
//								sender.sendMessage(msg("noGame", true));
//							}
//						}
//					} else {
//						sender.sendMessage(msg("noPerms", true));
//					}
//				} else if (args[0].equalsIgnoreCase("authors")) {
//					if (!(sender instanceof Player) || sender.hasPermission("ats.parkour.authors")) {
//						if (args.length == 1 || args.length == 3) {
//							sender.sendMessage(msg("usageAuthors", true));
//						} else if (args.length == 2) {
//							ParkourGame parkour = ParkourManager.getParkour(args[1]);
//							if (parkour != null) {
//								sender.sendMessage(msg("currentAuthors", false).replace("%AUTHORS%", "" + parkour.getAuthors()));
//							} else {
//								sender.sendMessage(msg("noGame", true));
//							}
//						} else {
//							ParkourGame parkour = ParkourManager.getParkour(args[1]);
//							if (parkour != null) {
//								if (args[2].equalsIgnoreCase("add")) {
//									parkour.addAuthor(args[3]);
//									sender.sendMessage(msg("addedAuthor", false));
//								} else if (args[2].equalsIgnoreCase("remove")) {
//									if (parkour.removeAuthor(args[3])) {
//										sender.sendMessage(msg("removedAuthor", false));
//									} else {
//										sender.sendMessage(msg("noAuthor", true));
//									}
//								} else {
//									sender.sendMessage(msg("wrongArg", true));
//								}
//							} else {
//								sender.sendMessage(msg("noGame", true));
//							}
//						}
//					} else {
//						sender.sendMessage(msg("noPerms", true));
//					}
//				} else if (args[0].equalsIgnoreCase("type")) {
//					if (!(sender instanceof Player) || sender.hasPermission("ats.parkour.type")) {
//						if (args.length == 1) {
//							sender.sendMessage(msg("usageType", true));
//						} else if (args.length == 2) {
//							ParkourGame parkour = ParkourManager.getParkour(args[1]);
//							if (parkour != null) {
//								sender.sendMessage(msg("currentType", false).replace("%TYPE%", "" + parkour.getOptions().getType().toString()));
//							} else {
//								sender.sendMessage(msg("noGame", true));
//							}
//						} else {
//							ParkourGame parkour = ParkourManager.getParkour(args[1]);
//							if (parkour != null) {
//								try {
//									parkour.getOptions().setType(Parkour.ParkourType.valueOf(args[2]));
//									sender.sendMessage(msg("setType", false).replace("%TYPE%", "" + Parkour.ParkourType.valueOf(args[2]).toString()));
//								} catch (Exception ex) {
//									sender.sendMessage(msg("noType", true));
//								}
//							} else {
//								sender.sendMessage(msg("noGame", true));
//							}
//						}
//					} else {
//						sender.sendMessage(msg("noPerms", true));
//					}
//				} else if (args[0].equalsIgnoreCase("lobby")) {
//					if ((sender instanceof Player) && sender.hasPermission("ats.parkour.lobby")) {
//						Location l = ((Player) sender).getLocation();
//						ParkourManager.setLobbyLocation(l);
//						sender.sendMessage(msg("setLobby", false).replace("%COORDX%", String.valueOf(l.getX())).replace("%COORDY%", String.valueOf(l.getY())).replace("%COORDZ%", String.valueOf(l.getZ())));
//					} else {
//						sender.sendMessage(msg("noPerms", true));
//					}
//				} else if (args[0].equalsIgnoreCase("help") || args[0].equalsIgnoreCase("?")) {
//					if (!(sender instanceof Player) || sender.hasPermission("ats.parkour.help")) {
//						int i = 0;
//						if (args.length != 1) {
//							try {
//								i = 5 * (Integer.parseInt(args[1]) - 1);
//							} catch (NumberFormatException ex) {
//								sender.sendMessage(ChatColor.DARK_RED + ex.getMessage());
//								i = 0;
//							}
//						}
//						Map<String, String> list = plugin.getMessages();
//						int max = i + 5;
//						int value = list.size() / 5 + 1;
//						int p = i / 5 + 1;
//						if (p <= value) {
//							sender.sendMessage(msg("currentPage", false).replace("%PAGE%", String.valueOf(p)).replace("%MAXPAGES%", String.valueOf(value)));
//							List<?> l = new ArrayList<>(list.entrySet());
//							for (; i < max && i < list.size(); i++) {
//								String arg = String.valueOf(l.get(i)).split("=")[0];
//								String desc = String.valueOf(l.get(i)).split("=")[1];
//								sender.sendMessage("§2/pk " + arg + "§r - " + desc);
//							}
//						}
//					} else {
//						sender.sendMessage(msg("noPerms", true));
//					}
//				} else if (args[0].equalsIgnoreCase("rename") || args[0].equalsIgnoreCase("rn")) {
//					if (!(sender instanceof Player) || sender.hasPermission("ats.parkour.rename")) {
//						if (args.length == 3) {
//							ParkourGame parkour1 = ParkourManager.getParkour(args[1]);
//							if (parkour1 != null) {
//								if (ParkourManager.getParkour(args[2]) != null) {
//									parkour1.setName(args[2]);
//									sender.sendMessage(msg("gameRenamed", false).replace("%OLDNAME%", args[1]).replace("%NEWNAME%", args[2]));
//								} else {
//									sender.sendMessage(msg("gameExists", true));
//								}
//							} else {
//								sender.sendMessage(msg("noGame", true));
//							}
//						} else {
//							sender.sendMessage(msg("usageRename", true));
//						}
//					} else {
//						sender.sendMessage(msg("noPerms", true));
//					}
//				} else if (args[0].equalsIgnoreCase("info")) {
//					if (!(sender instanceof Player) || sender.hasPermission("ats.parkour.info")) {
//						if (args.length == 2) {
//							ParkourGame parkour = ParkourManager.getParkour(args[1]);
//							if (parkour != null) {
//								sender.sendMessage(parkour.toString());
//							} else {
//								sender.sendMessage(msg("noGame", true));
//							}
//						} else {
//							sender.sendMessage(msg("usageInfo", true));
//						}
//					} else {
//						sender.sendMessage(msg("noPerms", true));
//					}
//				} else if (args[0].equalsIgnoreCase("teleportblock") || args[0].equalsIgnoreCase("tb")) {
//					if (!(sender instanceof Player) || sender.hasPermission("ats.parkour.teleportblock")) {
//						if (args.length == 2) {
//							ParkourGame parkour = ParkourManager.getParkour(args[1]);
//							if (parkour != null) {
//								parkour.setTeleportBlock(((Player) sender).getTargetBlock((Set<Material>) null, 5).getLocation());
//								sender.sendMessage(msg("setTeleportblock", false).replace("%PARKOUR%", args[1]));
//							} else {
//								sender.sendMessage(msg("noGame", true));
//							}
//						} else {
//							sender.sendMessage(msg("usageTeleportblock", true));
//						}
//					} else {
//						sender.sendMessage(msg("noPerms", true));
//					}
//				} else if (args[0].equalsIgnoreCase("check")) {
//					if ((sender instanceof Player) && sender.hasPermission("ats.parkour.check")) {
//						for (ParkourGame p : ParkourManager.getAllGames()) {
//							for (AbstractRegion r : p.getAllRegions()) {
//								if (r.contains(((Player) sender).getLocation()) && p.getWorld().equals(((Player) sender).getWorld())) {
//									sender.sendMessage(msg("currentParkour", false).replace("%PARKOUR%", p.getName()));
//									break;
//								}
//							}
//						}
//					} else {
//						sender.sendMessage(msg("noPerms", true));
//					}
//				} else if (args[0].equalsIgnoreCase("teleport") || args[0].equalsIgnoreCase("tp")) {
//					if ((sender instanceof Player) && sender.hasPermission("ats.parkour.teleport")) {
//						if (args.length > 1) {
//							ParkourGame parkour = ParkourManager.getParkour(args[1]);
//							if (parkour != null) {
//								parkour.addPlayer((Player) sender);
//								ParkourPlayer p = PlayerManager.getParkourSinglePlayer((Player) sender);
//								p.teleportToSpawn();
//								p.reset();
//							} else {
//								sender.sendMessage(msg("noGame", true));
//							}
//						} else {
//							sender.sendMessage(msg("usageTeleport", true));
//						}
//					} else {
//						sender.sendMessage(msg("noPerms", true));
//					}
//				} else if (args[0].equalsIgnoreCase("group") || args[0].equalsIgnoreCase("g")) {
//					if (!(sender instanceof Player) || sender.hasPermission("ats.parkour.gropu")) {
//						if (args.length == 1) {
//							sender.sendMessage(msg("udageGroup", true));
//						}
//					} else {
//						sender.sendMessage(msg("noPerms", true));
//					}
//				} else if (args[0].equalsIgnoreCase("bestrecord") || args[0].equalsIgnoreCase("br")) {
//					if (!(sender instanceof Player) || sender.hasPermission("ats.parkour.bestrecord")) {
//						if (args.length == 1) {
//							sender.sendMessage(msg("usageBestrecord", true));
//						} else {
//							ParkourGame parkour = ParkourManager.getParkour(args[1]);
//							if (parkour != null) {
//								Location loc = ((Player) sender).getTargetBlock((Set<Material>) null, 5).getLocation();
//								Material m = loc.getBlock().getType();
//								if (m.equals(Material.SIGN) || m.equals(Material.WALL_SIGN) || m.equals(Material.SIGN_POST)) {
//									parkour.setBestRecord(loc);
//									sender.sendMessage(msg("setBestrecord", false).replace("%COORDX%", "" + loc.getX()).replace("%COORDY%", "" + loc.getY()).replace("%COORDZ%", "" + loc.getZ()));
//									try {
//										PreparedStatement stat = plugin.getConnection().prepareStatement(String.format("SELECT time, nick FROM %s WHERE parkour=? ORDER BY time LIMIT 1", Data.TABLE_RECORDS));
//										stat.setString(1, args[1]);
//										ResultSet rs = stat.executeQuery();
//										if (rs.next()) {
//											DataBaseOperations.updateSign(plugin, rs.getString("nick"), rs.getDouble("time"), parkour);
//										} else {
//											DataBaseOperations.updateSign(plugin, "========", 0.00, parkour);
//										}
//									} catch (Exception ex) {
//										Bukkit.getServer().getLogger().throwing(getClass().getName(), "onCommand", ex);
//									}
//								} else {
//									sender.sendMessage(msg("noSign", true));
//								}
//							} else {
//								sender.sendMessage(msg("noGame", true));
//							}
//						}
//					} else {
//						sender.sendMessage(msg("noPerms", true));
//					}
//				} else {
//					sender.sendMessage(msg("wrongArg", true));
//				}
//			}
//		}
//		return true;
//	}
//
//	private String msg(String arg, boolean error) {
//		return plugin.msg(arg, error);
//	}
//}
