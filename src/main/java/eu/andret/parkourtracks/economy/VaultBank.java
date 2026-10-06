package eu.andret.parkourtracks.economy;

import eu.andret.parkourtracks.util.Amounts;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The economy behind Vault. The only class touching the Vault API, created only when Vault is installed. The economy
 * is looked up on every use, as economy plugins may register with Vault after this plugin started.
 */
public final class VaultBank implements Bank {
	@NotNull
	private final Server server;

	public VaultBank(@NotNull final Server server) {
		this.server = server;
	}

	@Nullable
	private Economy economy() {
		final RegisteredServiceProvider<Economy> provider = server.getServicesManager().getRegistration(Economy.class);
		return provider == null ? null : provider.getProvider();
	}

	@Override
	public boolean isAvailable() {
		return economy() != null;
	}

	@Override
	public boolean withdraw(@NotNull final Player player, final double amount) {
		final Economy economy = economy();
		return economy != null && economy.has(player, amount) && economy.withdrawPlayer(player, amount).transactionSuccess();
	}

	@Override
	public boolean deposit(@NotNull final OfflinePlayer player, final double amount) {
		final Economy economy = economy();
		return economy != null && economy.depositPlayer(player, amount).transactionSuccess();
	}

	@NotNull
	@Override
	public String format(final double amount) {
		final Economy economy = economy();
		return economy == null ? Amounts.format(amount) : economy.format(amount);
	}
}
