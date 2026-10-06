package eu.andret.parkourtracks.display;

import eu.andret.parkourtracks.ParkourTracksPlugin;
import eu.andret.parkourtracks.command.Permissions;
import eu.andret.parkourtracks.message.Message;
import eu.andret.parkourtracks.track.Checkpoint;
import eu.andret.parkourtracks.track.Track;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Labels floating over the spawn, the checkpoints and the finish of stopped tracks, for those editing them: only
 * players with {@code parkourtracks.edit} see them. They are never saved with the world; they are made again when
 * anything about the tracks changes and when a chunk of a track loads.
 */
public final class Markers implements Listener {
	private static final double HEIGHT = 1;

	@NotNull
	private final ParkourTracksPlugin plugin;
	@NotNull
	private final List<Entity> markers = new ArrayList<>();

	public Markers(@NotNull final ParkourTracksPlugin plugin) {
		this.plugin = plugin;
	}

	/**
	 * Removes every marker and makes those of the stopped tracks again.
	 */
	public void refresh() {
		removeAll();
		plugin.getTrackRegistry().getTracks()
				.stream()
				.filter(track -> !track.isRunning())
				.forEach(this::show);
	}

	public void removeAll() {
		markers.forEach(Entity::remove);
		markers.clear();
	}

	private void show(@NotNull final Track track) {
		final World world = plugin.getServer().getWorld(track.getWorld());
		if (world == null) {
			return;
		}
		if (track.getSpawn() != null) {
			mark(world, track.getSpawn(), label(Message.MARKER_SPAWN, track, 0));
		}
		for (int i = 0; i < track.getCheckpoints().size(); i++) {
			mark(world, track.getCheckpoints().get(i), label(Message.MARKER_CHECKPOINT, track, i + 1));
		}
		if (track.getFinish() != null) {
			mark(world, track.getFinish(), label(Message.MARKER_FINISH, track, 0));
		}
	}

	@NotNull
	private Component label(@NotNull final Message message, @NotNull final Track track, final int number) {
		return plugin.getMessages().get(message, Placeholder.unparsed("track", track.getName()),
				Placeholder.unparsed("number", String.valueOf(number)));
	}

	private void mark(@NotNull final World world, @NotNull final Checkpoint checkpoint, @NotNull final Component text) {
		final Location location = checkpoint.spot().toLocation(world).add(0, HEIGHT, 0);
		// Spawning in an unloaded chunk would load it; the chunk's own load brings the marker
		if (!world.isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
			return;
		}
		// An invisible marker armor stand: its name always faces the player looking at it
		final ArmorStand marker = world.spawn(location, ArmorStand.class, stand -> {
			stand.setPersistent(false);
			stand.setMarker(true);
			stand.setInvisible(true);
			stand.setGravity(false);
			stand.setInvulnerable(true);
			stand.customName(text);
			stand.setCustomNameVisible(true);
		});
		plugin.getServer().getOnlinePlayers()
				.stream()
				.filter(player -> !player.hasPermission(Permissions.EDIT))
				.forEach(player -> player.hideEntity(plugin, marker));
		markers.add(marker);
	}

	@EventHandler
	public void join(@NotNull final PlayerJoinEvent event) {
		final Player player = event.getPlayer();
		if (!player.hasPermission(Permissions.EDIT)) {
			markers.forEach(marker -> player.hideEntity(plugin, marker));
		}
	}

	@EventHandler
	public void chunkLoad(@NotNull final ChunkLoadEvent event) {
		final String world = event.getWorld().getName();
		final int minX = event.getChunk().getX() << 4;
		final int minZ = event.getChunk().getZ() << 4;
		final boolean hasTrack = plugin.getTrackRegistry().getTracks()
				.stream()
				.filter(track -> !track.isRunning() && track.getWorld().equals(world))
				.anyMatch(track -> track.getRegion().minX() <= minX + 15 && track.getRegion().maxX() >= minX
						&& track.getRegion().minZ() <= minZ + 15 && track.getRegion().maxZ() >= minZ);
		if (hasTrack) {
			refresh();
		}
	}
}
