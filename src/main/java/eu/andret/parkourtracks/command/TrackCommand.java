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
		final List<Track> tracks = support.getRegistry().getTracks();
		if (tracks.isEmpty()) {
			support.send(sender, Message.LIST_EMPTY);
			return;
		}
		support.send(sender, Message.LIST_HEADER);
		tracks.forEach(track -> support.send(sender, Message.LIST_ENTRY, CommandSupport.createTrackPlaceholder(track),
				CommandSupport.createDisplayNamePlaceholder(track), createStatePlaceholder(track)));
	}

	@Subcommand("info")
	@Description("Shows everything about a track and what it misses before it can start")
	@CommandPermission(Permissions.EDIT)
	public void info(@NotNull final CommandSender sender, @NotNull final Track track) {
		final TagResolver name = CommandSupport.createTrackPlaceholder(track);
		final Cuboid region = track.getRegion();
		support.send(sender, Message.INFO_HEADER, name, CommandSupport.createDisplayNamePlaceholder(track), createStatePlaceholder(track));
		support.send(sender, Message.INFO_TYPE, CommandSupport.createPlaceholder("type", track.getType().name().toLowerCase(Locale.ROOT)));
		support.send(sender, Message.INFO_REGION, CommandSupport.createPlaceholder("world", track.getWorld()),
				CommandSupport.createPlaceholder("from", region.minX() + ", " + region.minY() + ", " + region.minZ()),
				CommandSupport.createPlaceholder("to", region.maxX() + ", " + region.maxY() + ", " + region.maxZ()));
		support.send(sender, Message.INFO_SPAWN, describePresence(track.getSpawn()));
		support.send(sender, Message.INFO_FINISH, describePresence(track.getFinish()));
		support.send(sender, Message.INFO_CHECKPOINTS, CommandSupport.createPlaceholder("count", track.getCheckpoints().size()));
		support.send(sender, Message.INFO_WALLS, CommandSupport.createPlaceholder("count", track.getWalls().size()));
		support.send(sender, Message.INFO_LOBBY, describePresence(track.getLobby()));
		support.send(sender, Message.INFO_OPTIONS, name);
		TrackOption.ALL.forEach(option -> support.send(sender, Message.INFO_OPTION,
				CommandSupport.createPlaceholder("option", option.getName()), CommandSupport.createPlaceholder("value", option.formatValue(track))));
		support.send(sender, Message.INFO_MEDALS, name);
		for (final Medal medal : support.getPlugin().getSettings().medals()) {
			final MedalThreshold threshold = track.getMedals().get(medal.key());
			final Component time = threshold != null && threshold.hasTime()
					? Component.text(Ticks.format(threshold.ticks()))
					: support.getPlugin().getMessages().get(Message.VALUE_NOT_SET);
			support.send(sender, Message.INFO_MEDAL, Placeholder.parsed("medal", medal.displayName()),
					Placeholder.component("time", time),
					CommandSupport.createPlaceholder("reward", Amounts.format(threshold == null ? 0 : threshold.reward())));
		}
		support.send(sender, Message.INFO_EFFECTS, Placeholder.component("effects", formatEffects(track)));
		support.send(sender, Message.INFO_AUTHORS, Placeholder.component("authors", formatAuthors(track)));
		final List<TrackRules.Missing> missing = TrackRules.findMissingForStart(support.getRegistry(), track);
		if (missing.isEmpty()) {
			support.send(sender, Message.INFO_READY);
			return;
		}
		support.send(sender, Message.INFO_MISSING);
		missing.forEach(entry -> support.send(sender, Message.INFO_MISSING_ENTRY,
				Placeholder.component("missing", describeMissing(entry))));
	}

	@Subcommand("create")
	@Description("Creates a stopped track in the WorldEdit selection")
	@CommandPermission(Permissions.EDIT)
	public void create(@NotNull final Player sender, @NotNull final String name) {
		if (!Track.NAME_PATTERN.matcher(name).matches()) {
			throw support.fail(Message.NAME_INVALID, CommandSupport.createPlaceholder("name", name));
		}
		if (support.getRegistry().find(name).isPresent()) {
			throw support.fail(Message.NAME_TAKEN, CommandSupport.createPlaceholder("name", name));
		}
		final Selection selection = support.getSelection(sender);
		validateRegion(null, selection);
		final Track track = support.getRegistry().create(name, selection.world(), selection.cuboid());
		support.save();
		support.send(sender, Message.CREATED, CommandSupport.createTrackPlaceholder(track));
	}

	@Subcommand("region set")
	@Description("Moves the region of a track to the WorldEdit selection")
	@CommandPermission(Permissions.EDIT)
	public void setRegion(@NotNull final Player sender, @NotNull final Track track) {
		support.requireStopped(track);
		final Selection selection = support.getSelection(sender);
		validateRegion(track, selection);
		track.setRegion(selection.world(), selection.cuboid());
		support.save();
		support.send(sender, Message.REGION_SET, CommandSupport.createTrackPlaceholder(track));
	}

	@Subcommand("rename")
	@Description("Changes the name of a track used in commands; its results stay with it")
	@CommandPermission(Permissions.EDIT)
	public void rename(@NotNull final CommandSender sender, @NotNull final Track track, @NotNull final String name) {
		support.requireStopped(track);
		if (!Track.NAME_PATTERN.matcher(name).matches()) {
			throw support.fail(Message.NAME_INVALID, CommandSupport.createPlaceholder("name", name));
		}
		final Optional<Track> other = support.getRegistry().find(name);
		if (other.isPresent() && !other.get().equals(track)) {
			throw support.fail(Message.NAME_TAKEN, CommandSupport.createPlaceholder("name", name));
		}
		final String old = track.getName();
		support.getRegistry().rename(track, name);
		support.save();
		support.getPlugin().getRecordSigns().refresh(track.getId());
		support.send(sender, Message.RENAMED, CommandSupport.createPlaceholder("old", old), CommandSupport.createTrackPlaceholder(track));
	}

	@Subcommand("remove")
	@Description("Removes a stopped track; its results are kept")
	@CommandPermission(Permissions.MANAGE)
	public void remove(@NotNull final CommandSender sender, @NotNull final Track track) {
		support.requireStopped(track);
		support.getRegistry().remove(track);
		support.save();
		support.send(sender, Message.REMOVED, CommandSupport.createTrackPlaceholder(track));
	}

	@Subcommand("start")
	@Description("Starts a track, so players can play it")
	@CommandPermission(Permissions.MANAGE)
	public void start(@NotNull final CommandSender sender, @NotNull final Track track) {
		if (track.isRunning()) {
			throw support.fail(Message.ALREADY_RUNNING, CommandSupport.createTrackPlaceholder(track));
		}
		final List<TrackRules.Missing> missing = TrackRules.findMissingForStart(support.getRegistry(), track);
		if (!missing.isEmpty()) {
			throw support.fail(Message.CANNOT_START, CommandSupport.createTrackPlaceholder(track),
					Placeholder.component("missing", Component.join(
							JoinConfiguration.commas(true),
							missing.stream().map(this::describeMissing).toList())));
		}
		track.setRunning(true);
		support.save();
		support.send(sender, Message.STARTED, CommandSupport.createTrackPlaceholder(track));
	}

	@Subcommand("stop")
	@Description("Stops a track, so it can be edited")
	@CommandPermission(Permissions.MANAGE)
	public void stop(@NotNull final CommandSender sender, @NotNull final Track track) {
		if (!track.isRunning()) {
			throw support.fail(Message.NOT_RUNNING, CommandSupport.createTrackPlaceholder(track));
		}
		track.setRunning(false);
		support.save();
		support.getPlugin().getGames().stop(track);
		support.send(sender, Message.STOPPED, CommandSupport.createTrackPlaceholder(track));
	}

	@Subcommand("setlobby")
	@Description("Sets the global lobby where you stand")
	@CommandPermission(Permissions.MANAGE)
	public void setLobby(@NotNull final Player sender) {
		support.getRegistry().setLobby(createLobby(sender.getLocation()));
		support.save();
		support.send(sender, Message.LOBBY_SET);
	}

	@Subcommand("track lobby set")
	@Description("Sets the own lobby of a track where you stand, used instead of the global one")
	@CommandPermission(Permissions.EDIT)
	public void setTrackLobby(@NotNull final Player sender, @NotNull final Track track) {
		support.requireStopped(track);
		track.setLobby(createLobby(sender.getLocation()));
		support.save();
		support.send(sender, Message.TRACK_LOBBY_SET, CommandSupport.createTrackPlaceholder(track));
	}

	@Subcommand("track lobby clear")
	@Description("Makes a track use the global lobby again")
	@CommandPermission(Permissions.EDIT)
	public void clearTrackLobby(@NotNull final CommandSender sender, @NotNull final Track track) {
		support.requireStopped(track);
		track.setLobby(null);
		support.save();
		support.send(sender, Message.TRACK_LOBBY_CLEARED, CommandSupport.createTrackPlaceholder(track));
	}

	@Subcommand("reload")
	@Description("Loads the config and the messages again")
	@CommandPermission(Permissions.MANAGE)
	public void reload(@NotNull final CommandSender sender) {
		try {
			support.getPlugin().reload();
		} catch (final IllegalArgumentException ex) {
			throw support.fail(Message.RELOAD_FAILED, CommandSupport.createPlaceholder("error", String.valueOf(ex.getMessage())));
		}
		support.send(sender, Message.RELOADED);
	}

	private void validateRegion(@Nullable final Track track, @NotNull final Selection selection) {
		TrackRules.validateRegion(support.getRegistry(), track, selection.world(), selection.cuboid())
				.ifPresent(problem -> {
					if (problem.kind() == TrackRules.RegionProblem.Kind.OVERLAP && problem.overlapping() != null) {
						throw support.fail(Message.REGION_OVERLAP,
								CommandSupport.createPlaceholder("other", problem.overlapping().getName()));
					}
					// Only an existing track has parts to leave out
					final String value = Optional.ofNullable(track).map(Track::getName).orElse("");
					throw support.fail(Message.REGION_LEAVES_OUT, CommandSupport.createPlaceholder("track", value));
				});
	}

	/**
	 * A lobby inside a track would send players straight into it.
	 */
	@NotNull
	private WorldSpot createLobby(@NotNull final Location location) {
		final WorldSpot lobby = WorldSpot.of(location);
		support.getRegistry().getTracks()
				.stream()
				.filter(track -> track.getWorld().equals(lobby.world()))
				.filter(track -> track.getRegion().contains(location.getX(), location.getY(), location.getZ()))
				.findFirst()
				.ifPresent(track -> {
					throw support.fail(Message.LOBBY_INSIDE_TRACK, CommandSupport.createTrackPlaceholder(track));
				});
		return lobby;
	}

	@NotNull
	private TagResolver createStatePlaceholder(@NotNull final Track track) {
		return Placeholder.component("state", support.getPlugin().getMessages()
				.get(track.isRunning() ? Message.STATE_RUNNING : Message.STATE_STOPPED));
	}

	@NotNull
	private TagResolver describePresence(@Nullable final Object value) {
		return Placeholder.component("value", support.getPlugin().getMessages()
				.get(value == null ? Message.VALUE_NOT_SET : Message.VALUE_SET));
	}

	@NotNull
	private Component describeMissing(@NotNull final TrackRules.Missing missing) {
		return support.getPlugin().getMessages().get(switch (missing) {
			case SPAWN -> Message.MISSING_SPAWN;
			case FINISH -> Message.MISSING_FINISH;
			case LOBBY -> Message.MISSING_LOBBY;
		});
	}

	@NotNull
	private Component formatEffects(@NotNull final Track track) {
		if (track.getEffects().isEmpty()) {
			return support.getPlugin().getMessages().get(Message.VALUE_NONE);
		}
		return Component.join(JoinConfiguration.commas(true), track.getEffects()
				.stream()
				.map(effect -> support.getPlugin().getMessages().get(Message.INFO_EFFECT,
						CommandSupport.createPlaceholder("effect", effect.type()),
						CommandSupport.createPlaceholder("level", effect.amplifier() + 1)))
				.toList());
	}

	@NotNull
	private Component formatAuthors(@NotNull final Track track) {
		if (track.getAuthors().isEmpty()) {
			return support.getPlugin().getMessages().get(Message.VALUE_NONE);
		}
		return Component.text(track.getAuthors()
				.stream()
				.map(this::getPlayerName)
				.collect(Collectors.joining(", ")));
	}

	@NotNull
	private String getPlayerName(@NotNull final UUID id) {
		final OfflinePlayer player = support.getPlugin().getServer().getOfflinePlayer(id);
		final String name = player.getName();
		if (name != null) {
			return name;
		}
		return id.toString();
	}
}
