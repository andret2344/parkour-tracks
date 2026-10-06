package eu.andret.parkourtracks.economy;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * The server's economy, as far as the plugin needs it. {@link VaultBank} when Vault is installed, {@link NoBank}
 * otherwise; tests put their own in.
 */
public interface Bank {
	/**
	 * Whether there is an economy at all; without one there are no fees and no rewards.
	 */
	boolean isAvailable();

	/**
	 * Takes the amount from the player.
	 *
	 * @return whether it was taken; not when the player cannot afford it or the economy refused
	 */
	boolean withdraw(@NotNull Player player, double amount);

	/**
	 * Gives the amount to the player, online or not.
	 *
	 * @return whether it was given; an economy may refuse, e.g. for a player without an account
	 */
	boolean deposit(@NotNull OfflinePlayer player, double amount);

	/**
	 * The amount as the economy shows money, e.g. {@code $12.50}.
	 */
	@NotNull
	String format(double amount);
}
