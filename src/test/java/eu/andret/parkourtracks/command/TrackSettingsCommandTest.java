package eu.andret.parkourtracks.command;

import eu.andret.parkourtracks.helper.PluginTest;
import eu.andret.parkourtracks.track.MedalThreshold;
import eu.andret.parkourtracks.track.SkipMode;
import eu.andret.parkourtracks.track.Track;
import eu.andret.parkourtracks.track.TrackEffect;
import eu.andret.parkourtracks.track.TrackOption;
import eu.andret.parkourtracks.track.TrackType;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class TrackSettingsCommandTest extends PluginTest {
	@Test
	void optionSetsValuesAndSavesThem() {
		// given
		final PlayerMock admin = admin(0, 0, 0);
		final Track track = tower();

		// when
		admin.performCommand("ptracks option tower hardcore true");
		admin.performCommand("ptracks option tower SKIPMODE notify");
		admin.performCommand("ptracks option tower difficulty 5");
		admin.performCommand("ptracks option tower fee 12.5");
		admin.performCommand("ptracks option tower icon ladder");
		admin.performCommand("ptracks option tower permission Tracks.VIP");
		admin.performCommand("ptracks option tower displayName <gold>The Big Tower");
		admin.performCommand("ptracks option tower boatType birch_boat");

		// then
		assertThat(messages(admin)).containsExactly("Set hardcore of track tower to true.",
				"Set skipMode of track tower to notify.", "Set difficulty of track tower to 5.",
				"Set fee of track tower to 12.50.", "Set icon of track tower to ladder.",
				"Set permission of track tower to tracks.vip.",
				"Set displayName of track tower to <gold>The Big Tower.",
				"Set boatType of track tower to birch_boat.");
		assertThat(track.getOptions().isHardcore()).isTrue();
		assertThat(track.getOptions().getSkipMode()).isEqualTo(SkipMode.NOTIFY);
		assertThat(track.getOptions().getDifficulty()).isEqualTo(5);
		assertThat(track.getOptions().getFee()).isEqualTo(12.5);
		assertThat(track.getOptions().getIcon()).isEqualTo(Material.LADDER);
		assertThat(track.getOptions().getPermission()).isEqualTo("tracks.vip");
		assertThat(track.getDisplayName()).isEqualTo("<gold>The Big Tower");
		assertThat(track.getOptions().getBoatType()).isEqualTo(EntityType.BIRCH_BOAT);
		plugin.getTrackRegistry().load();
		assertThat(plugin.getTrackRegistry().find("tower").orElseThrow().getOptions().isHardcore()).isTrue();
	}

	@Test
	void optionClearsOptionalValuesWithNone() {
		// given
		final PlayerMock admin = admin(0, 0, 0);
		final Track track = tower();
		track.getOptions().setIcon(Material.LADDER);
		track.getOptions().setPermission("tracks.vip");

		// when
		admin.performCommand("ptracks option tower icon none");
		admin.performCommand("ptracks option tower permission NONE");

		// then
		assertThat(track.getOptions().getIcon()).isNull();
		assertThat(track.getOptions().getPermission()).isNull();
	}

	@Test
	void optionRejectsInvalidValues() {
		// given
		final PlayerMock admin = admin(0, 0, 0);
		final Track track = tower();

		// when
		admin.performCommand("ptracks option tower hardcore yes");
		admin.performCommand("ptracks option tower difficulty 6");
		admin.performCommand("ptracks option tower difficulty hard");
		admin.performCommand("ptracks option tower fee -1");
		admin.performCommand("ptracks option tower type arena");
		admin.performCommand("ptracks option tower icon water");
		admin.performCommand("ptracks option tower permission no spaces");
		admin.performCommand("ptracks option tower boatType oak_chest_boat");
		admin.performCommand("ptracks option tower speed 3");

		// then
		assertThat(messages(admin)).containsExactly("hardcore has to be true or false.", "difficulty has to be 1-5.",
				"difficulty has to be a number.", "fee has to be >= 0.",
				"type has to be one of: server, training, players.", "icon has to be one of: none, an item.",
				"permission has to be a permission node such as tracks.vip, or none.",
				"boatType has to be one of: " + TrackOption.getBoatTypes().stream().map(type -> type.name().toLowerCase(Locale.ROOT)).collect(Collectors.joining(", ")) + ".",
				"There is no option speed. Options: displayName, type, difficulty, icon, permission, fee, reward, "
						+ "hardcore, skipMode, pauseOnCheckpoints, sprintForced, damageAllowed, enderPearls, boat, "
						+ "boatType, afterFinish.");
		assertThat(track.getOptions().isHardcore()).isFalse();
		assertThat(track.getOptions().getDifficulty()).isEqualTo(1);
		assertThat(track.getType()).isEqualTo(TrackType.SERVER);
	}

	@Test
	void optionWithoutAnOptionListsThemAll() {
		// given
		final PlayerMock admin = admin(0, 0, 0);
		tower();

		// when
		admin.performCommand("ptracks option tower");

		// then
		assertThat(messages(admin)).hasSize(TrackOption.ALL.size() + 1)
				.startsWith("Options (/ptracks option tower <option> <value>):", "- displayName: tower")
				.contains("- hardcore: false", "- skipMode: fail");
	}

	@Test
	void optionWithoutAValueShowsItEvenWhileRunning() {
		// given
		final PlayerMock admin = admin(0, 0, 0);
		final Track track = tower();
		track.getOptions().setHardcore(true);
		track.setRunning(true);

		// when
		admin.performCommand("ptracks option tower hardcore");

		// then
		assertThat(messages(admin)).containsExactly("hardcore of track tower: true");
	}

	@Test
	void boatCannotGoWithSprintForcedOrEnderPearls() {
		// given
		final PlayerMock admin = admin(0, 0, 0);
		final Track track = tower();

		// when
		admin.performCommand("ptracks option tower sprintForced true");
		admin.performCommand("ptracks option tower boat true");
		admin.performCommand("ptracks option tower sprintForced false");
		admin.performCommand("ptracks option tower enderPearls true");
		admin.performCommand("ptracks option tower boat true");
		admin.performCommand("ptracks option tower enderPearls false");
		admin.performCommand("ptracks option tower boat true");
		admin.performCommand("ptracks option tower sprintForced true");
		admin.performCommand("ptracks option tower enderPearls true");

		// then
		assertThat(messages(admin)).containsExactly("Set sprintForced of track tower to true.",
				"boat cannot be turned on together with sprintForced.", "Set sprintForced of track tower to false.",
				"Set enderPearls of track tower to true.", "boat cannot be turned on together with enderPearls.",
				"Set enderPearls of track tower to false.", "Set boat of track tower to true.",
				"sprintForced cannot be turned on together with boat.",
				"enderPearls cannot be turned on together with boat.");
		assertThat(track.getOptions().isBoat()).isTrue();
		assertThat(track.getOptions().isSprintForced()).isFalse();
		assertThat(track.getOptions().isEnderPearls()).isFalse();
	}

	@Test
	void editLockRejectsSettingsOfARunningTrack() {
		// given
		final PlayerMock admin = admin(0, 0, 0);
		final Track track = tower();
		track.setRunning(true);

		// when
		admin.performCommand("ptracks option tower hardcore true");
		admin.performCommand("ptracks medal time tower gold 30");
		admin.performCommand("ptracks effect set tower speed 1");

		// then
		assertThat(messages(admin)).hasSize(3).allMatch("Track tower is running. Stop it before changing it."::equals);
		assertThat(track.getOptions().isHardcore()).isFalse();
		assertThat(track.getMedals()).isEmpty();
		assertThat(track.getEffects()).isEmpty();
	}

	@Test
	void medalTimesAndRewardsAreSetSeparately() {
		// given
		final PlayerMock admin = admin(0, 0, 0);
		final Track track = tower();

		// when
		admin.performCommand("ptracks medal reward tower gold 50");
		admin.performCommand("ptracks medal time tower gold 30.5");
		admin.performCommand("ptracks medal reward tower gold 75");

		// then
		assertThat(messages(admin)).containsExactly("Set the reward of Gold on track tower to 50.00.",
				"Set the time of Gold on track tower to 00:30.50.", "Set the reward of Gold on track tower to 75.00.");
		assertThat(track.getMedals()).containsEntry("gold", new MedalThreshold(610, 75));
	}

	@Test
	void aBetterMedalNeedsAShorterTime() {
		// given
		final PlayerMock admin = admin(0, 0, 0);
		final Track track = tower();
		admin.performCommand("ptracks medal time tower gold 30");
		messages(admin);

		// when
		admin.performCommand("ptracks medal time tower platinum 30");
		admin.performCommand("ptracks medal time tower silver 29");
		admin.performCommand("ptracks medal time tower platinum 25");
		admin.performCommand("ptracks medal time tower bronze 40");

		// then
		assertThat(messages(admin)).containsExactly("A better medal needs a shorter time: the time clashes with Gold.",
				"A better medal needs a shorter time: the time clashes with Gold.",
				"Set the time of Platinum on track tower to 00:25.00.",
				"Set the time of Bronze on track tower to 00:40.00.");
		assertThat(track.getMedals()).containsOnlyKeys("gold", "platinum", "bronze");
	}

	@Test
	void medalValuesAreChecked() {
		// given
		final PlayerMock admin = admin(0, 0, 0);
		final Track track = tower();

		// when
		admin.performCommand("ptracks medal time tower gold 0");
		admin.performCommand("ptracks medal time tower gold soon");
		admin.performCommand("ptracks medal reward tower gold -5");
		admin.performCommand("ptracks medal reward tower gold lots");
		admin.performCommand("ptracks medal time tower wood 30");

		// then
		assertThat(messages(admin)).containsExactly(
				"The time has to be a positive number of seconds, such as 30.5.",
				"The time has to be a positive number of seconds, such as 30.5.",
				"The reward has to be a number, 0 or more.", "The reward has to be a number, 0 or more.",
				"There is no medal wood. Medals: platinum, gold, silver, bronze.");
		assertThat(track.getMedals()).isEmpty();
	}

	@Test
	void medalRemoveDropsTheThreshold() {
		// given
		final PlayerMock admin = admin(0, 0, 0);
		final Track track = tower();
		track.setMedal("gold", new MedalThreshold(600, 10));

		// when
		admin.performCommand("ptracks medal remove tower gold");

		// then
		assertThat(messages(admin)).containsExactly("Removed Gold from track tower.");
		assertThat(track.getMedals()).isEmpty();
	}

	@Test
	void effectsAreSetByLevelAndRemoved() {
		// given
		final PlayerMock admin = admin(0, 0, 0);
		final Track track = tower();

		// when
		admin.performCommand("ptracks effect set tower jump_boost 2");
		admin.performCommand("ptracks effect set tower minecraft:speed 1");
		admin.performCommand("ptracks effect remove tower SPEED");

		// then
		assertThat(messages(admin)).containsExactly("Players on track tower get minecraft:jump_boost 2.",
				"Players on track tower get minecraft:speed 1.", "Players on track tower no longer get minecraft:speed.");
		assertThat(track.getEffects()).containsExactly(new TrackEffect("minecraft:jump_boost", 1));
	}

	@Test
	void effectsMustExist() {
		// given
		final PlayerMock admin = admin(0, 0, 0);
		tower();

		// when
		admin.performCommand("ptracks effect set tower flying 1");
		admin.performCommand("ptracks effect remove tower speed");

		// then
		assertThat(messages(admin)).containsExactly("There is no effect flying.",
				"Track tower has no effect minecraft:speed.");
	}

	@Test
	void authorsOnlyOnTracksOfTypePlayers() {
		// given
		final PlayerMock admin = admin(0, 0, 0);
		final PlayerMock builder = server.addPlayer("Builder");
		final Track track = tower();

		// when
		admin.performCommand("ptracks author add tower Builder");

		// then
		assertThat(messages(admin)).containsExactly("Only tracks of type players have authors. "
				+ "Set the type with /ptracks option tower type players.");
		assertThat(track.getAuthors()).isEmpty();
		assertThat(builder.getName()).isEqualTo("Builder");
	}

	@Test
	void authorsAreAddedAndRemoved() {
		// given
		final PlayerMock admin = admin(0, 0, 0);
		final PlayerMock builder = server.addPlayer("Builder");
		final Track track = tower();
		track.setType(TrackType.PLAYERS);

		// when
		admin.performCommand("ptracks author add tower Builder");
		admin.performCommand("ptracks author add tower Builder");
		admin.performCommand("ptracks author add tower Nobody");
		final List<UUID> authors = List.copyOf(track.getAuthors());
		admin.performCommand("ptracks author remove tower Builder");
		admin.performCommand("ptracks author remove tower Builder");

		// then
		assertThat(authors).containsExactly(builder.getUniqueId());
		assertThat(track.getAuthors()).isEmpty();
		assertThat(messages(admin)).containsExactly("Added Builder to the authors of track tower.",
				"Builder is already an author of track tower.",
				"No player named Nobody has ever played on this server.",
				"Removed Builder from the authors of track tower.", "Builder is not an author of track tower.");
	}

	@Test
	void completesTracksOptionsValuesMedalsAndEffects() {
		// given
		final PlayerMock admin = admin(0, 0, 0);
		tower();

		// when
		final List<String> tracks = server.getCommandMap().tabComplete(admin, "ptracks option ");
		final List<String> options = server.getCommandMap().tabComplete(admin, "ptracks option tower ");
		final List<String> values = server.getCommandMap().tabComplete(admin, "ptracks option tower skipMode ");
		final List<String> medals = server.getCommandMap().tabComplete(admin, "ptracks medal time tower ");
		final List<String> effects = server.getCommandMap().tabComplete(admin, "ptracks effect set tower ");

		// then
		assertThat(tracks).contains("tower");
		assertThat(options).contains("hardcore", "skipMode", "boat");
		assertThat(values).containsExactlyInAnyOrder("allow", "notify", "fail");
		assertThat(medals).containsExactlyInAnyOrder("platinum", "gold", "silver", "bronze");
		assertThat(effects).contains("speed", "jump_boost");
	}
}
