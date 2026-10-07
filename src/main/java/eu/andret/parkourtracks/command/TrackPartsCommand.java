package eu.andret.parkourtracks.command;

import eu.andret.parkourtracks.message.Message;
import eu.andret.parkourtracks.selection.Selection;
import eu.andret.parkourtracks.track.Checkpoint;
import eu.andret.parkourtracks.track.Spot;
import eu.andret.parkourtracks.track.Track;
import eu.andret.parkourtracks.track.TrackRules;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Description;
import revxrsal.commands.annotation.Optional;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;

import java.util.function.Consumer;

/**
 * The spawn, the finish, the checkpoints and the walls of a track. Positions in commands count from 1.
 */
@Command({"parkourtracks", "ptracks"})
public final class TrackPartsCommand {
	private static final String POSITION = "position";
	@NotNull
	private final CommandSupport support;

	public TrackPartsCommand(@NotNull final CommandSupport support) {
		this.support = support;
	}

	@Subcommand("spawn")
	@Description("Sets the spawn of a track to the WorldEdit selection, players landing where you stand")
	@CommandPermission(Permissions.EDIT)
	public void spawn(@NotNull final Player sender, @NotNull final Track track) {
		setCheckpoint(sender, track, track::setSpawn);
		support.send(sender, Message.SPAWN_SET, CommandSupport.createTrackPlaceholder(track));
	}

	@Subcommand("finish")
	@Description("Sets the finish of a track to the WorldEdit selection, players landing where you stand")
	@CommandPermission(Permissions.EDIT)
	public void finish(@NotNull final Player sender, @NotNull final Track track) {
		setCheckpoint(sender, track, track::setFinish);
		support.send(sender, Message.FINISH_SET, CommandSupport.createTrackPlaceholder(track));
	}

	@Subcommand("checkpoint add")
	@Description("Adds a checkpoint between the spawn and the finish, at the end or at the given position")
	@CommandPermission(Permissions.EDIT)
	public void addCheckpoint(@NotNull final Player sender, @NotNull final Track track,
			@Optional @Nullable final Integer position) {
		final int count = track.getCheckpoints().size();
		final int index = java.util.Optional.ofNullable(position)
				.map(integer -> convertToIndex(integer, count + 1))
				.orElse(count);
		setCheckpoint(sender, track, checkpoint -> track.addCheckpoint(index, checkpoint));
		support.send(sender, Message.CHECKPOINT_ADDED, CommandSupport.createTrackPlaceholder(track),
				CommandSupport.createPlaceholder(POSITION, index + 1));
	}

	@Subcommand("checkpoint set")
	@Description("Replaces a checkpoint with the WorldEdit selection")
	@CommandPermission(Permissions.EDIT)
	public void setCheckpoint(@NotNull final Player sender, @NotNull final Track track, final int position) {
		final int index = convertToIndex(position, track.getCheckpoints().size());
		setCheckpoint(sender, track, checkpoint -> track.setCheckpoint(index, checkpoint));
		support.send(sender, Message.CHECKPOINT_SET, CommandSupport.createTrackPlaceholder(track), CommandSupport.createPlaceholder(POSITION, position));
	}

	@Subcommand("checkpoint remove")
	@Description("Removes a checkpoint")
	@CommandPermission(Permissions.EDIT)
	public void removeCheckpoint(@NotNull final CommandSender sender, @NotNull final Track track, final int position) {
		support.requireStopped(track);
		track.removeCheckpoint(convertToIndex(position, track.getCheckpoints().size()));
		support.save();
		support.send(sender, Message.CHECKPOINT_REMOVED, CommandSupport.createTrackPlaceholder(track),
				CommandSupport.createPlaceholder(POSITION, position));
	}

	@Subcommand("wall add")
	@Description("Adds a wall, which sends players back to their last checkpoint, in the WorldEdit selection")
	@CommandPermission(Permissions.EDIT)
	public void addWall(@NotNull final Player sender, @NotNull final Track track) {
		final Selection selection = getWallSelection(sender, track);
		track.addWall(selection.cuboid());
		support.save();
		support.send(sender, Message.WALL_ADDED, CommandSupport.createTrackPlaceholder(track),
				CommandSupport.createPlaceholder(POSITION, track.getWalls().size()));
	}

	@Subcommand("wall set")
	@Description("Replaces a wall with the WorldEdit selection")
	@CommandPermission(Permissions.EDIT)
	public void setWall(@NotNull final Player sender, @NotNull final Track track, final int position) {
		final int index = convertToIndex(position, track.getWalls().size());
		final Selection selection = getWallSelection(sender, track);
		track.setWall(index, selection.cuboid());
		support.save();
		support.send(sender, Message.WALL_SET, CommandSupport.createTrackPlaceholder(track), CommandSupport.createPlaceholder(POSITION, position));
	}

	@Subcommand("wall remove")
	@Description("Removes a wall")
	@CommandPermission(Permissions.EDIT)
	public void removeWall(@NotNull final CommandSender sender, @NotNull final Track track, final int position) {
		support.requireStopped(track);
		track.removeWall(convertToIndex(position, track.getWalls().size()));
		support.save();
		support.send(sender, Message.WALL_REMOVED, CommandSupport.createTrackPlaceholder(track), CommandSupport.createPlaceholder(POSITION, position));
	}

	/**
	 * Builds a checkpoint from the selection and where the player stands, checks it and hands it to {@code setter}.
	 */
	private void setCheckpoint(@NotNull final Player sender, @NotNull final Track track,
			@NotNull final Consumer<Checkpoint> setter) {
		support.requireStopped(track);
		final Selection selection = support.getSelection(sender);
		final Checkpoint checkpoint = new Checkpoint(selection.cuboid(), Spot.of(sender.getLocation()));
		TrackRules.validateCheckpoint(track, selection.world(), sender.getWorld().getName(), checkpoint)
				.ifPresent(problem -> {
					throw createPlacementFailure(track, problem);
				});
		setter.accept(checkpoint);
		support.save();
	}

	@NotNull
	private Selection getWallSelection(@NotNull final Player sender, @NotNull final Track track) {
		support.requireStopped(track);
		final Selection selection = support.getSelection(sender);
		TrackRules.validateWall(track, selection.world(), selection.cuboid())
				.ifPresent(problem -> {
					throw createPlacementFailure(track, problem);
				});
		return selection;
	}

	@NotNull
	private MessageException createPlacementFailure(@NotNull final Track track,
			@NotNull final TrackRules.PlacementProblem problem) {
		return switch (problem) {
			case OTHER_WORLD -> support.fail(Message.PLACEMENT_OTHER_WORLD, CommandSupport.createPlaceholder("world", track.getWorld()));
			case OUTSIDE_REGION -> support.fail(Message.PLACEMENT_OUTSIDE_REGION, CommandSupport.createTrackPlaceholder(track));
			case SPOT_OUTSIDE_AREA -> support.fail(Message.PLACEMENT_SPOT_OUTSIDE_AREA);
		};
	}

	/**
	 * Turns a position counted from 1 into a list index, refusing positions above {@code max}.
	 */
	private int convertToIndex(final int position, final int max) {
		if (position < 1 || position > max) {
			throw support.fail(Message.POSITION_INVALID, CommandSupport.createPlaceholder("max", max));
		}
		return position - 1;
	}
}
