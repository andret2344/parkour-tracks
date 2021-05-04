/*
 *  Copyright Andret (c) 2018-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.api;

import org.bukkit.OfflinePlayer;

public interface FinancialProvider {
	boolean addMoney(OfflinePlayer player, double amount);

	double getMoney(OfflinePlayer player);
}
