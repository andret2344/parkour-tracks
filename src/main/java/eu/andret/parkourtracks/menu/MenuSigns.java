package eu.andret.parkourtracks.menu;

import eu.andret.parkourtracks.ParkourTracksPlugin;
import eu.andret.parkourtracks.command.Permissions;
import eu.andret.parkourtracks.message.Message;
import eu.andret.parkourtracks.track.Track;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Signs players click: {@code [ptmenu]} opens the menu, {@code [ptjoin]} with a track name on the second line enters
 * that track (confirming the fee first, like the menu). What a sign does lives in its PDC.
 */
public final class MenuSigns implements Listener {
	private static final String MENU_HEADER = "[ptmenu]";
	private static final String JOIN_HEADER = "[ptjoin]";
	private static final String MENU = "menu";
	private static final int LINES = 4;

	@NotNull
	private final ParkourTracksPlugin plugin;
	@NotNull
	private final NamespacedKey key;

	public MenuSigns(@NotNull final ParkourTracksPlugin plugin) {
		this.plugin = plugin;
		key = new NamespacedKey(plugin, "sign");
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void write(@NotNull final SignChangeEvent event) {
		final List<String> lines = event.lines()
				.stream()
				.map(line -> PlainTextComponentSerializer.plainText().serialize(line).trim())
				.toList();
		final boolean menu = lines.getFirst().equalsIgnoreCase(MENU_HEADER);
		if (!menu && !lines.getFirst().equalsIgnoreCase(JOIN_HEADER)) {
			return;
		}
		final Player player = event.getPlayer();
		if (!player.hasPermission(Permissions.EDIT)) {
			plugin.getMessages().send(player, Message.SIGN_NO_PERMISSION);
			return;
		}
		final Optional<Track> track = menu ? Optional.empty() : plugin.getTrackRegistry().find(lines.get(1));
		if (!menu && track.isEmpty()) {
			plugin.getMessages().send(player, Message.TRACK_NOT_FOUND, Placeholder.unparsed("name", lines.get(1)));
			return;
		}
		if (!(event.getBlock().getState() instanceof final Sign sign)) {
			return;
		}
		sign.getPersistentDataContainer().set(key, PersistentDataType.STRING,
				track.map(found -> found.getId().toString()).orElse(MENU));
		sign.update();
		final List<Component> rendered = track.map(this::renderJoinLines).orElseGet(this::renderMenuLines);
		for (int i = 0; i < LINES; i++) {
			event.line(i, rendered.get(i));
		}
		plugin.getMessages().send(player, Message.SIGN_CREATED_MENU);
	}

	/**
	 * Right-clicking one of these signs uses it instead of opening the sign editor.
	 */
	@EventHandler(priority = EventPriority.HIGH)
	public void click(@NotNull final PlayerInteractEvent event) {
		if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND
				|| event.getClickedBlock() == null || !(event.getClickedBlock().getState() instanceof final Sign sign)) {
			return;
		}
		final String value = sign.getPersistentDataContainer().get(key, PersistentDataType.STRING);
		if (value == null) {
			return;
		}
		event.setCancelled(true);
		final Player player = event.getPlayer();
		if (!player.hasPermission(Permissions.PLAY)) {
			return;
		}
		if (MENU.equals(value)) {
			plugin.getMenus().open(player);
			return;
		}
		final Optional<Track> track = parseTrackId(value).flatMap(id -> plugin.getTrackRegistry().find(id));
		if (track.isEmpty()) {
			plugin.getMessages().send(player, Message.SIGN_UNKNOWN_TRACK);
			return;
		}
		if (!track.get().isRunning()) {
			plugin.getMessages().send(player, Message.SIGN_TRACK_STOPPED, Placeholder.unparsed("track", track.get().getName()));
			return;
		}
		plugin.getMenus().choose(player, track.get());
	}

	@NotNull
	private static Optional<UUID> parseTrackId(@NotNull final String value) {
		try {
			return Optional.of(UUID.fromString(value));
		} catch (final IllegalArgumentException _) {
			return Optional.empty();
		}
	}

	@NotNull
	private List<Component> renderMenuLines() {
		return List.of(plugin.getMessages().get(Message.MENU_SIGN_LINE_1), plugin.getMessages().get(Message.MENU_SIGN_LINE_2),
				plugin.getMessages().get(Message.MENU_SIGN_LINE_3), plugin.getMessages().get(Message.MENU_SIGN_LINE_4));
	}

	@NotNull
	private List<Component> renderJoinLines(@NotNull final Track track) {
		final double fee = track.getOptions().getFee();
		final TagResolver[] resolvers = {
				Placeholder.unparsed("track", track.getName()),
				Placeholder.component("display-name", MiniMessage.miniMessage().deserialize(track.getDisplayName())),
				Placeholder.unparsed("difficulty", String.valueOf(track.getOptions().getDifficulty())),
				Placeholder.component("fee", fee > 0 && plugin.getBank().isAvailable()
						? Component.text(plugin.getBank().format(fee))
						: plugin.getMessages().get(Message.MENU_FREE))
		};
		return List.of(plugin.getMessages().get(Message.JOIN_SIGN_LINE_1, resolvers),
				plugin.getMessages().get(Message.JOIN_SIGN_LINE_2, resolvers),
				plugin.getMessages().get(Message.JOIN_SIGN_LINE_3, resolvers),
				plugin.getMessages().get(Message.JOIN_SIGN_LINE_4, resolvers));
	}
}
