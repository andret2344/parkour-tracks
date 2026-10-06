package eu.andret.parkourtracks.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TicksTest {
	@Test
	void formatsMinutesSecondsAndHundredths() {
		// when / then
		assertThat(Ticks.format(0)).isEqualTo("00:00.00");
		assertThat(Ticks.format(1)).isEqualTo("00:00.05");
		assertThat(Ticks.format(610)).isEqualTo("00:30.50");
		assertThat(Ticks.format(20 * 61)).isEqualTo("01:01.00");
		assertThat(Ticks.format(20L * 60 * 120)).isEqualTo("120:00.00");
	}

	@Test
	void parsesSecondsIntoTheNearestTicks() {
		// when / then
		assertThat(Ticks.parseSeconds("30")).hasValue(600);
		assertThat(Ticks.parseSeconds("30.5")).hasValue(610);
		assertThat(Ticks.parseSeconds("0.06")).hasValue(1);
		assertThat(Ticks.parseSeconds("0.01")).isEmpty();
		assertThat(Ticks.parseSeconds("0")).isEmpty();
		assertThat(Ticks.parseSeconds("-3")).isEmpty();
		assertThat(Ticks.parseSeconds("NaN")).isEmpty();
		assertThat(Ticks.parseSeconds("1e12")).isEmpty();
		assertThat(Ticks.parseSeconds("soon")).isEmpty();
	}

	@Test
	void formatsAmountsWithTwoDecimals() {
		// when / then
		assertThat(Amounts.format(12.5)).isEqualTo("12.50");
		assertThat(Amounts.format(0)).isEqualTo("0.00");
	}
}
