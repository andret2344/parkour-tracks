/*
 * Copyright Andret Tools System (c) 2025. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.sample;

import eu.andret.arguments.AnnotatedCommand;
import eu.andret.arguments.CommandManager;
import eu.andret.ats.parkour.ParkourPlugin;
import eu.andret.ats.parkour.sample.provider.SampleFinancialProvider;
import eu.andret.ats.parkour.sample.provider.SampleRankProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class ParkourPluginSample extends JavaPlugin {
	@NotNull
	SampleFinancialProvider financialProvider = new SampleFinancialProvider();
	@NotNull
	SampleRankProvider rankProvider = new SampleRankProvider();

	@Override
	public void onEnable() {
		getParkourPlugin().setFinancialProvider(financialProvider);
		getParkourPlugin().setRankProvider(rankProvider);
		final AnnotatedCommand<ParkourPluginSample> command = CommandManager.registerCommand(ParkourPluginSampleCommand.class, this);
		command.addEnumCompleter(BalanceInteraction.class);
		command.addTypeCompleter(boolean.class, Arrays.asList(Boolean.FALSE.toString(), Boolean.TRUE.toString()));
		command.addEnumMapper(BalanceInteraction.class);
		command.setOnUnknownSubCommandExecutionListener(sender -> sender.sendMessage("/sample rank <value> or /sample balance <interaction> <amount>"));
	}

	@NotNull
	ParkourPlugin getParkourPlugin() {
		return getPlugin(ParkourPlugin.class);
	}

	@NotNull
	public SampleFinancialProvider getFinancialProvider() {
		return financialProvider;
	}

	@NotNull
	public SampleRankProvider getRankProvider() {
		return rankProvider;
	}
}
