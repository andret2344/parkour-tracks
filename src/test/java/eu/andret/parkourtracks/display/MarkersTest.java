package eu.andret.parkourtracks.display;

import eu.andret.parkourtracks.helper.PluginTest;
import eu.andret.parkourtracks.helper.TestTracks;
import eu.andret.parkourtracks.track.Track;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.event.world.ChunkLoadEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class MarkersTest extends PluginTest {
	private Track track;

	@BeforeEach
	void setUpTrack() {
		track = TestTracks.tower(plugin, world);
		track.setRunning(false);
		// The track spans the chunks 0 and 1 along x
		world.getChunkAt(0, 0).load();
		world.getChunkAt(1, 0).load();
	}

	List<ArmorStand> markers() {
		return world.getEntitiesByClass(ArmorStand.class).stream().filter(Entity::isValid).toList();
	}

	List<String> labels() {
		return markers().stream()
				.map(marker -> PlainTextComponentSerializer.plainText().serialize(Objects.requireNonNull(marker.customName())))
				.toList();
	}

	@Test
	void aStoppedTrackShowsItsSpawnCheckpointsAndFinish() {
		// when
		plugin.getMarkers().refresh();

		// then
		assertThat(labels()).containsExactlyInAnyOrder("Spawn of tower", "Checkpoint 1", "Checkpoint 2",
				"Finish of tower");
		assertThat(markers()).allMatch(marker -> !marker.isPersistent() && marker.isMarker() && marker.isInvisible()
				&& marker.isCustomNameVisible());
		assertThat(markers()).map(Entity::getLocation).contains(new Location(world, 2.5, 2, 2.5));
	}

	@Test
	void onlyEditorsSeeThem() {
		// given
		final PlayerMock editor = admin(30, 1, 30);
		final PlayerMock before = server.addPlayer();

		// when
		plugin.getMarkers().refresh();
		final PlayerMock after = server.addPlayer();

		// then
		assertThat(markers()).isNotEmpty()
				.allMatch(editor::canSee)
				.noneMatch(before::canSee)
				.noneMatch(after::canSee);
	}

	@Test
	void aRunningTrackShowsNone() {
		// given
		plugin.getMarkers().refresh();

		// when
		admin(30, 1, 30).performCommand("ptracks start tower");

		// then
		assertThat(markers()).isEmpty();
	}

	@Test
	void changesOfTheTrackShowRightAway() {
		// given
		plugin.getMarkers().refresh();

		// when
		admin(30, 1, 30).performCommand("ptracks checkpoint remove tower 1");

		// then
		assertThat(labels()).containsExactlyInAnyOrder("Spawn of tower", "Checkpoint 1", "Finish of tower");
	}

	@Test
	void markersInUnloadedChunksComeWithTheChunk() {
		// given
		world.getChunkAt(1, 0).unload();
		plugin.getMarkers().refresh();
		final List<String> withoutFinish = labels();

		// when: MockBukkit does not fire the event itself
		world.getChunkAt(1, 0).load();
		server.getPluginManager().callEvent(new ChunkLoadEvent(world.getChunkAt(1, 0), false));

		// then
		assertThat(withoutFinish).doesNotContain("Finish of tower").hasSize(3);
		assertThat(markers()).hasSize(4);
	}

	@Test
	void disablingThePluginRemovesThem() {
		// given
		plugin.getMarkers().refresh();

		// when
		server.getPluginManager().disablePlugin(plugin);

		// then
		assertThat(markers()).isEmpty();
	}
}
