package eu.andret.ats.parkour.api;

import org.bukkit.OfflinePlayer;

public interface FinancialProvider {
	void addMoney(OfflinePlayer player, double amount);

	double getMoney(OfflinePlayer player);
}
