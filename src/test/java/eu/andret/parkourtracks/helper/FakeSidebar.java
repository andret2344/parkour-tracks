package eu.andret.parkourtracks.helper;

import eu.andret.parkourtracks.display.Sidebar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Keeps what each player's sidebar shows as plain text: the title first, then the lines.
 */
public final class FakeSidebar implements Sidebar {
	@NotNull
	private final Map<UUID, List<String>> shown = new HashMap<>();

	@Override
	public void show(@NotNull final Player player, @NotNull final Component title, @NotNull final List<Component> lines) {
		final List<String> text = new ArrayList<>();
		text.add(plain(title));
		lines.stream().map(FakeSidebar::plain).forEach(text::add);
		shown.put(player.getUniqueId(), text);
	}

	@Override
	public void hide(@NotNull final Player player) {
		shown.remove(player.getUniqueId());
	}

	/**
	 * The title and the lines the player sees, or {@code null} when the sidebar is hidden.
	 */
	public List<String> of(@NotNull final Player player) {
		return shown.get(player.getUniqueId());
	}

	@NotNull
	private static String plain(@NotNull final Component component) {
		return PlainTextComponentSerializer.plainText().serialize(component);
	}
}
