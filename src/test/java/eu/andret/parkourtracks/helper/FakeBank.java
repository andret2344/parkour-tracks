package eu.andret.parkourtracks.helper;

import eu.andret.parkourtracks.economy.Bank;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * An economy in memory, showing money as {@code $12.50}. Deposits to players marked with {@link #refuse} fail.
 */
public final class FakeBank implements Bank {
	@NotNull
	private final Map<UUID, Double> balances = new HashMap<>();
	@NotNull
	private final Set<UUID> refused = new HashSet<>();

	public void set(@NotNull final OfflinePlayer player, final double balance) {
		balances.put(player.getUniqueId(), balance);
	}

	public double balance(@NotNull final OfflinePlayer player) {
		return balances.getOrDefault(player.getUniqueId(), 0.0);
	}

	public void refuse(@NotNull final OfflinePlayer player, final boolean refuse) {
		if (refuse) {
			refused.add(player.getUniqueId());
		} else {
			refused.remove(player.getUniqueId());
		}
	}

	@Override
	public boolean isAvailable() {
		return true;
	}

	@Override
	public boolean withdraw(@NotNull final Player player, final double amount) {
		if (balance(player) < amount) {
			return false;
		}
		balances.put(player.getUniqueId(), balance(player) - amount);
		return true;
	}

	@Override
	public boolean deposit(@NotNull final OfflinePlayer player, final double amount) {
		if (refused.contains(player.getUniqueId())) {
			return false;
		}
		balances.put(player.getUniqueId(), balance(player) + amount);
		return true;
	}

	@NotNull
	@Override
	public String format(final double amount) {
		return String.format(Locale.ROOT, "$%.2f", amount);
	}
}
