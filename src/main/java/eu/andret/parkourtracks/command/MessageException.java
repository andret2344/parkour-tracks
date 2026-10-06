package eu.andret.parkourtracks.command;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import revxrsal.commands.command.CommandActor;
import revxrsal.commands.exception.SendableException;

/**
 * Stops a command and sends the sender a message from {@code messages.yml}. Lamp sends it as it is, without any
 * exception handler.
 */
public final class MessageException extends SendableException {
	@NotNull
	private final transient Component message;

	public MessageException(@NotNull final Component message) {
		this.message = message;
	}

	@Override
	public void sendTo(@NotNull final CommandActor actor) {
		// The plugin registers its commands with Bukkit Lamp only, so every actor is a Bukkit one
		((BukkitCommandActor) actor).reply(message);
	}
}
