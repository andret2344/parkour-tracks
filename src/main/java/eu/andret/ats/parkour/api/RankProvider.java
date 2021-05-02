/*
 * Copyright Andret (c) 2019-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.api;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface RankProvider {
	boolean isVip(@NotNull Player player);
}
