/*
 * Copyright Andret (c) 2019-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.sample;

import eu.andret.arguments.AnnotatedCommandExecutor;
import eu.andret.arguments.api.annotation.Argument;
import eu.andret.arguments.api.annotation.BaseCommand;
import eu.andret.arguments.api.entity.ExecutorType;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@BaseCommand("sample")
public class ParkourPluginSampleCommand extends AnnotatedCommandExecutor<ParkourPluginSample> {
	public ParkourPluginSampleCommand(@NotNull final CommandSender sender, @NotNull final ParkourPluginSample plugin) {
		super(sender, plugin);
	}

	@Argument(executorType = ExecutorType.PLAYER)
	public String rank() {
		final boolean vip = plugin.getRankProvider().isVip((Player) sender);
		return vip ? "You're a VIP!" : "You are no or not yet VIP.";
	}

	@Argument(executorType = ExecutorType.PLAYER)
	public String rank(final boolean rank) {
		plugin.getRankProvider().getVips().put(((Player) sender).getUniqueId(), rank);
		return rank ? "You are now a VIP!" : "You are longer VIP.";
	}

	@Argument(executorType = ExecutorType.PLAYER)
	public String balance() {
		final double money = plugin.getFinancialProvider().getMoney((Player) sender);
		return String.format("You have: %s", plugin.getParkourPlugin().formatMoney(money));
	}

	@Argument(executorType = ExecutorType.PLAYER)
	public String balance(final BalanceInteraction interaction, final int amount) {
		return balance(interaction, (double) amount);
	}

	@Argument(executorType = ExecutorType.PLAYER)
	public String balance(final BalanceInteraction interaction, final double amount) {
		final Player player = (Player) sender;
		switch (interaction) {
			case ADD:
				plugin.getFinancialProvider().addMoney(player, amount);
				return "Added " + plugin.getParkourPlugin().formatMoney(amount);
			case SUB:
				plugin.getFinancialProvider().addMoney(player, -amount);
				return "Subtracted " + plugin.getParkourPlugin().formatMoney(amount);
			case SET:
				final double targetAmount = Math.max(amount, 0);
				plugin.getFinancialProvider().getFinances().put(player.getUniqueId(), targetAmount);
				return "Set balance to " + plugin.getParkourPlugin().formatMoney(targetAmount);
			default:
				return null;
		}
	}
}
