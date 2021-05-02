package eu.andret.ats.parkour.sample;

import eu.andret.arguments.AnnotatedCommand;
import eu.andret.arguments.CommandManager;
import eu.andret.ats.parkour.ParkourPlugin;
import eu.andret.ats.parkour.sample.provider.SampleFinancialProvider;
import eu.andret.ats.parkour.sample.provider.SampleRankProvider;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

@Getter
public class ParkourPluginSample extends JavaPlugin {
	@NotNull
	SampleFinancialProvider financialProvider = new SampleFinancialProvider();
	@NotNull
	SampleRankProvider rankProvider = new SampleRankProvider();

	@Override
	public void onEnable() {
		getParkourPlugin().setFinancialProvider(financialProvider);
		getParkourPlugin().setRankProvider(rankProvider);
		final AnnotatedCommand command = CommandManager.registerCommand(ParkourPluginSampleCommand.class, this);
		command.addTypeCompleter(BalanceInteraction.class, BalanceInteraction.stringValues());
		command.addArgumentMapper("interaction", BalanceInteraction.class, BalanceInteraction::valueOf);
		command.setOnUnknownSubCommandExecutionListener(sender -> sender.sendMessage("/sample rank <value> or /sample balance <interaction> <amount>"));
	}

	@NotNull
	ParkourPlugin getParkourPlugin() {
		return getPlugin(ParkourPlugin.class);
	}
}
