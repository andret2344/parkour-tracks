package eu.andret.ats.parkour;

import eu.andret.ats.parkour.parkour.ParkourManager;
import eu.andret.ats.parkour.tutorial.TutorialManager;
import eu.andret.ats.parkour.util.M;
import org.bukkit.entity.Player;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ParkourCommandTest {
	@Test
	void test() {
		// given
		final Player sender = mock(Player.class);
		final eu.andret.ats.parkour.ParkourPlugin plugin = mock(eu.andret.ats.parkour.ParkourPlugin.class);
		final ParkourCommand parkourCommand = new ParkourCommand(sender, plugin);
		final ParkourManager parkourManager = new ParkourManager();
		final TutorialManager tutorialManager = new TutorialManager(plugin);
		when(plugin.getParkourManager()).thenReturn(parkourManager);
		when(plugin.getTutorialManager()).thenReturn(tutorialManager);
		when(plugin.msg(M.Error.DEFAULT.noLobby)).thenReturn("no-lobby");

		// when
		final String lobby = parkourCommand.lobby();

		// then
		assertThat(lobby).isEqualTo("no-lobby");
	}
}
