/*
 * Copyright Andret (c) 2019-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

public class M {
	private static final String FIX = "fix";
	private static final String IGNORE = "ignore";
	private static final String LOBBY = "lobby";
	private static final String HELP = "help";
	private static final String SUCCESS = "success";
	private static final String CREATE = "create";
	private static final String INFO = "info";
	private static final String RECREATE = "recreate";
	private static final String RECORDS_BLOCK = "records-block";
	private static final String REMOVE = "remove";
	private static final String RENAME = "rename";
	private static final String SPAWN = "spawn";
	private static final String START = "start";
	private static final String STOP = "stop";
	private static final String TELEPORT = "teleport";
	private static final String TELEPORT_BLOCK = "teleport-block";
	private static final String USAGE = "usage";
	private static final String ALWAYS_SPAWN = "always-spawn";
	private static final String BOAT = "boat";
	private static final String COLOR = "color";
	private static final String DAMAGE_ALLOWED = "damage-allowed";
	private static final String DIFFICULTY = "difficulty";
	private static final String ENABLED = "enabled";
	private static final String MODIFY_INVENTORY = "modify-inventory";
	private static final String SAVING_RESULTS = "saving-results";
	private static final String SPRINT_FORCED = "sprint-forced";
	private static final String TYPE = "type";
	private static final String VIP_ONLY = "vip-only";
	private static final String GET = "get";
	private static final String SET = "set";
	private static final String AUTHORS = "authors";
	private static final String DISPLAY_NAME = "display-name";
	private static final String EFFECT = "effect";
	private static final String TOP = "top";
	private static final String GAME = "game";
	private static final String EMPTY = "empty";
	private static final String HEADER = "header";
	private static final String ITEM = "item";
	private static final String ADD = "add";
	private static final String ADDED = "added";
	private static final String REMOVED = "removed";
	private static final String ALREADY_EXISTS = "already-exists";
	private static final String ALREADY_STARTED = "already-started";
	private static final String ALREADY_STOPPED = "already-stopped";
	private static final String FORBIDDEN_FLYING = "forbidden-flying";
	private static final String FORBIDDEN_MODIFICATION = "forbidden-modification";
	private static final String INSUFFICIENT_PERMISSIONS = "insufficient-permissions";
	private static final String INVALID_ARGUMENT = "invalid-argument";
	private static final String INVALID_COLOR = "invalid-color";
	private static final String INVALID_EFFECT = "invalid-effect";
	private static final String INVALID_GAME = "invalid-game";
	private static final String INVALID_NAME = "invalid-name";
	private static final String INVALID_SELECTION = "invalid-selection";
	private static final String INVALID_TYPE = "invalid-type";
	private static final String MISSING_CHECKPOINT = "missing-checkpoint";
	private static final String MISSING_LOBBY = "missing-lobby";
	private static final String MISSING_MEDALS = "missing-medals";
	private static final String MISSING_SPAWN = "missing-spawn";
	private static final String NEGATIVE_NUMBER = "negative-number";
	private static final String NOT_BLOCK = "not-block";
	private static final String NOT_CONNECTED = "not-connected";
	private static final String NOT_SIGN = "not-sign";
	private static final String NOT_VIP = "not-vip";
	private static final String TOO_LARGE_NUMBER = "too-large-number";
	private static final String SET_LOBBY = "set-lobby";
	private static final String NO_ECONOMY = "no-economy";
	private static final String NO_RANKS = "no-economy";
	private static final String NO_MEDAL = "no-medal";
	private static final String NO_MEDAL_DATA = "no-medal-data";
	private static final String MEDAL = "medal";
	private static final String BOUNDS_EXCEEDED = "bounds-exceeded";
	private static final String FEE = "fee";
	private static final String REWARD = "reward";

	@AllArgsConstructor
	private static class Section {
		@NotNull
		protected final String key;
	}

	@AllArgsConstructor
	public static class Message {
		@NotNull
		private final Section parent;
		@NotNull
		private final String key;
		@Getter
		private final boolean error;

		public Message(@NotNull final Section parent, @NotNull final String key) {
			this(parent, key, false);
		}

		@NotNull
		@Override
		public String toString() {
			return parent.key + "." + key;
		}
	}

	public static final class General extends Section {
		public static final General FIX = new General(M.FIX);
		public static final General IGNORE = new General(M.IGNORE);
		public static final General LOBBY = new General(M.LOBBY);
		public static final General SET_LOBBY = new General(M.SET_LOBBY);

		public final Message helpMessage = new Message(this, HELP);
		public final Message success = new Message(this, SUCCESS);

		public General(@NotNull final String key) {
			super("general." + key);
		}
	}

	public static final class Executive extends Section {
		public static final Executive CREATE = new Executive(M.CREATE);
		public static final Executive INFO = new Executive(M.INFO);
		public static final Executive RECREATE = new Executive(M.RECREATE);
		public static final Executive RECORDS_BLOCK = new Executive(M.RECORDS_BLOCK);
		public static final Executive REMOVE = new Executive(M.REMOVE);
		public static final Executive RENAME = new Executive(M.RENAME);
		public static final Executive SPAWN = new Executive(M.SPAWN);
		public static final Executive START = new Executive(M.START);
		public static final Executive STOP = new Executive(M.STOP);
		public static final Executive TELEPORT = new Executive(M.TELEPORT);
		public static final Executive TELEPORT_BLOCK = new Executive(M.TELEPORT_BLOCK);

		public final Message helpMessage = new Message(this, HELP);
		public final Message usage = new Message(this, USAGE);
		public final Message success = new Message(this, SUCCESS);

		public Executive(@NotNull final String key) {
			super("executive." + key);
		}
	}

	public static final class Option extends Section {
		public static final Option ALWAYS_SPAWN = new Option(M.ALWAYS_SPAWN);
		public static final Option BOAT = new Option(M.BOAT);
		public static final Option COLOR = new Option(M.COLOR);
		public static final Option DAMAGE_ALLOWED = new Option(M.DAMAGE_ALLOWED);
		public static final Option DIFFICULTY = new Option(M.DIFFICULTY);
		public static final Option ENABLED = new Option(M.ENABLED);
		public static final Option FEE = new Option(M.FEE);
		public static final Option MEDAL = new Option(M.MEDAL);
		public static final Option MODIFY_INVENTORY = new Option(M.MODIFY_INVENTORY);
		public static final Option REWARD = new Option(M.REWARD);
		public static final Option SAVING_RESULTS = new Option(M.SAVING_RESULTS);
		public static final Option SPRINT_FORCED = new Option(M.SPRINT_FORCED);
		public static final Option TYPE = new Option(M.TYPE);
		public static final Option VIP_ONLY = new Option(M.VIP_ONLY);

		public final Message helpMessage = new Message(this, HELP);
		public final Message usage = new Message(this, USAGE);
		public final Message get = new Message(this, GET);
		public final Message set = new Message(this, SET);

		public Option(@NotNull final String key) {
			super("option." + key);
		}
	}

	public static final class Parkour extends Section {
		public static final Parkour AUTHORS = new Parkour(M.AUTHORS);
		public static final Parkour DISPLAY_NAME = new Parkour(M.DISPLAY_NAME);

		public final Message helpMessage = new Message(this, HELP);
		public final Message usage = new Message(this, USAGE);
		public final Message get = new Message(this, GET);
		public final Message set = new Message(this, SET);

		public Parkour(@NotNull final String key) {
			super("parkour." + key);
		}
	}

	public static class List extends Section {
		public static final List EFFECT = new List(M.EFFECT);
		public static final List TOP = new List(M.TOP);
		public static final List GAMES = new List(GAME);
		public static final List HELP = new List(M.HELP);
		public static final List MEDAL = new List(M.MEDAL);

		public final Message helpMessage = new Message(this, M.HELP);
		public final Message usage = new Message(this, USAGE);
		public final Message empty = new Message(this, EMPTY);
		public final Message header = new Message(this, HEADER);
		public final Message item = new Message(this, ITEM);

		public List(@NotNull final String key) {
			super("list." + key);
		}
	}

	public static final class Region {
		public static final class Checkpoint extends Section {
			public static final Checkpoint ADD = new Checkpoint(M.ADD);
			public static final Checkpoint SET = new Checkpoint(M.SET);

			public final Message helpMessage = new Message(this, HELP);
			public final Message usage = new Message(this, USAGE);
			public final Message success = new Message(this, SUCCESS);

			public Checkpoint(@NotNull final String key) {
				super("region.checkpoint." + key);
			}
		}

		public static final class Wall extends Section {
			public static final Wall ADD = new Wall(M.ADD);
			public static final Wall SET = new Wall(M.SET);

			public final Message helpMessage = new Message(this, HELP);
			public final Message usage = new Message(this, USAGE);
			public final Message success = new Message(this, SUCCESS);

			public Wall(@NotNull final String key) {
				super("region.wall." + key);
			}
		}

		private Region() {
		}
	}

	public static final class Amplifier extends Section {
		public static final Amplifier EFFECT = new Amplifier(M.EFFECT);

		public final Message helpMessage = new Message(this, HELP);
		public final Message usage = new Message(this, USAGE);
		public final Message added = new Message(this, ADDED);
		public final Message removed = new Message(this, REMOVED);

		public Amplifier(@NotNull final String key) {
			super("amplifier." + key);
		}
	}

	public static final class Error extends Section {
		public static final Error DEFAULT = new Error("error");

		public final Message alreadyExists = new Message(this, ALREADY_EXISTS, true);
		public final Message alreadyStarted = new Message(this, ALREADY_STARTED, true);
		public final Message alreadyStopped = new Message(this, ALREADY_STOPPED, true);
		public final Message boundsExceeded = new Message(this, BOUNDS_EXCEEDED, true);
		public final Message forbiddenFlying = new Message(this, FORBIDDEN_FLYING, true);
		public final Message forbiddenModification = new Message(this, FORBIDDEN_MODIFICATION, true);
		public final Message insufficientPermissions = new Message(this, INSUFFICIENT_PERMISSIONS, true);
		public final Message invalidArgument = new Message(this, INVALID_ARGUMENT, true);
		public final Message invalidColor = new Message(this, INVALID_COLOR, true);
		public final Message invalidEffect = new Message(this, INVALID_EFFECT, true);
		public final Message invalidGame = new Message(this, INVALID_GAME, true);
		public final Message invalidName = new Message(this, INVALID_NAME, true);
		public final Message invalidSelection = new Message(this, INVALID_SELECTION, true);
		public final Message invalidType = new Message(this, INVALID_TYPE, true);
		public final Message missingCheckpoint = new Message(this, MISSING_CHECKPOINT, true);
		public final Message missingLobby = new Message(this, MISSING_LOBBY, true);
		public final Message missingMedals = new Message(this, MISSING_MEDALS, true);
		public final Message missingSpawn = new Message(this, MISSING_SPAWN, true);
		public final Message negativeNumber = new Message(this, NEGATIVE_NUMBER, true);
		public final Message noEconomy = new Message(this, NO_ECONOMY, true);
		public final Message noRanks = new Message(this, NO_RANKS, true);
		public final Message noMedal = new Message(this, NO_MEDAL, true);
		public final Message noMedalData = new Message(this, NO_MEDAL_DATA, true);
		public final Message notBlock = new Message(this, NOT_BLOCK, true);
		public final Message notConnected = new Message(this, NOT_CONNECTED, true);
		public final Message notSign = new Message(this, NOT_SIGN, true);
		public final Message notVip = new Message(this, NOT_VIP, true);
		public final Message tooLargeNumber = new Message(this, TOO_LARGE_NUMBER, true);

		public Error(@NotNull final String key) {
			super(key);
		}
	}

	private M() {
	}
}
