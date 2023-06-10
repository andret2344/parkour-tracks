/*
 * Copyright Andret (c) 2018. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.tutorial;

import eu.andret.ats.parkour.parkour.Medal;
import eu.andret.ats.parkour.util.Constants;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

@ToString
@EqualsAndHashCode
public final class TutorialPlayer {
	@Getter
	@NotNull
	private final Player player;
	@NotNull
	private final TutorialManager manager;
	private int step;
	private int last;

	public TutorialPlayer(@NotNull final Player player, @NotNull final TutorialManager manager) {
		this.player = player;
		this.manager = manager;
	}

	public TutorialPlayer next() {
		step++;
		return this;
	}

	public boolean done(final int id) {
		return done(id, true);
	}

	public boolean done(final int id, final boolean mistakeInformation) {
		if (id == step) {
			last = step;
			return true;
		}
		if (mistakeInformation) {
			player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&d&lAn&r&d: Didn't I tell you to do something else?"));
		}
		return false;
	}

	@NotNull
	public TutorialPlayer sendMessage() {
		Arrays.stream(getMessage().split("\n"))
				.map(message -> ChatColor.translateAlternateColorCodes('&', message))
				.map(message -> message.replace(Constants.AMP, "&"))
				.forEach(player::sendMessage);
		return this;
	}

	@NotNull
	private String concatenateMedals(final List<String> medals) {
		final int lastIndex = medals.size() - 1;
		return String.join("&d and ", String.join("&d, ", medals.subList(0, lastIndex)), medals.get(lastIndex));
	}

	@NotNull
	private String getMessage() {
		final List<String> medals = manager.getPlugin()
				.getMedals()
				.stream()
				.sorted()
				.map(Medal::displayName)
				.toList();
		final String medal = medals.get(0);

		return switch (step) {
			case 0 ->
					"&dHello and welcome to the &natsParkour setup tutorial&r&d. I'm going to teach you how to correctly setup a working parkour. " +
							"My name is &lAn&r&d, and I will lead you through this tutorial. " +
							"So, at first the parkour's lobby has to be set up. Let's check if it's done already using the command &b/parkour lobby&d.\n" +
							"Sample command execution: &b/parkour lobby";
			case 1 ->
					"&dAs you can see below, the lobby is not yet configured. Go to the location where the lobby should appear and execute &b/parkour setLobby&d.\n" +
							"Sample command execution: &b/parkour setLobby";
			case 2 -> "&dNice, the lobby is already set.\n" +
					"&dNote: &nI can't say if it's the correct location of the lobby. If not, you can reconfigure it with the correct location using &b&n/parkour setLobby&d&n command&r&d.";
			case 3 -> "&dGreat, you have set up the lobby!\n" +
					"&dNote: &nYou can always reconfigure the lobby location using &b&n/parkour setLobby&d&n command again&r&d.";
			case 4 ->
					"&dIt's about time to create your first parkour! Make a &lWorldEdit&r&d selection of the whole parkour region and type &b/parkour create &3<name>&d.\n" +
							"&dNote: &nBear in mind, \"&l<name>&r\"&d&n and other similar components are only placeholders, don't blindly copy them.\n" +
							"&dNote: &nThe name of any parkour can contain only lowercase and uppercase letters, numbers, the underscore sign (_) and the dash sign (-).\n" +
							"Sample command execution: &b/parkour create &3fancy_parkour";
			case 5 ->
					"&dCool, our parkour region is set up. Now make another &lWorldEdit&r&d selection, this time for the parkour spawn region, then execute &b/parkour addCheckpoint &3<name>&d.\n" +
							"&dNote: &nYou are currently adding a checkpoint, but keep in mind that the first checkpoint of the parkour becomes its spawn location&r&d.\n" +
							"&dNote: &nYour position and the direction you are facing will save and apply to players after teleporting to this region&r&d.\n" +
							"Sample command execution: &b/parkour addCheckpoint &3fancy_parkour";
			case 6 ->
					"&dOk, the spawn region is set. To start the parkour, a minimum of two checkpoints is necessary, because the last parkour checkpoint is its endpoint. " +
							"It's your turn now! Add at least one more checkpoint again by selecting a region and executing &b/parkour addCheckpoint &3<name>&d.\n" +
							"&dNote: &nIf you made a mistake, don't worry, just use the alternative command: &b&n/parkour setCheckpoint &3&n<name> &e&n<id>&r&d.\n" +
							"Sample command execution: &b/parkour addCheckpoint &3fancy_parkour\n" +
							"Sample command execution: &b/parkour setCheckpoint &3fancy_parkour &e1";
			case 7 ->
					"&dYou're almost done! Now it's time for the most difficult part. We have to set up all of the medals: " + concatenateMedals(medals) + "&d. " +
							"Execute &b/parkour medal &3<name> &b<medal> &6time &e<value>&d providing time using a decimal point (not a comma).\n" +
							"Sample command execution: &b/parkour medal &3fancy_parkour " + medal + " &6time &e10.0";
			case 8 ->
					"&dThat's an interesting choice... Ok, let's keep going. Now the reward for this medal must be set using &b/parkour medal &3<name> &b<medal> &6reward &e<value>&d.\n" +
							"Sample command execution: &b/parkour medal &3fancy_parkour " + medal + " &6reward &e100.0";
			case 9 ->
					"&dNice! The medal is done. You can now set the remaining medals, as they're obligatory (with the time configured at least), before starting the parkour. " +
							"Once you're done, we need to create a wall. Using &lWorldEdit&r&d, select the ground region and execute &b/parkour addWall &3<name>&r&d.\n" +
							"&dNote: &nThe \"wall\" is a common name for region that will teleport a player back to the last checkpoint or the set spawn&r&d.\n" +
							"&dNote: &nWalls are similar to checkpoints, they also have an alternative command in case of a mistake: &b&n/parkour setWall &3&n<name> &e&n<id>&r&d.\n" +
							"Sample command execution: &b/parkour addWall &3fancy_parkour\n" +
							"Sample command execution: &b/parkour setWall &3fancy_parkour &e1";
			case 10 ->
					"&dPerfect! To fully understand the possibilities of this plugin, you have to understand what the Parkour Options are. " +
							"They look like typical commands, but are totally unnecessary in a basic parkour build. Options consist of:\n" +
							"&6displayName&r: &2String &r(default: &2&lequal to name&r) -&d the name that will be shown to players &7(note: this is never used in commands, it's only a visual change)\n" +
							"&6sprintForced&r: &2boolean &r(default: &2&lfalse&r) -&d sprint cannot be turned off, but when a player pauses in a parkour game, they get teleported back\n" +
							"&6alwaysSpawn&r: &2boolean &r(default: &2&lfalse&r) -&d even if player achieves a checkpoint, hitting a wall will teleport them to the spawn region\n" +
							"&6savingResults&r: &2boolean &r(default: &2&ltrue&r) -&d whether or not the database should store a player's result when they complete the parkour game &7(note: requires the database connection established to function)\n" +
							"&6damageAllowed&r: &2boolean &r(default: &2&lfalse&r) -&d whether or not players should receive any damage during parkour game\n" +
							"&6boat&r: &2boolean &r(default: &2&lfalse&r) -&d whether or not players should start in boats &7(note: be careful, this option is designed to use in water parkour, there is a possibility of side effects)\n" +
							"&6modifyInventory&r: &2boolean &r(default: &2&ltrue&r) -&d whether or not should the player's equipment be modified when joining or leaving game " +
							"&7(note: it means e.g., when joining a game, the inventory is cleared and two obligatory items appear in the hot bar, and once leaving the inventory it is cleared again)\n" +
							"&6vipOnly&r: &2boolean &r(default: &2&ltrue&r) -&d only a VIP player can join the parkour &7(note: requires RankProvider configured to function)\n" +
							"&6fee&r: &2double &r(default: &2&l0.00&r) -&d the amount of money the player needs to pay before entering the parkour &7(note: requires FinancialProvider configured to function)\n" +
							"&6reward&r: &2double &r(default: &2&l0.00&r) -&d the amount of money a player will earn after completing parkour, independently from the medals' rewards &7(note: requires FinancialProvider configured to function)\n" +
							"&6difficulty&r: &2int &r(default: &2&l1&r) -&d the difficulty of the parkour &7(note: this option is mainly used in atsQuickParkour)\n" +
							"&6color&r: &2dye/wool color &r(default: &lWHITE&r) -&d the color representing the parkour, chosen from dye/wool colors &7(note: this option is mainly used in atsQuickParkour)\n" +
							"&6type&r: &2server, training or players &r(default: &2&lserver&r) -&d the type of parkour, from who or for whom it is designed " +
							"&7(note: server - prepared by server admins; training - parkours with neither time nor records; players - parkours authored by players. There is a possibility that in the future there will be more types)&d.\n" +
							"&dWell, is it clear? I strongly hope you got it. Let's configure one of them, let's say... Ah, &ndisplay name&r&d seems perfect to me! Just execute &b/parkour displayName &3<name> &9<displayName>&d.\n" +
							"Sample command execution: &b/parkour displayName &3fancy_parkour&r %AMP%1My %AMP%2Fancy %AMP%3Parkour";
			case 11 ->
					"&dNice, the parkour got a whole new look! Let's see it on the parkour games list, execute command &b/parkour list&d.\n" +
							"Sample command execution: &b/parkour list";
			case 12 ->
					"&dGreat, the parkour's new name is valid and available. Of course you can play with other options, don't hesitate. Once you're done, just start the game with &b/parkour start &3<name>&d.\n" +
							"Sample command execution: &b/parkour start &3fancy_parkour";
			case 13 ->
					"&dAnd... that's it! You have just completed the basic configuration of a parkour game. As you've reached the end of my tutorial, you no longer need me. See ya!\n" +
							"&dNote: &nFor more information type &b&n/parkour&d&n, &b&n/parkour help &5&n[page]&r&d&n, or ask the creator - &6&l&nAndret2344&r&d.\n" +
							"Sample command execution: &b/parkour\n" +
							"Sample command execution: &b/parkour help &53";
			default -> "&dEm... what?";
		};
	}
}
