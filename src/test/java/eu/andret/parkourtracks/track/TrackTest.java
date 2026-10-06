package eu.andret.parkourtracks.track;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TrackTest {
	static Track newTrack() {
		return new Track(UUID.randomUUID(), "tower", "world", new Cuboid(0, 0, 0, 20, 20, 20));
	}

	static Checkpoint checkpoint(final int x) {
		return new Checkpoint(new Cuboid(x, 0, 0, x, 1, 1), new Spot(x + 0.5, 0, 0.5, 0, 0));
	}

	@Test
	void newTrackShowsItsNameAndIsStopped() {
		// when
		final Track track = newTrack();

		// then
		assertThat(track.getDisplayName()).isEqualTo("tower");
		assertThat(track.isRunning()).isFalse();
		assertThat(track.getType()).isEqualTo(TrackType.SERVER);
		assertThat(track.getSpawn()).isNull();
		assertThat(track.getFinish()).isNull();
		assertThat(track.getLobby()).isNull();
	}

	@Test
	void rejectsInvalidName() {
		// given
		final Track track = newTrack();

		// when / then
		assertThatThrownBy(() -> new Track(UUID.randomUUID(), "", "world", new Cuboid(0, 0, 0, 1, 1, 1)))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> track.setName("the tower")).isInstanceOf(IllegalArgumentException.class);
		assertThat(track.getName()).isEqualTo("tower");
	}

	@Test
	void checkpointsKeepTheirOrderAndCanBeInsertedInTheMiddle() {
		// given
		final Track track = newTrack();
		track.addCheckpoint(0, checkpoint(1));
		track.addCheckpoint(1, checkpoint(3));

		// when
		track.addCheckpoint(1, checkpoint(2));
		track.setCheckpoint(2, checkpoint(4));
		track.removeCheckpoint(0);

		// then
		assertThat(track.getCheckpoints()).containsExactly(checkpoint(2), checkpoint(4));
	}

	@Test
	void addingCheckpointsNeverTouchesTheFinish() {
		// given
		final Track track = newTrack();
		track.setFinish(checkpoint(19));

		// when
		track.addCheckpoint(0, checkpoint(5));

		// then
		assertThat(track.getFinish()).isEqualTo(checkpoint(19));
		assertThat(track.getCheckpoints()).containsExactly(checkpoint(5));
	}

	@Test
	void wallsCanBeAddedReplacedAndRemoved() {
		// given
		final Track track = newTrack();
		track.addWall(new Cuboid(0, 0, 0, 1, 1, 1));
		track.addWall(new Cuboid(2, 2, 2, 3, 3, 3));

		// when
		track.setWall(0, new Cuboid(4, 4, 4, 5, 5, 5));
		track.removeWall(1);

		// then
		assertThat(track.getWalls()).containsExactly(new Cuboid(4, 4, 4, 5, 5, 5));
	}

	@Test
	void listsCannotBeChangedFromOutside() {
		// given
		final Track track = newTrack();

		// when / then
		assertThatThrownBy(() -> track.getCheckpoints().add(checkpoint(1))).isInstanceOf(UnsupportedOperationException.class);
		assertThatThrownBy(() -> track.getWalls().clear()).isInstanceOf(UnsupportedOperationException.class);
		assertThatThrownBy(() -> track.getMedals().clear()).isInstanceOf(UnsupportedOperationException.class);
	}

	@Test
	void settingAnEffectReplacesTheOneOfTheSameType() {
		// given
		final Track track = newTrack();
		track.setEffect(new TrackEffect("minecraft:speed", 0));
		track.setEffect(new TrackEffect("minecraft:jump_boost", 0));

		// when
		track.setEffect(new TrackEffect("minecraft:speed", 2));
		track.removeEffect("minecraft:jump_boost");

		// then
		assertThat(track.getEffects()).containsExactly(new TrackEffect("minecraft:speed", 2));
	}

	@Test
	void medalsKeepThresholdsByKey() {
		// given
		final Track track = newTrack();
		track.setMedal("gold", new MedalThreshold(400, 50));
		track.setMedal("silver", new MedalThreshold(600, 0));

		// when
		track.setMedal("gold", new MedalThreshold(300, 50));
		track.removeMedal("silver");

		// then
		assertThat(track.getMedals()).containsOnlyKeys("gold").containsEntry("gold", new MedalThreshold(300, 50));
	}

	@Test
	void authorsAreReplacedAsAWhole() {
		// given
		final Track track = newTrack();
		final UUID first = UUID.randomUUID();
		final UUID second = UUID.randomUUID();
		track.setAuthors(List.of(first));

		// when
		track.setAuthors(List.of(second));

		// then
		assertThat(track.getAuthors()).containsExactly(second);
	}

	@Test
	void setRegionMovesTheTrack() {
		// given
		final Track track = newTrack();

		// when
		track.setRegion("nether", new Cuboid(0, 0, 0, 5, 5, 5));

		// then
		assertThat(track.getWorld()).isEqualTo("nether");
		assertThat(track.getRegion()).isEqualTo(new Cuboid(0, 0, 0, 5, 5, 5));
	}

	@Test
	void tracksAreEqualByIdOnly() {
		// given
		final UUID id = UUID.randomUUID();
		final Track track = new Track(id, "tower", "world", new Cuboid(0, 0, 0, 1, 1, 1));
		final Track same = new Track(id, "cave", "nether", new Cuboid(5, 5, 5, 6, 6, 6));

		// when / then
		assertThat(track).isEqualTo(same).hasSameHashCodeAs(same).isNotEqualTo(newTrack());
		assertThat(track.toString()).contains("tower").contains(id.toString());
	}

	@Test
	void medalThresholdRejectsNegativeValuesAndHasNoTimeAtZero() {
		// when / then
		assertThatThrownBy(() -> new MedalThreshold(-1, 0)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new MedalThreshold(0, -1)).isInstanceOf(IllegalArgumentException.class);
		assertThat(new MedalThreshold(0, 10).hasTime()).isFalse();
		assertThat(new MedalThreshold(1, 10).hasTime()).isTrue();
	}

	@Test
	void effectRejectsNegativeAmplifier() {
		// when / then
		assertThatThrownBy(() -> new TrackEffect("minecraft:speed", -1)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void optionsHaveTheAgreedDefaults() {
		// when
		final TrackOptions options = newTrack().getOptions();

		// then
		assertThat(options.isSprintForced()).isFalse();
		assertThat(options.isHardcore()).isFalse();
		assertThat(options.isDamageAllowed()).isFalse();
		assertThat(options.isBoat()).isFalse();
		assertThat(options.isPauseOnCheckpoints()).isFalse();
		assertThat(options.getSkipMode()).isEqualTo(SkipMode.FAIL);
		assertThat(options.isEnderPearls()).isFalse();
		assertThat(options.getFee()).isZero();
		assertThat(options.getReward()).isZero();
		assertThat(options.getDifficulty()).isEqualTo(1);
		assertThat(options.getIcon()).isNull();
		assertThat(options.getPermission()).isNull();
		assertThat(options.getAfterFinish()).isEqualTo(AfterFinish.LOBBY);
	}

	@Test
	void optionsRejectValuesOutOfRange() {
		// given
		final TrackOptions options = newTrack().getOptions();

		// when / then
		assertThatThrownBy(() -> options.setFee(-1)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> options.setReward(-1)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> options.setDifficulty(0)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> options.setDifficulty(6)).isInstanceOf(IllegalArgumentException.class);
		options.setDifficulty(5);
		assertThat(options.getDifficulty()).isEqualTo(5);
	}

	@Test
	void flagsCanBeSet() {
		// given
		final TrackOptions options = newTrack().getOptions();

		// when
		options.setSprintForced(true);
		options.setHardcore(true);
		options.setDamageAllowed(true);
		options.setPauseOnCheckpoints(true);
		options.setEnderPearls(true);
		options.setReward(5);

		// then
		assertThat(options.isSprintForced()).isTrue();
		assertThat(options.isHardcore()).isTrue();
		assertThat(options.isDamageAllowed()).isTrue();
		assertThat(options.isPauseOnCheckpoints()).isTrue();
		assertThat(options.isEnderPearls()).isTrue();
		assertThat(options.getReward()).isEqualTo(5);
	}
}
