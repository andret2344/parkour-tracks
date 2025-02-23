/*
 * Copyright Andret Tools System (c) 2025. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.sample.provider;

import eu.andret.ats.parkour.api.FinancialProvider;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SampleFinancialProvider implements FinancialProvider {
	@NotNull
	private final Map<UUID, Double> finances = new HashMap<>();

	@Override
	public boolean addMoney(@NotNull final OfflinePlayer player, final double amount) {
		final double owned = finances.getOrDefault(player.getUniqueId(), 0D);
		if (owned + amount < 0) {
			return false;
		}
		finances.put(player.getUniqueId(), owned + amount);
		return true;
	}

	@Override
	public double getMoney(@NotNull final OfflinePlayer player) {
		return finances.getOrDefault(player.getUniqueId(), 0D);
	}

	public @NotNull Map<UUID, Double> getFinances() {
		return finances;
	}
}
