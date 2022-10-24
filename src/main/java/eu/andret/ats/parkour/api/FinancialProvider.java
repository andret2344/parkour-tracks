/*
 * Copyright Andret (c) 2018-2022. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.api;

import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

/**
 * The provider class to add money or check player's balance. Inject it's implementation class into
 * {@link eu.andret.ats.parkour.ParkourPlugin#setFinancialProvider(FinancialProvider)}.
 *
 * @author Andret2344
 * @since 1.0.0
 */
public interface FinancialProvider {
	/**
	 * The method to add some amount to player's balance.
	 *
	 * @param player The player whom balance should be modified.
	 * @param amount The amount to add, pass negative value to subtract.
	 *
	 * @return {@code true} if addition or subtraction was successful, {@code false} otherwise.
	 */
	boolean addMoney(@NotNull OfflinePlayer player, double amount);

	/**
	 * The method to get current player's balance.
	 *
	 * @param player The player whom balance is to be checked.
	 *
	 * @return The current balance of the passed player.
	 */
	double getMoney(@NotNull OfflinePlayer player);
}
