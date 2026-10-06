package eu.andret.parkourtracks.result;

import eu.andret.parkourtracks.ParkourTracksPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * Copies the tracks file and the results database into {@code backups/<date and time>/} every few minutes, keeping
 * the newest few. The database is copied by SQLite itself, so the copy is consistent while the plugin runs.
 */
public final class Backups {
	@NotNull
	private static final DateTimeFormatter NAME = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss-SSS", Locale.ROOT);
	private static final long TICKS_PER_MINUTE = 20L * 60;

	@NotNull
	private final ParkourTracksPlugin plugin;
	@NotNull
	private final Path tracksFile;
	@NotNull
	private final Path folder;
	@Nullable
	private BukkitTask task;

	public Backups(@NotNull final ParkourTracksPlugin plugin, @NotNull final Path tracksFile) {
		this.plugin = plugin;
		this.tracksFile = tracksFile;
		folder = plugin.getDataFolder().toPath().resolve("backups");
	}

	/**
	 * (Re)starts the schedule from the settings; a frequency of 0 turns backups off.
	 */
	public void schedule() {
		if (task != null) {
			task.cancel();
			task = null;
		}
		final int minutes = plugin.getSettings().backupFrequencyMinutes();
		if (minutes > 0) {
			task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::backUp,
					minutes * TICKS_PER_MINUTE, minutes * TICKS_PER_MINUTE);
		}
	}

	/**
	 * Makes a backup now; the copying happens on the database thread.
	 */
	@NotNull
	public CompletableFuture<Path> backUp() {
		final Path target = folder.resolve(LocalDateTime.now().format(NAME));
		final int keep = plugin.getSettings().backupKeep();
		try {
			Files.createDirectories(target);
		} catch (final IOException ex) {
			plugin.getLogger().severe("Could not create the backup folder " + target + ": " + ex.getMessage());
			return CompletableFuture.failedFuture(ex);
		}
		return plugin.getResults().backUp(target.resolve("results.db"))
				.thenApply(ignored -> {
					try {
						if (Files.exists(tracksFile)) {
							Files.copy(tracksFile, target.resolve(tracksFile.getFileName()));
						}
						prune(keep);
					} catch (final IOException ex) {
						throw new IllegalStateException(ex);
					}
					plugin.getLogger().info("Backed up the tracks and the results to " + folder.relativize(target));
					return target;
				})
				.whenComplete((ignored, ex) -> {
					if (ex != null) {
						plugin.getLogger().severe("Could not back up: " + ex.getMessage());
					}
				});
	}

	/**
	 * Deletes all but the newest {@code keep} backups; their names sort by date.
	 */
	private void prune(final int keep) throws IOException {
		final List<Path> backups;
		try (final Stream<Path> children = Files.list(folder)) {
			backups = children.filter(Files::isDirectory)
					.sorted(Comparator.comparing(Path::getFileName).reversed())
					.toList();
		}
		for (final Path old : backups.subList(Math.min(keep, backups.size()), backups.size())) {
			try (final Stream<Path> files = Files.walk(old)) {
				for (final Path file : files.sorted(Comparator.reverseOrder()).toList()) {
					Files.delete(file);
				}
			}
		}
	}
}
