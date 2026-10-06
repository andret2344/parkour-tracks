package eu.andret.parkourtracks;

import eu.andret.parkourtracks.helper.PluginTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ParkourTracksPluginTest extends PluginTest {
	@Test
	void enables() {
		// when
		final boolean result = plugin.isEnabled();

		// then
		assertThat(result).isTrue();
	}
}
