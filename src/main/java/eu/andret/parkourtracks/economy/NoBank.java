package eu.andret.parkourtracks.economy;

import eu.andret.parkourtracks.util.Amounts;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * No economy: nothing is ever taken or given.
 */
public final class NoBank implements Bank {
	@Override
	public boolean isAvailable() {
		return false;
	}

	@Override
	public boolean withdraw(@NotNull final Player player, final double amount) {
		return false;
	}

	@Override
	public boolean deposit(@NotNull final OfflinePlayer player, final double amount) {
		return false;
	}

	@NotNull
	@Override
	public String format(final double amount) {
		return Amounts.format(amount);
	}
}
