package eu.andret.parkourtracks.track;

import com.google.gson.FormattingStyle;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads and writes the tracks file. A write goes to a temporary file first, which then replaces the tracks file in one
 * move, so a crash in the middle never leaves a half-written file behind.
 */
public final class TrackStore {
	@NotNull
	private static final Gson GSON = new GsonBuilder()
			.setFormattingStyle(FormattingStyle.PRETTY.withIndent("\t"))
			.disableHtmlEscaping()
			.create();

	@NotNull
	private final Path file;

	public TrackStore(@NotNull final Path file) {
		this.file = file;
	}

	/**
	 * What the tracks file holds: the global lobby and the tracks.
	 */
	public record Content(@Nullable WorldSpot lobby, @NotNull List<Track> tracks) {
	}

	/**
	 * The shape of the file, kept apart from {@link Content} so Gson fills in what is missing instead of failing.
	 */
	private static final class StoredContent {
		@Nullable
		private WorldSpot lobby;
		@Nullable
		private List<Track> tracks;
	}

	/**
	 * Reads the tracks file; a missing file means no tracks and no lobby.
	 *
	 * @throws IllegalStateException when the file cannot be read or does not hold valid tracks
	 */
	@NotNull
	public Content load() {
		if (!Files.exists(file)) {
			return new Content(null, List.of());
		}
		try (final Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			final StoredContent stored = GSON.fromJson(reader, StoredContent.class);
			if (stored == null) {
				throw new IllegalStateException("The file is empty");
			}
			final List<Track> tracks = stored.tracks == null ? new ArrayList<>() : new ArrayList<>(stored.tracks);
			tracks.forEach(Track::checkLoaded);
			return new Content(stored.lobby, tracks);
		} catch (final IOException | RuntimeException ex) {
			// Gson wraps what a record constructor throws, so the cause at the bottom says what is wrong
			throw new IllegalStateException("Could not read " + file.getFileName() + ": " + rootMessage(ex), ex);
		}
	}

	@Nullable
	private static String rootMessage(@NotNull final Throwable throwable) {
		Throwable cause = throwable;
		while (cause.getCause() != null) {
			cause = cause.getCause();
		}
		return cause.getMessage();
	}

	/**
	 * Replaces the tracks file with the given content.
	 *
	 * @throws IllegalStateException when the file cannot be written; the previous file is then left as it was
	 */
	public void save(@NotNull final Content content) {
		final StoredContent stored = new StoredContent();
		stored.lobby = content.lobby();
		stored.tracks = content.tracks();
		final Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
		try {
			Files.createDirectories(file.toAbsolutePath().getParent());
			try (final Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
				GSON.toJson(stored, writer);
			}
			Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (final IOException ex) {
			throw new IllegalStateException("Could not write " + file.getFileName() + ": " + ex.getMessage(), ex);
		}
	}
}
