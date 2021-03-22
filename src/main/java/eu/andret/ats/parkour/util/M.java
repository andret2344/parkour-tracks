/*
 * Copyright Andret (c) 2019-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.util;

import lombok.AllArgsConstructor;
import lombok.Getter;

public class M {
	@AllArgsConstructor
	private static class Section {
		protected final String key;
	}

	@AllArgsConstructor
	public static class Message {
		private final Section parent;
		private final String key;
		@Getter
		private final boolean error;

		public Message(final Section parent, final String key) {
			this(parent, key, false);
		}

		@Override
		public String toString() {
			return parent.key + "." + key;
		}
	}

	public static final class General extends Section {
		public static final General FIX = new General("fix");
		public static final General IGNORE = new General("ignore");
		public static final General LOBBY = new General("lobby");

		public final Message help = new Message(this, "help");
		public final Message success = new Message(this, "success");

		public General(final String key) {
			super("general." + key);
		}
	}

	public static final class Executive extends Section {
		public static final Executive CREATE = new Executive("create");
		public static final Executive INFO = new Executive("info");
		public static final Executive RECREATE = new Executive("recreate");
		public static final Executive RECORDS_BLOCK = new Executive("records-block");
		public static final Executive REMOVE = new Executive("remove");
		public static final Executive RENAME = new Executive("rename");
		public static final Executive SPAWN = new Executive("spawn");
		public static final Executive START = new Executive("start");
		public static final Executive STOP = new Executive("stop");
		public static final Executive TELEPORT = new Executive("teleport");
		public static final Executive TELEPORT_BLOCK = new Executive("teleport-block");

		public final Message help = new Message(this, "help");
		public final Message usage = new Message(this, "usage");
		public final Message success = new Message(this, "success");

		public Executive(final String key) {
			super("executive." + key);
		}
	}

	public static final class Option extends Section {
		public static final Option ALWAYS_SPAWN = new Option("always-spawn");
		public static final Option BOAT = new Option("boat");
		public static final Option COLOR = new Option("color");
		public static final Option DAMAGE_ALLOWED = new Option("damage-allowed");
		public static final Option DIFFICULTY = new Option("difficulty");
		public static final Option ENABLED = new Option("enabled");
		public static final Option MODIFY_INVENTORY = new Option("modify-inventory");
		public static final Option SAVING_RESULTS = new Option("saving-results");
		public static final Option SPRINT_FORCED = new Option("sprint-forced");
		public static final Option TYPE = new Option("type");
		public static final Option VIP_ONLY = new Option("vip-only");

		public final Message help = new Message(this, "help");
		public final Message usage = new Message(this, "usage");
		public final Message get = new Message(this, "get");
		public final Message set = new Message(this, "set");

		public Option(final String key) {
			super("option." + key);
		}
	}

	public static final class Medal extends Section {
		public static final Medal BRONZE = new Medal("bronze");
		public static final Medal SILVER = new Medal("silver");
		public static final Medal GOLD = new Medal("gold");
		public static final Medal PLATINUM = new Medal("platinum");

		public final Message help = new Message(this, "help");
		public final Message usage = new Message(this, "usage");
		public final Message get = new Message(this, "get");
		public final Message set = new Message(this, "set");

		public Medal(final String key) {
			super("medal." + key);
		}
	}

	public static final class Parkour extends Section {
		public static final Parkour AUTHORS = new Parkour("authors");
		public static final Parkour DISPLAY_NAME = new Parkour("display-name");

		public final Message help = new Message(this, "help");
		public final Message usage = new Message(this, "usage");
		public final Message get = new Message(this, "get");
		public final Message set = new Message(this, "set");

		public Parkour(final String key) {
			super("parkour." + key);
		}
	}

	public static class List extends Section {
		public static final List EFFECT = new List("effect");
		public static final List TOP = new List("top");
		public static final List GAMES = new List("game");
		public static final List HELP = new List("help");

		public final Message help = new Message(this, "help");
		public final Message usage = new Message(this, "usage");
		public final Message empty = new Message(this, "empty");
		public final Message header = new Message(this, "header");
		public final Message item = new Message(this, "item");

		public List(final String key) {
			super("list." + key);
		}
	}

	public static final class Region {
		public static final class Checkpoint extends Section {
			public static final Checkpoint ADD = new Checkpoint("add");
			public static final Checkpoint SET = new Checkpoint("set");

			public final Message help = new Message(this, "help");
			public final Message usage = new Message(this, "usage");
			public final Message success = new Message(this, "success");

			public Checkpoint(final String key) {
				super("region.checkpoint." + key);
			}
		}

		public static final class Wall extends Section {
			public static final Wall ADD = new Wall("add");
			public static final Wall SET = new Wall("set");

			public final Message help = new Message(this, "help");
			public final Message usage = new Message(this, "usage");
			public final Message success = new Message(this, "success");

			public Wall(final String key) {
				super("region.wall." + key);
			}
		}
	}

	public static final class Amplifier extends Section {
		public static final Amplifier EFFECT = new Amplifier("effect");

		public final Message help = new Message(this, "help");
		public final Message usage = new Message(this, "usage");
		public final Message added = new Message(this, "added");
		public final Message removed = new Message(this, "removed");

		public Amplifier(final String key) {
			super("amplifier." + key);
		}
	}

	public static final class Error extends Section {
		public static final Error DEFAULT = new Error("error");

		public final Message alreadyExists = new Message(this, "already-exists", true);
		public final Message alreadyStarted = new Message(this, "already-started", true);
		public final Message alreadyStopped = new Message(this, "already-stopped", true);
		public final Message forbiddenFlying = new Message(this, "forbidden-flying", true);
		public final Message insufficientPermissions = new Message(this, "insufficient-permissions", true);
		public final Message invalidArgument = new Message(this, "invalid-argument", true);
		public final Message invalidColor = new Message(this, "invalid-color", true);
		public final Message invalidEffect = new Message(this, "invalid-effect", true);
		public final Message invalidGame = new Message(this, "invalid-game", true);
		public final Message invalidName = new Message(this, "invalid-name", true);
		public final Message invalidSelection = new Message(this, "invalid-selection", true);
		public final Message invalidType = new Message(this, "invalid-type", true);
		public final Message missingCheckpoint = new Message(this, "missing-checkpoint", true);
		public final Message missingLobby = new Message(this, "missing-lobby", true);
		public final Message missingMedals = new Message(this, "missing-medals", true);
		public final Message missingSpawn = new Message(this, "missing-spawn", true);
		public final Message negativeNumber = new Message(this, "negative-number", true);
		public final Message notBlock = new Message(this, "not-block", true);
		public final Message notConnected = new Message(this, "not-connected", true);
		public final Message notSign = new Message(this, "not-sign", true);
		public final Message notVip = new Message(this, "not-vip", true);
		public final Message tooLargeNumber = new Message(this, "too-large-number", true);

		public Error(final String key) {
			super(key);
		}
	}

	private M() {
	}
}
