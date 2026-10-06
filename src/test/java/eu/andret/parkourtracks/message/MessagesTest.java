package eu.andret.parkourtracks.message;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MessagesTest {
	@NotNull
	static YamlConfiguration shipped() throws IOException, InvalidConfigurationException {
		final YamlConfiguration config = new YamlConfiguration();
		try (final InputStream stream = Objects.requireNonNull(MessagesTest.class.getResourceAsStream("/messages.yml"))) {
			config.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
		}
		return config;
	}

	@NotNull
	static YamlConfiguration yaml(@NotNull final String content) throws InvalidConfigurationException {
		final YamlConfiguration config = new YamlConfiguration();
		config.loadFromString(content);
		return config;
	}

	@Test
	void shippedFileHasEveryMessageAndNothingElse() throws IOException, InvalidConfigurationException {
		// given
		final YamlConfiguration shipped = shipped();

		// when / then
		assertThat(shipped.getKeys(false))
				.containsExactlyInAnyOrderElementsOf(Arrays.stream(Message.values()).map(Message::key).toList());
	}

	@Test
	void keyIsTheNameInLowercaseWithDashes() {
		// when / then
		assertThat(Message.TRACK_NOT_FOUND.key()).isEqualTo("track-not-found");
	}

	@Test
	void fillsInPlaceholders() throws IOException, InvalidConfigurationException {
		// given
		final Messages messages = Messages.load(shipped(), shipped());

		// when
		final String text = PlainTextComponentSerializer.plainText()
				.serialize(messages.get(Message.TRACK_NOT_FOUND, Placeholder.unparsed("name", "<b>tower")));

		// then: values are inserted as text, never parsed as tags
		assertThat(text).isEqualTo("There is no track named <b>tower.");
	}

	@Test
	void missingMessagesFallBackToTheShippedOnes() throws IOException, InvalidConfigurationException {
		// given
		final YamlConfiguration file = yaml("created: 'Made <track>!'");

		// when
		final Messages messages = Messages.load(file, shipped());

		// then
		assertThat(PlainTextComponentSerializer.plainText()
				.serialize(messages.get(Message.CREATED, Placeholder.unparsed("track", "tower")))).isEqualTo("Made tower!");
		assertThat(plain(messages, Message.LIST_HEADER)).isEqualTo("Tracks:");
	}

	@Test
	void rejectsMessageThatIsNotAText() throws InvalidConfigurationException {
		// given
		final YamlConfiguration file = yaml("created:\n  text: x");

		// when / then
		assertThatThrownBy(() -> Messages.load(file, shipped()))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("'created'");
	}

	@Test
	void rejectsMessageMissingFromBothFiles() throws InvalidConfigurationException {
		// when / then
		assertThatThrownBy(() -> Messages.load(yaml(""), yaml("")))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@NotNull
	static String plain(@NotNull final Messages messages, @NotNull final Message message) {
		return PlainTextComponentSerializer.plainText().serialize(messages.get(message));
	}
}
