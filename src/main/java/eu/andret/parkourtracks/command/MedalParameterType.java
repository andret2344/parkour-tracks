package eu.andret.parkourtracks.command;

import eu.andret.parkourtracks.config.Medal;
import eu.andret.parkourtracks.message.Message;
import org.jetbrains.annotations.NotNull;
import revxrsal.commands.autocomplete.SuggestionProvider;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import revxrsal.commands.node.ExecutionContext;
import revxrsal.commands.parameter.ParameterType;
import revxrsal.commands.stream.MutableStringStream;

import java.util.stream.Collectors;

/**
 * A medal argument, by its key in {@code config.yml}.
 */
public final class MedalParameterType implements ParameterType<BukkitCommandActor, Medal> {
	@NotNull
	private final CommandSupport support;

	public MedalParameterType(@NotNull final CommandSupport support) {
		this.support = support;
	}

	@NotNull
	@Override
	public Medal parse(@NotNull final MutableStringStream input, @NotNull final ExecutionContext<BukkitCommandActor> context) {
		final String key = input.readString();
		return support.plugin().getSettings().findMedal(key)
				.orElseThrow(() -> support.fail(Message.MEDAL_NOT_FOUND, CommandSupport.text("medal", key),
						CommandSupport.text("medals", support.plugin().getSettings().medals().stream()
								.map(Medal::key)
								.collect(Collectors.joining(", ")))));
	}

	@NotNull
	@Override
	public SuggestionProvider<BukkitCommandActor> defaultSuggestions() {
		return context -> support.plugin().getSettings().medals()
				.stream()
				.map(Medal::key)
				.toList();
	}
}
