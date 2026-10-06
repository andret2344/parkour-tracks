package eu.andret.parkourtracks.command;

import eu.andret.parkourtracks.config.Medal;
import eu.andret.parkourtracks.message.Message;
import eu.andret.parkourtracks.selection.Selection;
import eu.andret.parkourtracks.track.Cuboid;
import eu.andret.parkourtracks.track.MedalThreshold;
import eu.andret.parkourtracks.track.Track;
import eu.andret.parkourtracks.track.TrackOption;
import eu.andret.parkourtracks.track.TrackRules;
import eu.andret.parkourtracks.track.WorldSpot;
import eu.andret.parkourtracks.util.Amounts;
import eu.andret.parkourtracks.util.Ticks;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.CommandPlaceholder;
import revxrsal.commands.annotation.Description;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import revxrsal.commands.bukkit.annotation.CommandPermission;
import revxrsal.commands.command.ExecutableCommand;
import revxrsal.commands.help.Help;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Creating, removing, starting and stopping tracks, the lobby, the overview and reloading.
 */
@Command({"parkourtracks", "ptracks"})
public final class TrackCommand {
	@NotNull
	private final CommandSupport support;

	public TrackCommand(@NotNull final CommandSupport support) {
		this.support = support;
	}

	@CommandPlaceholder
	public void help(@NotNull final CommandSender sender,
					 @NotNull final Help.ChildrenCommands<BukkitCommandActor> commands) {
		for (final ExecutableCommand<BukkitCommandActor> command : commands) {
			if (command.description() == null) {
				sender.sendMessage("/" + command.usage());
			} else {
				sender.sendMessage("/" + command.usage() + " - " + command.description());
			}
		}
	}

	@Subcommand("list")
	@Description("Lists all tracks")
	@CommandPermission(Permissions.EDIT)
	public void list(@NotNull final CommandSender sender) {
		final List<Track> tracks = support.registry().getTracks();
		if (tracks.isEmpty()) {
			support.send(sender, Message.LIST_EMPTY);
			return;
		}
		support.send(sender, Message.LIST_HEADER);
		tracks.forEach(track -> support.send(sender, Message.LIST_ENTRY, CommandSupport.track(track),
				CommandSupport.displayName(track), state(track)));
	}

	@Subcommand("info")
	@Description("Shows everything about a track and what it misses before it can start")
	@CommandPermission(Permissions.EDIT)
	public void info(@NotNull final CommandSender sender, @NotNull final Track track) {
		final TagResolver name = CommandSupport.track(track);
		final Cuboid region = track.getRegion();
		support.send(sender, Message.INFO_HEADER, name, CommandSupport.displayName(track), state(track));
		support.send(sender, Message.INFO_TYPE, CommandSupport.text("type", track.getType().name().toLowerCase(Locale.ROOT)));
		support.send(sender, Message.INFO_REGION, CommandSupport.text("world", track.getWorld()),
				CommandSupport.text("from", region.minX() + ", " + region.minY() + ", " + region.minZ()),
				CommandSupport.text("to", region.maxX() + ", " + region.maxY() + ", " + region.maxZ()));
		support.send(sender, Message.INFO_SPAWN, isSet(track.getSpawn()));
		support.send(sender, Message.INFO_FINISH, isSet(track.getFinish()));
		support.send(sender, Message.INFO_CHECKPOINTS, CommandSupport.text("count", track.getCheckpoints().size()));
		support.send(sender, Message.INFO_WALLS, CommandSupport.text("count", track.getWalls().size()));
		support.send(sender, Message.INFO_LOBBY, isSet(track.getLobby()));
		support.send(sender, Message.INFO_OPTIONS, name);
		TrackOption.ALL.forEach(option -> support.send(sender, Message.INFO_OPTION,
				CommandSupport.text("option", option.getName()), CommandSupport.text("value", option.display(track))));
		support.send(sender, Message.INFO_MEDALS, name);
		for (final Medal medal : support.plugin().getSettings().medals()) {
			final MedalThreshold threshold = track.getMedals().get(medal.key());
			final Component time = threshold != null && threshold.hasTime()
					? Component.text(Ticks.format(threshold.ticks()))
					: support.plugin().getMessages().get(Message.VALUE_NOT_SET);
			support.send(sender, Message.INFO_MEDAL, Placeholder.parsed("medal", medal.displayName()),
					Placeholder.component("time", time),
					CommandSupport.text("reward", Amounts.format(threshold == null ? 0 : threshold.reward())));
		}
		support.send(sender, Message.INFO_EFFECTS, Placeholder.component("effects", effects(track)));
		support.send(sender, Message.INFO_AUTHORS, Placeholder.component("authors", authors(track)));
		final List<TrackRules.Missing> missing = TrackRules.missingForStart(support.registry(), track);
		if (missing.isEmpty()) {
			support.send(sender, Message.INFO_READY);
			return;
		}
		support.send(sender, Message.INFO_MISSING);
		missing.forEach(entry -> support.send(sender, Message.INFO_MISSING_ENTRY,
				Placeholder.component("missing", missing(entry))));
	}

	@Subcommand("create")
	@Description("Creates a stopped track in the WorldEdit selection")
	@CommandPermission(Permissions.EDIT)
	public void create(@NotNull final Player sender, @NotNull final String name) {
		if (!Track.NAME_PATTERN.matcher(name).matches()) {
			throw support.fail(Message.NAME_INVALID, CommandSupport.text("name", name));
		}
		if (support.registry().find(name).isPresent()) {
			throw support.fail(Message.NAME_TAKEN, CommandSupport.text("name", name));
		}
		final Selection selection = support.selection(sender);
		checkRegion(null, selection);
		final Track track = support.registry().create(name, selection.world(), selection.cuboid());
		support.save();
		support.send(sender, Message.CREATED, CommandSupport.track(track));
	}

	@Subcommand("region set")
	@Description("Moves the region of a track to the WorldEdit selection")
	@CommandPermission(Permissions.EDIT)
	public void setRegion(@NotNull final Player sender, @NotNull final Track track) {
		support.requireStopped(track);
		final Selection selection = support.selection(sender);
		checkRegion(track, selection);
		track.setRegion(selection.world(), selection.cuboid());
		support.save();
		support.send(sender, Message.REGION_SET, CommandSupport.track(track));
	}

	@Subcommand("rename")
	@Description("Changes the name of a track used in commands; its results stay with it")
	@CommandPermission(Permissions.EDIT)
	public void rename(@NotNull final CommandSender sender, @NotNull final Track track, @NotNull final String name) {
		support.requireStopped(track);
		if (!Track.NAME_PATTERN.matcher(name).matches()) {
			throw support.fail(Message.NAME_INVALID, CommandSupport.text("name", name));
		}
		final Optional<Track> other = support.registry().find(name);
		if (other.isPresent() && !other.get().equals(track)) {
			throw support.fail(Message.NAME_TAKEN, CommandSupport.text("name", name));
		}
		final String old = track.getName();
		support.registry().rename(track, name);
		support.save();
		support.send(sender, Message.RENAMED, CommandSupport.text("old", old), CommandSupport.track(track));
	}

	@Subcommand("remove")
	@Description("Removes a stopped track; its results are kept")
	@CommandPermission(Permissions.MANAGE)
	public void remove(@NotNull final CommandSender sender, @NotNull final Track track) {
		support.requireStopped(track);
		support.registry().remove(track);
		support.save();
		support.send(sender, Message.REMOVED, CommandSupport.track(track));
	}

	@Subcommand("start")
	@Description("Starts a track, so players can play it")
	@CommandPermission(Permissions.MANAGE)
	public void start(@NotNull final CommandSender sender, @NotNull final Track track) {
		if (track.isRunning()) {
			throw support.fail(Message.ALREADY_RUNNING, CommandSupport.track(track));
		}
		final List<TrackRules.Missing> missing = TrackRules.missingForStart(support.registry(), track);
		if (!missing.isEmpty()) {
			throw support.fail(Message.CANNOT_START, CommandSupport.track(track),
					Placeholder.component("missing", Component.join(
							JoinConfiguration.commas(true),
							missing.stream().map(this::missing).toList())));
		}
		track.setRunning(true);
		support.save();
		support.send(sender, Message.STARTED, CommandSupport.track(track));
	}

	@Subcommand("stop")
	@Description("Stops a track, so it can be edited")
	@CommandPermission(Permissions.MANAGE)
	public void stop(@NotNull final CommandSender sender, @NotNull final Track track) {
		if (!track.isRunning()) {
			throw support.fail(Message.NOT_RUNNING, CommandSupport.track(track));
		}
		track.setRunning(false);
		support.save();
		support.plugin().getGames().stop(track);
		support.send(sender, Message.STOPPED, CommandSupport.track(track));
	}

	@Subcommand("setlobby")
	@Description("Sets the global lobby where you stand")
	@CommandPermission(Permissions.MANAGE)
	public void setLobby(@NotNull final Player sender) {
		support.registry().setLobby(lobbyAt(sender.getLocation()));
		support.save();
		support.send(sender, Message.LOBBY_SET);
	}

	@Subcommand("track lobby set")
	@Description("Sets the own lobby of a track where you stand, used instead of the global one")
	@CommandPermission(Permissions.EDIT)
	public void setTrackLobby(@NotNull final Player sender, @NotNull final Track track) {
		support.requireStopped(track);
		track.setLobby(lobbyAt(sender.getLocation()));
		support.save();
		support.send(sender, Message.TRACK_LOBBY_SET, CommandSupport.track(track));
	}

	@Subcommand("track lobby clear")
	@Description("Makes a track use the global lobby again")
	@CommandPermission(Permissions.EDIT)
	public void clearTrackLobby(@NotNull final CommandSender sender, @NotNull final Track track) {
		support.requireStopped(track);
		track.setLobby(null);
		support.save();
		support.send(sender, Message.TRACK_LOBBY_CLEARED, CommandSupport.track(track));
	}

	@Subcommand("reload")
	@Description("Loads the config and the messages again")
	@CommandPermission(Permissions.MANAGE)
	public void reload(@NotNull final CommandSender sender) {
		try {
			support.plugin().reload();
		} catch (final IllegalArgumentException ex) {
			throw support.fail(Message.RELOAD_FAILED, CommandSupport.text("error", String.valueOf(ex.getMessage())));
		}
		support.send(sender, Message.RELOADED);
	}

	private void checkRegion(@Nullable final Track track, @NotNull final Selection selection) {
		TrackRules.checkRegion(support.registry(), track, selection.world(), selection.cuboid())
				.ifPresent(problem -> {
					if (problem.kind() == TrackRules.RegionProblem.Kind.OVERLAP && problem.overlapping() != null) {
						throw support.fail(Message.REGION_OVERLAP,
								CommandSupport.text("other", problem.overlapping().getName()));
					}
					// Only an existing track has parts to leave out
					throw support.fail(Message.REGION_LEAVES_OUT, CommandSupport.text("track", track == null ? "" : track.getName()));
				});
	}

	/**
	 * A lobby inside a track would send players straight into it.
	 */
	@NotNull
	private WorldSpot lobbyAt(@NotNull final Location location) {
		final WorldSpot lobby = WorldSpot.of(location);
		support.registry().getTracks()
				.stream()
				.filter(track -> track.getWorld().equals(lobby.world()))
				.filter(track -> track.getRegion().contains(location.getX(), location.getY(), location.getZ()))
				.findFirst()
				.ifPresent(track -> {
					throw support.fail(Message.LOBBY_INSIDE_TRACK, CommandSupport.track(track));
				});
		return lobby;
	}

	@NotNull
	private TagResolver state(@NotNull final Track track) {
		return Placeholder.component("state", support.plugin().getMessages()
				.get(track.isRunning() ? Message.STATE_RUNNING : Message.STATE_STOPPED));
	}

	@NotNull
	private TagResolver isSet(@Nullable final Object value) {
		return Placeholder.component("value", support.plugin().getMessages()
				.get(value == null ? Message.VALUE_NOT_SET : Message.VALUE_SET));
	}

	@NotNull
	private Component missing(@NotNull final TrackRules.Missing missing) {
		return support.plugin().getMessages().get(switch (missing) {
			case SPAWN -> Message.MISSING_SPAWN;
			case FINISH -> Message.MISSING_FINISH;
			case LOBBY -> Message.MISSING_LOBBY;
		});
	}

	@NotNull
	private Component effects(@NotNull final Track track) {
		if (track.getEffects().isEmpty()) {
			return support.plugin().getMessages().get(Message.VALUE_NONE);
		}
		return Component.join(JoinConfiguration.commas(true), track.getEffects()
				.stream()
				.map(effect -> support.plugin().getMessages().get(Message.INFO_EFFECT,
						CommandSupport.text("effect", effect.type()),
						CommandSupport.text("level", effect.amplifier() + 1)))
				.toList());
	}

	@NotNull
	private Component authors(@NotNull final Track track) {
		if (track.getAuthors().isEmpty()) {
			return support.plugin().getMessages().get(Message.VALUE_NONE);
		}
		return Component.text(track.getAuthors()
				.stream()
				.map(this::playerName)
				.collect(Collectors.joining(", ")));
	}

	@NotNull
	private String playerName(@NotNull final UUID id) {
		final OfflinePlayer player = support.plugin().getServer().getOfflinePlayer(id);
		return player.getName() == null ? id.toString() : player.getName();
	}
}
