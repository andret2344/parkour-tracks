package eu.andret.parkourtracks.util;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * Formats amounts of money (fees, rewards) until the economy formats them itself.
 */
public final class Amounts {
	private Amounts() {
	}

	@NotNull
	public static String format(final double amount) {
		return String.format(Locale.ROOT, "%.2f", amount);
	}
}
