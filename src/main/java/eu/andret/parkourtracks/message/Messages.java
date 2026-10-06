package eu.andret.parkourtracks.message;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.Map;

/**
 * The texts of {@code messages.yml}, MiniMessage with placeholders. A key missing from the admin's file falls back to
 * the shipped file, so new messages of an update show up without editing anything.
 */
public final class Messages {
	@NotNull
	private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

	@NotNull
	private final Map<Message, String> templates;

	private Messages(@NotNull final Map<Message, String> templates) {
		this.templates = templates;
	}

	/**
	 * Reads every message from {@code file}, or from {@code defaults} when the file lacks it.
	 *
	 * @throws IllegalArgumentException when a message is not a text, or is missing from both
	 */
	@NotNull
	public static Messages load(@NotNull final ConfigurationSection file, @NotNull final ConfigurationSection defaults) {
		final Map<Message, String> templates = new EnumMap<>(Message.class);
		for (final Message message : Message.values()) {
			final ConfigurationSection source = file.contains(message.key()) ? file : defaults;
			if (!source.isString(message.key())) {
				throw new IllegalArgumentException("'" + message.key() + "' in messages.yml has to be a text");
			}
			templates.put(message, source.getString(message.key()));
		}
		return new Messages(templates);
	}

	@NotNull
	public Component get(@NotNull final Message message, @NotNull final TagResolver... resolvers) {
		return MINI_MESSAGE.deserialize(templates.get(message), resolvers);
	}

	public void send(@NotNull final CommandSender sender, @NotNull final Message message,
					 @NotNull final TagResolver... resolvers) {
		sender.sendMessage(get(message, resolvers));
	}
}
