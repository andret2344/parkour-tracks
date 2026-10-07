package eu.andret.parkourtracks.command;

import eu.andret.parkourtracks.message.Message;
import eu.andret.parkourtracks.track.TrackOption;
import org.jetbrains.annotations.NotNull;
import revxrsal.commands.autocomplete.SuggestionProvider;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import revxrsal.commands.node.ExecutionContext;
import revxrsal.commands.parameter.ParameterType;
import revxrsal.commands.stream.MutableStringStream;

import java.util.stream.Collectors;

/**
 * A track option argument, by name ignoring case.
 */
public final class OptionParameterType implements ParameterType<BukkitCommandActor, TrackOption<?>> {
	@NotNull
	private final CommandSupport support;

	public OptionParameterType(@NotNull final CommandSupport support) {
		this.support = support;
	}

	@NotNull
	@Override
	public TrackOption<?> parse(@NotNull final MutableStringStream input,
			@NotNull final ExecutionContext<BukkitCommandActor> context) {
		final String name = input.readString();
		return TrackOption.find(name)
				.orElseThrow(() -> support.fail(Message.OPTION_NOT_FOUND, CommandSupport.createPlaceholder("option", name),
						CommandSupport.createPlaceholder("options", TrackOption.ALL.stream()
								.map(TrackOption::getName)
								.collect(Collectors.joining(", ")))));
	}

	@NotNull
	@Override
	public SuggestionProvider<BukkitCommandActor> defaultSuggestions() {
		return _ -> TrackOption.ALL.stream()
				.map(TrackOption::getName)
				.toList();
	}
}
