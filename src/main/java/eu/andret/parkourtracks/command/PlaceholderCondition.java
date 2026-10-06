package eu.andret.parkourtracks.command;

import org.jetbrains.annotations.NotNull;
import revxrsal.commands.annotation.CommandPlaceholder;
import revxrsal.commands.command.CommandActor;
import revxrsal.commands.exception.ExpectedLiteralException;
import revxrsal.commands.node.ExecutionContext;
import revxrsal.commands.process.CommandCondition;
import revxrsal.commands.stream.MutableStringStream;

/**
 * Stops the {@link CommandPlaceholder} command from running when anything follows the command name.
 * <p>
 * Lamp runs a command that matched only the start of the input, so without this {@code /ptracks info x} with an
 * unknown {@code x} would show the help of {@code /ptracks} instead of the error of the subcommand. Lamp reports an
 * {@link ExpectedLiteralException} only when no other error is left, so the error of the subcommand wins.
 * <p>
 * Typed for any actor, as {@link ExpectedLiteralException} takes a node of {@link CommandActor}.
 */
public final class PlaceholderCondition implements CommandCondition<CommandActor> {
	@Override
	public void test(@NotNull final ExecutionContext<CommandActor> context) {
		if (!context.command().annotations().contains(CommandPlaceholder.class)) {
			return;
		}
		final MutableStringStream input = context.input().toMutableCopy();
		input.readUnquotedString();
		input.skipWhitespace();
		if (!input.hasFinished()) {
			throw new ExpectedLiteralException(input.peekRemaining(), context.command().firstNode());
		}
	}
}
