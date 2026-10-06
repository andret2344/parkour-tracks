package eu.andret.parkourtracks.util;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.OptionalInt;

/**
 * Converts between game ticks (20 per second) and the times shown to and typed by people.
 */
public final class Ticks {
	public static final int PER_SECOND = 20;

	private Ticks() {
	}

	/**
	 * Formats ticks as {@code mm:ss.cc} (minutes, seconds, hundredths); the minutes grow past 99 when needed.
	 */
	@NotNull
	public static String format(final long ticks) {
		final long hundredths = ticks * 100 / PER_SECOND;
		return String.format(Locale.ROOT, "%02d:%02d.%02d", hundredths / 6000, hundredths / 100 % 60, hundredths % 100);
	}

	/**
	 * Parses seconds with an optional decimal point (e.g. {@code 30.5}) into the nearest whole number of ticks.
	 *
	 * @return the ticks, or empty when the text is not a positive number of seconds
	 */
	@NotNull
	public static OptionalInt parseSeconds(@NotNull final String text) {
		final double seconds;
		try {
			seconds = Double.parseDouble(text);
		} catch (final NumberFormatException ex) {
			return OptionalInt.empty();
		}
		if (!Double.isFinite(seconds) || seconds <= 0 || seconds > Integer.MAX_VALUE / (double) PER_SECOND) {
			return OptionalInt.empty();
		}
		final int ticks = (int) Math.round(seconds * PER_SECOND);
		return ticks > 0 ? OptionalInt.of(ticks) : OptionalInt.empty();
	}
}
