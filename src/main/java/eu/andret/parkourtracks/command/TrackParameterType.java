package eu.andret.parkourtracks.command;

import eu.andret.parkourtracks.message.Message;
import eu.andret.parkourtracks.track.Track;
import org.jetbrains.annotations.NotNull;
import revxrsal.commands.autocomplete.SuggestionProvider;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import revxrsal.commands.node.ExecutionContext;
import revxrsal.commands.parameter.ParameterType;
import revxrsal.commands.stream.MutableStringStream;

/**
 * A track argument, by name ignoring case.
 */
public final class TrackParameterType implements ParameterType<BukkitCommandActor, Track> {
	@NotNull
	private final CommandSupport support;

	public TrackParameterType(@NotNull final CommandSupport support) {
		this.support = support;
	}

	@NotNull
	@Override
	public Track parse(@NotNull final MutableStringStream input, @NotNull final ExecutionContext<BukkitCommandActor> context) {
		final String name = input.readString();
		return support.registry().find(name)
				.orElseThrow(() -> support.fail(Message.TRACK_NOT_FOUND, CommandSupport.text("name", name)));
	}

	@NotNull
	@Override
	public SuggestionProvider<BukkitCommandActor> defaultSuggestions() {
		return context -> support.registry().getTracks()
				.stream()
				.map(Track::getName)
				.toList();
	}
}
