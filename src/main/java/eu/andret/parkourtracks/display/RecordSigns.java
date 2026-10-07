package eu.andret.parkourtracks.display;

import eu.andret.parkourtracks.ParkourTracksPlugin;
import eu.andret.parkourtracks.command.Permissions;
import eu.andret.parkourtracks.message.Message;
import eu.andret.parkourtracks.result.ResultStore;
import eu.andret.parkourtracks.track.Track;
import eu.andret.parkourtracks.util.Ticks;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Signs showing a place of a track's ranking. An admin writes {@code [ptracks]}, the track and the place (1 when
 * empty) on a side; what the sign shows lives in its PDC, so it needs no list in the tracks file. The signs of loaded
 * chunks are indexed by track, so a completion refreshes only the signs of its track.
 */
public final class RecordSigns implements Listener {
	private static final String HEADER = "[ptracks]";
	private static final int MAX_PLACE = 1000;
	private static final int LINES = 4;

	/**
	 * What a record sign shows: the track's ranking place, on one side.
	 */
	public record RecordSign(@NotNull UUID track, int place, @NotNull Side side) {
		@NotNull
		String encode() {
			return track + ";" + place + ";" + side.name();
		}

		@NotNull
		static Optional<RecordSign> decode(@Nullable final String text) {
			if (text == null) {
				return Optional.empty();
			}
			final String[] parts = text.split(";");
			try {
				return Optional.of(new RecordSign(UUID.fromString(parts[0]), Integer.parseInt(parts[1]),
						Side.valueOf(parts[2])));
			} catch (final IllegalArgumentException | ArrayIndexOutOfBoundsException _) {
				return Optional.empty();
			}
		}
	}

	@NotNull
	private final ParkourTracksPlugin plugin;
	@NotNull
	private final NamespacedKey key;
	@NotNull
	private final Map<UUID, Set<Location>> index = new HashMap<>();

	public RecordSigns(@NotNull final ParkourTracksPlugin plugin) {
		this.plugin = plugin;
		key = new NamespacedKey(plugin, "record");
	}

	/**
	 * Indexes and refreshes the signs of every loaded chunk; the chunks loaded before the plugin started never fire a
	 * {@link ChunkLoadEvent} for it.
	 */
	public void loadAll() {
		plugin.getServer().getWorlds()
				.stream()
				.map(World::getLoadedChunks)
				.flatMap(Arrays::stream)
				.forEach(this::load);
	}

	@EventHandler
	public void chunkLoad(@NotNull final ChunkLoadEvent event) {
		load(event.getChunk());
	}

	@EventHandler
	public void chunkUnload(@NotNull final ChunkUnloadEvent event) {
		final Chunk chunk = event.getChunk();
		index.values().forEach(locations -> locations.removeIf(location -> location.getWorld() == chunk.getWorld()
				&& location.getBlockX() >> 4 == chunk.getX() && location.getBlockZ() >> 4 == chunk.getZ()));
	}

	private void load(@NotNull final Chunk chunk) {
		final Set<UUID> tracks = new HashSet<>();
		for (final BlockState state : chunk.getTileEntities()) {
			if (state instanceof final Sign sign) {
				read(sign).ifPresent(recordSign -> {
					index.computeIfAbsent(recordSign.track(), track -> new HashSet<>()).add(sign.getLocation());
					tracks.add(recordSign.track());
				});
			}
		}
		tracks.forEach(this::refresh);
	}

	/**
	 * A side whose first line is {@code [ptracks]} becomes a record sign, when the writer may edit tracks and the
	 * track and the place are right.
	 */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void write(@NotNull final SignChangeEvent event) {
		final String[] lines = event.lines()
				.stream()
				.map(line -> PlainTextComponentSerializer.plainText().serialize(line).trim())
				.toArray(String[]::new);
		if (lines.length < 3 || !lines[0].equalsIgnoreCase(HEADER)) {
			return;
		}
		if (!event.getPlayer().hasPermission(Permissions.EDIT)) {
			plugin.getMessages().send(event.getPlayer(), Message.SIGN_NO_PERMISSION);
			return;
		}
		final Optional<Track> track = plugin.getTrackRegistry().find(lines[1]);
		if (track.isEmpty()) {
			plugin.getMessages().send(event.getPlayer(), Message.TRACK_NOT_FOUND, Placeholder.unparsed("name", lines[1]));
			return;
		}
		final int place = parsePlace(lines[2]);
		if (place == 0) {
			plugin.getMessages().send(event.getPlayer(), Message.SIGN_PLACE_INVALID,
					Placeholder.unparsed("max", String.valueOf(MAX_PLACE)));
			return;
		}
		if (!(event.getBlock().getState() instanceof final Sign sign)) {
			return;
		}
		final RecordSign recordSign = new RecordSign(track.get().getId(), place, event.getSide());
		sign.getPersistentDataContainer().set(key, PersistentDataType.STRING, recordSign.encode());
		sign.update();
		index.computeIfAbsent(recordSign.track(), id -> new HashSet<>()).add(sign.getLocation());
		final List<Component> rendered = render(track.get(), place, null, true);
		for (int i = 0; i < LINES; i++) {
			event.line(i, rendered.get(i));
		}
		plugin.getMessages().send(event.getPlayer(), Message.SIGN_CREATED, Placeholder.unparsed("track", track.get().getName()));
		// The lines the event writes are only a placeholder until the ranking comes back
		plugin.runOnMainThread(() -> refresh(recordSign.track()));
	}

	private static int parsePlace(@NotNull final String text) {
		if (text.isEmpty()) {
			return 1;
		}
		try {
			final int place = Integer.parseInt(text);
			return place >= 1 && place <= MAX_PLACE ? place : 0;
		} catch (final NumberFormatException _) {
			return 0;
		}
	}

	/**
	 * Shows the current ranking on every loaded record sign of the track.
	 */
	public void refresh(@NotNull final UUID track) {
		final Set<Location> locations = index.getOrDefault(track, Set.of());
		Set.copyOf(locations).forEach(location -> {
			final Optional<Sign> sign = findSignAt(location);
			final Optional<RecordSign> recordSign = sign.flatMap(this::read);
			if (recordSign.isEmpty() || !recordSign.get().track().equals(track)) {
				// Broken or rewritten since it was indexed
				locations.remove(location);
				return;
			}
			plugin.getResults().fetchRanked(track, recordSign.get().place())
					.thenAcceptAsync(ranked -> findSignAt(location).ifPresent(current -> show(current, recordSign.get(), ranked.orElse(null))),
							plugin::runOnMainThread);
		});
	}

	private void show(@NotNull final Sign sign, @NotNull final RecordSign recordSign,
			@Nullable final ResultStore.Ranked ranked) {
		final List<Component> lines = plugin.getTrackRegistry().find(recordSign.track())
				.map(value -> render(value, recordSign.place(), ranked, false))
				.orElseGet(() -> List.of(plugin.getMessages().get(Message.SIGN_UNKNOWN_TRACK), Component.empty(), Component.empty(), Component.empty()));
		for (int i = 0; i < LINES; i++) {
			sign.getSide(recordSign.side()).line(i, lines.get(i));
		}
		sign.update();
	}

	@NotNull
	private List<Component> render(@NotNull final Track track, final int place,
			@Nullable final ResultStore.Ranked ranked, final boolean loading) {
		final Component player;
		final Component time;
		if (loading) {
			player = Component.text("...");
			time = Component.empty();
		} else if (ranked == null) {
			player = plugin.getMessages().get(Message.SIGN_NOBODY);
			time = Component.empty();
		} else {
			player = Component.text(getPlayerName(ranked.player()));
			time = Component.text(Ticks.format(ranked.ticks()));
		}
		final TagResolver[] resolvers = {
				Placeholder.unparsed("track", track.getName()),
				Placeholder.component("display-name", MiniMessage.miniMessage().deserialize(track.getDisplayName())),
				Placeholder.unparsed("place", String.valueOf(place)),
				Placeholder.component("player", player),
				Placeholder.component("time", time)
		};
		return List.of(plugin.getMessages().get(Message.SIGN_LINE_1, resolvers),
				plugin.getMessages().get(Message.SIGN_LINE_2, resolvers),
				plugin.getMessages().get(Message.SIGN_LINE_3, resolvers),
				plugin.getMessages().get(Message.SIGN_LINE_4, resolvers));
	}

	@NotNull
	private String getPlayerName(@NotNull final UUID id) {
		final OfflinePlayer player = plugin.getServer().getOfflinePlayer(id);
		final String name = player.getName();
		if (name != null) {
			return name;
		}
		return id.toString().substring(0, 8);
	}

	@NotNull
	private Optional<RecordSign> read(@NotNull final Sign sign) {
		return RecordSign.decode(sign.getPersistentDataContainer().get(key, PersistentDataType.STRING));
	}

	@NotNull
	private static Optional<Sign> findSignAt(@NotNull final Location location) {
		final World world = location.getWorld();
		if (world == null || !world.isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
			return Optional.empty();
		}
		final Block block = location.getBlock();
		return block.getState() instanceof final Sign sign ? Optional.of(sign) : Optional.empty();
	}
}
