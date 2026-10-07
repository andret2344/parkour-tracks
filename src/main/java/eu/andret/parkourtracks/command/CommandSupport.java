package eu.andret.parkourtracks.command;

import eu.andret.parkourtracks.ParkourTracksPlugin;
import eu.andret.parkourtracks.message.Message;
import eu.andret.parkourtracks.selection.Selection;
import eu.andret.parkourtracks.selection.SelectionException;
import eu.andret.parkourtracks.track.Track;
import eu.andret.parkourtracks.track.TrackRegistry;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * What the command classes share: messages, the edit lock, selections and saving.
 */
@SuppressWarnings("record")
public final class CommandSupport {
	@NotNull
	private final ParkourTracksPlugin plugin;

	public CommandSupport(@NotNull final ParkourTracksPlugin plugin) {
		this.plugin = plugin;
	}

	@NotNull
	public ParkourTracksPlugin getPlugin() {
		return plugin;
	}

	@NotNull
	public TrackRegistry getRegistry() {
		return plugin.getTrackRegistry();
	}

	public void send(@NotNull final CommandSender sender, @NotNull final Message message, @NotNull final TagResolver... resolvers) {
		plugin.getMessages().send(sender, message, resolvers);
	}

	/**
	 * The exception that stops the command with the given message.
	 */
	@NotNull
	public MessageException fail(@NotNull final Message message, @NotNull final TagResolver... resolvers) {
		return new MessageException(plugin.getMessages().get(message, resolvers));
	}

	/**
	 * The edit lock: nobody changes a running track.
	 */
	public void requireStopped(@NotNull final Track track) {
		if (track.isRunning()) {
			throw fail(Message.TRACK_RUNNING, createTrackPlaceholder(track));
		}
	}

	@NotNull
	public Selection getSelection(@NotNull final Player player) {
		try {
			return plugin.getSelections().get(player);
		} catch (final SelectionException ex) {
			if (ex.getReason() == SelectionException.Reason.NOT_CUBOID) {
				throw fail(Message.SELECTION_NOT_CUBOID);
			}
			throw fail(Message.SELECTION_INCOMPLETE);
		}
	}

	/**
	 * Saves the tracks file and shows the change in the markers; called after every change.
	 */
	public void save() {
		getRegistry().save();
		plugin.getMarkers().refresh();
	}

	/**
	 * {@code <track>}: the track's name, as typed in commands.
	 */
	@NotNull
	public static TagResolver createTrackPlaceholder(@NotNull final Track track) {
		return Placeholder.unparsed("track", track.getName());
	}

	/**
	 * {@code <display-name>}: the track's display name, with its formatting.
	 */
	@NotNull
	public static TagResolver createDisplayNamePlaceholder(@NotNull final Track track) {
		return Placeholder.component("display-name", MiniMessage.miniMessage().deserialize(track.getDisplayName()));
	}

	@NotNull
	public static TagResolver createPlaceholder(@NotNull final String name, @NotNull final Object value) {
		return Placeholder.unparsed(name, String.valueOf(value));
	}
}
