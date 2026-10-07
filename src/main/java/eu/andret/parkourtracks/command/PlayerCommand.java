package eu.andret.parkourtracks.command;

import eu.andret.parkourtracks.game.GameManager;
import eu.andret.parkourtracks.game.LeaveReason;
import eu.andret.parkourtracks.message.Message;
import eu.andret.parkourtracks.track.Track;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Description;
import revxrsal.commands.annotation.Optional;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;

/**
 * The commands of players: entering a track or the lobby, leaving a game, ignoring tracks.
 */
@Command({"parkourtracks", "ptracks"})
public final class PlayerCommand {
	@NotNull
	private final CommandSupport support;
	@NotNull
	private final GameManager games;

	public PlayerCommand(@NotNull final CommandSupport support, @NotNull final GameManager games) {
		this.support = support;
		this.games = games;
	}

	@Subcommand("leave")
	@Description("Leaves the track you are on")
	@CommandPermission(Permissions.PLAY)
	public void leave(@NotNull final Player sender) {
		if (games.getSession(sender).isEmpty()) {
			throw support.fail(Message.NOT_IN_GAME);
		}
		games.leave(sender, LeaveReason.EXIT);
	}

	@Subcommand("enter")
	@Description("Enters a track, or goes to the lobby without one, leaving the track you are on")
	@CommandPermission(Permissions.PLAY)
	public void enter(@NotNull final Player sender, @Optional @Nullable final Track track) {
		if (track != null) {
			if (!track.isRunning()) {
				throw support.fail(Message.NOT_RUNNING, CommandSupport.createTrackPlaceholder(track));
			}
			support.getPlugin().getMenus().choose(sender, track);
			return;
		}
		if (games.getSession(sender).isPresent()) {
			games.leave(sender, LeaveReason.EXIT);
			return;
		}
		final Location lobby = games.findLobby(null).orElseThrow(() -> support.fail(Message.NO_LOBBY));
		games.teleport(sender, lobby);
	}

	@Subcommand("menu")
	@Description("Opens the track selection menu")
	@CommandPermission(Permissions.PLAY)
	public void menu(@NotNull final Player sender) {
		support.getPlugin().getMenus().open(sender);
	}

	@Subcommand("ignore")
	@Description("Switches ignoring tracks, to walk through them without starting a game")
	@CommandPermission(Permissions.IGNORE)
	public void ignore(@NotNull final Player sender) {
		support.send(sender, games.toggleIgnoring(sender) ? Message.IGNORE_ON : Message.IGNORE_OFF);
	}
}
