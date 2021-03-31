/*
 * Copyright Andret (c) 2019-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour;

import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class TutorialPlayer {
	@Getter
	private final Player player;
	int step;
	int last;

	public TutorialPlayer(final Player player) {
		this.player = player;
	}

	public TutorialPlayer next() {
		step++;
		return this;
	}

	public boolean done(final int i, final boolean mistakeInformation) {
		if (i == step) {
			last = step;
			return true;
		}
		if (mistakeInformation) {
			player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&dI guess you have something other to do in tutorial..."));
		}
		return false;
	}

	public TutorialPlayer sendMessage() {
		player.sendMessage(ChatColor.translateAlternateColorCodes('&', getMessage()));
		return this;
	}

	private String getMessage() {
		switch (step) {
			case 0:
				return "&dHello and welcome to the tutorial. I'm going to teach you how to setup correct working parkour. " +
						"First the lobby has to be set up. Let's check if it's done already, type &b/parkour lobby&d!";
			case 1:
				return "&dAs you can see below, no lobby is configured. Go to the location where lobby should appear and type: &b/parkour setLobby&d.";
			case 2:
				return "&dNice, the lobby is already set. Unfortunately, I don't know if it has correct location, if no, " +
						"you can replace it with correct one with &b/parkour setLobby&d command.";
			case 3:
				return "&dGreat, you have set the lobby!";
			case 4:
				return "&dIt's time to create your parkour. Make a &lWorldEdit&r&d selection of whole parkour region and type &b/parkour create &3<name>&d.";

			case 5:
				return "&dCool, our region is set. Now select the spawn region (remember, player will spawn in CENTER of it) " +
						"and execute &b/parkour setSpawn &3<name>&d. " +
						"Remember: &lThe direction you are looking will be also saved and applied after teleporting to spawn region&r&d!";
			case 6:
				return "&dOk, spawn region is set. To have running parkour, at least one checkpoint is needed. " +
						"Remember: &nLast checkpoint is treated as finish&r&d! Now, add one or more checkpoint by selecting " +
						"region and executing &b/parkour addCheckpoint &3<name>&d. Here also direction you are looking is saved!";
			case 7:
				return "&dYou're almost done! Now it's the most difficult part. Try to predict approximate time needed to complete this parkour. " +
						"We have to set up all 4 medals: &4bronze&d, &7silver&d,&e gold&d and&f platinum&d. Execute &b/parkour &4bronze &3<name> &1<time>&d " +
						"providing time with period (e.g. &b/parkour &4bronze &3FancyParkour &110.0&d)!";
			case 8:
				return "&dThat's an interesting choice... Ok, let's go further. Now set silver medal time using &b/parkour &7silver &3<name> &1<time>&d.";
			case 9:
				return "&dCool, now &b/parkour&e gold &3<name> &1<time>&d (I will do better command syntax in the future, I promise).";
			case 10:
				return "&dAnd the final one: &b/parkour&f platinum &3<name> &1<time>&d.";
			case 11:
				return "&dAnd now just start the game with &b/parkour start &3<name>&d.";
			case 12:
				return "&dThat's it, basic configuration of parkour is done. Now I'm leaving you, you can do it on your own. Bye!";
			default:
				return "&dEm... what?";
		}
	}
}
