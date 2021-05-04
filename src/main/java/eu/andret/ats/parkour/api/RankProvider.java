/*
 *  Copyright Andret (c) 2018-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.api;

import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public interface RankProvider {
	boolean isVip(@NotNull OfflinePlayer player);
}
