package eu.andret.ats.parkour.sample;

import eu.andret.arguments.AnnotatedCommandExecutor;
import eu.andret.arguments.api.annotation.Argument;
import eu.andret.arguments.api.annotation.BaseCommand;
import eu.andret.arguments.api.annotation.Param;
import eu.andret.arguments.api.entity.ExecutorType;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

@BaseCommand("sample")
public class ParkourPluginSampleCommand extends AnnotatedCommandExecutor<ParkourPluginSample> {
	private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("+###,##0.00\u00A4;-###,##0.00\u00A4");

	public ParkourPluginSampleCommand(@NotNull final CommandSender sender, @NotNull final ParkourPluginSample plugin) {
		super(sender, plugin);
		final DecimalFormatSymbols dfs = new DecimalFormatSymbols();
		dfs.setCurrencySymbol("{@}");
		dfs.setGroupingSeparator(' ');
		dfs.setDecimalSeparator('.');
		DECIMAL_FORMAT.setDecimalFormatSymbols(dfs);
		DECIMAL_FORMAT.setGroupingSize(3);
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
		return String.format("You have: %s", DECIMAL_FORMAT.format(money));
	}

	@Argument(executorType = ExecutorType.PLAYER)
	public String balance(@Param("interaction") final BalanceInteraction interaction, final double amount) {
		final Player player = (Player) sender;
		switch (interaction) {
			case ADD:
				plugin.getFinancialProvider().addMoney(player, amount);
				return "Added " + DECIMAL_FORMAT.format(amount);
			case SUB:
				plugin.getFinancialProvider().addMoney(player, -amount);
				return "Subtracted " + DECIMAL_FORMAT.format(amount);
			case SET:
				if (amount < 0) {
					plugin.getFinancialProvider().getFinances().put((player).getUniqueId(), 0D);
					return "Set balance to 0";
				}
				plugin.getFinancialProvider().getFinances().put((player).getUniqueId(), amount);
				return "Set balance to " + DECIMAL_FORMAT.format(amount);
			default:
				return null;
		}
	}
}
