/*
 * Copyright Andret (c) 2018-2022. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.api;

import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

/**
 * The provider class to verify if tested player should be allowed to enter VIP-only parkours. Inject it's inherited
 * class into {@link eu.andret.ats.parkour.ParkourPlugin#setRankProvider(RankProvider)}.
 *
 * @author Andret2344
 * @since 1.0.0
 */
public interface RankProvider {
	/**
	 * The method to check player's VIP status and if they should be allowed to enter the parkour that requires such a
	 * rank.
	 *
	 * @param player The player to test whether he has a VIP rank.
	 *
	 * @return {@code true} if player should be allowed to VIP-only parkours, {@code false} otherwise.
	 */
	boolean isVip(@NotNull OfflinePlayer player);
}
