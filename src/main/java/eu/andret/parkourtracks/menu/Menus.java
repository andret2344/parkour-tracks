package eu.andret.parkourtracks.menu;

import eu.andret.parkourtracks.ParkourTracksPlugin;
import eu.andret.parkourtracks.config.Medal;
import eu.andret.parkourtracks.economy.Bank;
import eu.andret.parkourtracks.message.Message;
import eu.andret.parkourtracks.result.ResultStore;
import eu.andret.parkourtracks.track.Track;
import eu.andret.parkourtracks.track.TrackRules;
import eu.andret.parkourtracks.track.TrackType;
import eu.andret.parkourtracks.util.Ticks;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * The track selection menu: a window per category (the track types), pages of 45 tracks, and a window to confirm
 * paying a fee. Only running tracks are listed; tracks the player has no permission for show locked.
 */
public final class Menus {
	private static final int PAGE_SIZE = 45;
	private static final int PAGE_ROWS_SIZE = 54;
	private static final int PREVIOUS = 45;
	private static final int BACK = 49;
	private static final int NEXT = 53;
	private static final int CONFIRM_SIZE = 27;
	private static final int CONFIRM_YES = 11;
	private static final int CONFIRM_INFO = 13;
	private static final int CONFIRM_NO = 15;
	private static final String LINE_BREAK = "<br>";
	@NotNull
	private static final List<Material> DIFFICULTY_WOOL = List.of(Material.LIME_WOOL, Material.YELLOW_WOOL,
			Material.ORANGE_WOOL, Material.RED_WOOL, Material.BLACK_WOOL);
	@NotNull
	private static final Map<TrackType, Material> CATEGORY_ICONS = Map.of(TrackType.SERVER, Material.NETHER_STAR,
			TrackType.PLAYERS, Material.PLAYER_HEAD, TrackType.TRAINING, Material.LEATHER_BOOTS);
	@NotNull
	private static final Map<TrackType, Message> CATEGORY_NAMES = Map.of(TrackType.SERVER, Message.MENU_CATEGORY_SERVER,
			TrackType.PLAYERS, Message.MENU_CATEGORY_PLAYERS, TrackType.TRAINING, Message.MENU_CATEGORY_TRAINING);

	@NotNull
	private final ParkourTracksPlugin plugin;

	public Menus(@NotNull final ParkourTracksPlugin plugin) {
		this.plugin = plugin;
	}

	/**
	 * Opens the menu: the categories with running tracks, or straight the only one there is.
	 */
	public void open(@NotNull final Player player) {
		final List<TrackType> categories = Arrays.stream(TrackType.values())
				.filter(type -> !tracks(type).isEmpty())
				.toList();
		if (categories.isEmpty()) {
			plugin.getMessages().send(player, Message.MENU_EMPTY);
			return;
		}
		if (categories.size() == 1) {
			openCategory(player, categories.getFirst(), 0);
			return;
		}
		final Menu menu = new Menu();
		final Inventory inventory = plugin.getServer().createInventory(menu, CONFIRM_SIZE, text(Message.MENU_TITLE));
		menu.setInventory(inventory);
		final int[] slots = categories.size() == 2 ? new int[]{11, 15} : new int[]{11, 13, 15};
		for (int i = 0; i < categories.size(); i++) {
			final TrackType type = categories.get(i);
			inventory.setItem(slots[i], item(CATEGORY_ICONS.get(type), text(CATEGORY_NAMES.get(type)),
					List.of(text(Message.MENU_CATEGORY_LORE, Placeholder.unparsed("count", String.valueOf(tracks(type).size()))))));
			menu.onClick(slots[i], clicker -> openCategory(clicker, type, 0));
		}
		player.openInventory(inventory);
	}

	/**
	 * The running tracks of a type, the easier first, then by name.
	 */
	@NotNull
	private List<Track> tracks(@NotNull final TrackType type) {
		return plugin.getTrackRegistry().getTracks()
				.stream()
				.filter(Track::isRunning)
				.filter(track -> track.getType() == type)
				.sorted(Comparator.comparingInt((Track track) -> track.getOptions().getDifficulty()).thenComparing(Track::getName))
				.toList();
	}

	/**
	 * Opens a page of a category, once the player's results on its tracks come from the database.
	 */
	public void openCategory(@NotNull final Player player, @NotNull final TrackType type, final int page) {
		final List<Track> all = tracks(type);
		final int pages = Math.max(1, (all.size() + PAGE_SIZE - 1) / PAGE_SIZE);
		final int shown = Math.clamp(page, 0, pages - 1);
		final List<Track> tracks = all.subList(shown * PAGE_SIZE, Math.min(all.size(), (shown + 1) * PAGE_SIZE));
		final List<CompletableFuture<Entry>> entries = tracks.stream()
				.map(track -> plugin.getResults().playerResult(track.getId(), player.getUniqueId())
						.thenCombine(plugin.getResults().ranked(track.getId(), 1),
								(result, record) -> new Entry(track, result, record)))
				.toList();
		CompletableFuture.allOf(entries.toArray(CompletableFuture[]::new))
				.thenRun(() -> plugin.runOnMainThread(() -> {
					if (player.isOnline()) {
						showCategory(player, type, shown, pages, entries.stream().map(CompletableFuture::join).toList());
					}
				}));
	}

	/**
	 * A track with the player's results and its record.
	 */
	private record Entry(@NotNull Track track, @NotNull Optional<ResultStore.PlayerResult> result,
						 @NotNull Optional<ResultStore.Ranked> record) {
	}

	private void showCategory(@NotNull final Player player, @NotNull final TrackType type, final int page,
							  final int pages, @NotNull final List<Entry> entries) {
		final Menu menu = new Menu();
		final Inventory inventory = plugin.getServer().createInventory(menu, PAGE_ROWS_SIZE,
				text(Message.MENU_CATEGORY_TITLE, Placeholder.component("category", text(CATEGORY_NAMES.get(type))),
						Placeholder.unparsed("page", String.valueOf(page + 1)),
						Placeholder.unparsed("pages", String.valueOf(pages))));
		menu.setInventory(inventory);
		for (int i = 0; i < entries.size(); i++) {
			final Entry entry = entries.get(i);
			inventory.setItem(i, trackItem(player, entry));
			menu.onClick(i, clicker -> choose(clicker, entry.track()));
		}
		if (page > 0) {
			inventory.setItem(PREVIOUS, item(Material.ARROW, text(Message.MENU_PREVIOUS), List.of()));
			menu.onClick(PREVIOUS, clicker -> openCategory(clicker, type, page - 1));
		}
		if (page < pages - 1) {
			inventory.setItem(NEXT, item(Material.ARROW, text(Message.MENU_NEXT), List.of()));
			menu.onClick(NEXT, clicker -> openCategory(clicker, type, page + 1));
		}
		inventory.setItem(BACK, item(Material.BARRIER, text(Message.MENU_BACK), List.of()));
		menu.onClick(BACK, this::open);
		player.openInventory(inventory);
	}

	@NotNull
	private ItemStack trackItem(@NotNull final Player player, @NotNull final Entry entry) {
		final Track track = entry.track();
		final Material icon = track.getOptions().getIcon() != null
				? track.getOptions().getIcon()
				: DIFFICULTY_WOOL.get(track.getOptions().getDifficulty() - 1);
		final Component none = text(Message.VALUE_NONE);
		final List<Medal> medals = plugin.getSettings().medals();
		final TagResolver[] resolvers = {
				Placeholder.unparsed("track", track.getName()),
				Placeholder.component("display-name", MiniMessage.miniMessage().deserialize(track.getDisplayName())),
				Placeholder.unparsed("difficulty", String.valueOf(track.getOptions().getDifficulty())),
				Placeholder.component("fee", fee(track)),
				Placeholder.component("best", entry.result().<Component>map(result -> Component.text(Ticks.format(result.bestTicks()))).orElse(none)),
				Placeholder.component("record", entry.record().<Component>map(record -> Component.text(Ticks.format(record.ticks()))).orElse(none)),
				Placeholder.component("holder", entry.record().<Component>map(record -> Component.text(name(record.player())))
						.orElse(text(Message.SIGN_NOBODY))),
				Placeholder.component("medal", entry.result()
						.flatMap(result -> TrackRules.bestMedal(medals, track.getMedals(), result.bestTicks()))
						.<Component>map(medal -> MiniMessage.miniMessage().deserialize(medal.displayName()))
						.orElse(none)),
				Placeholder.unparsed("completions", String.valueOf(entry.result().map(ResultStore.PlayerResult::completions).orElse(0))),
				Placeholder.unparsed("authors", authors(track)),
				Placeholder.component("access", text(canEnter(player, track) ? Message.MENU_ACCESS_OPEN : Message.MENU_ACCESS_LOCKED))
		};
		final List<Component> lore = new ArrayList<>(lines(Message.MENU_TRACK_LORE, resolvers));
		if (track.getType() == TrackType.PLAYERS) {
			lore.addAll(lines(Message.MENU_TRACK_AUTHORS, resolvers));
		}
		final ItemStack item = item(icon, text(Message.MENU_TRACK_NAME, resolvers), lore);
		if (entry.result().isPresent()) {
			// Completed tracks glow
			final ItemMeta meta = item.getItemMeta();
			meta.setEnchantmentGlintOverride(true);
			item.setItemMeta(meta);
		}
		return item;
	}

	/**
	 * Enters the track, after the window confirming the fee when there is one to pay.
	 */
	public void choose(@NotNull final Player player, @NotNull final Track track) {
		if (!canEnter(player, track)) {
			player.closeInventory();
			plugin.getMessages().send(player, Message.NO_TRACK_PERMISSION, Placeholder.unparsed("track", track.getName()));
			return;
		}
		final Bank bank = plugin.getBank();
		if (track.getOptions().getFee() > 0 && bank.isAvailable()) {
			confirm(player, track);
			return;
		}
		player.closeInventory();
		plugin.getGames().enter(player, track);
	}

	/**
	 * Opens the window asking whether to pay the track's fee and play it.
	 */
	public void confirm(@NotNull final Player player, @NotNull final Track track) {
		final Menu menu = new Menu();
		final TagResolver[] resolvers = {
				Placeholder.unparsed("track", track.getName()),
				Placeholder.component("display-name", MiniMessage.miniMessage().deserialize(track.getDisplayName())),
				Placeholder.component("fee", fee(track))
		};
		final Inventory inventory = plugin.getServer().createInventory(menu, CONFIRM_SIZE,
				text(Message.CONFIRM_TITLE, resolvers));
		menu.setInventory(inventory);
		inventory.setItem(CONFIRM_YES, item(Material.LIME_WOOL, text(Message.CONFIRM_YES, resolvers), List.of()));
		inventory.setItem(CONFIRM_INFO, item(Material.PAPER, text(Message.MENU_TRACK_NAME, resolvers), List.of()));
		inventory.setItem(CONFIRM_NO, item(Material.RED_WOOL, text(Message.CONFIRM_NO, resolvers), List.of()));
		menu.onClick(CONFIRM_YES, clicker -> {
			clicker.closeInventory();
			if (track.isRunning()) {
				plugin.getGames().enter(clicker, track);
			}
		});
		menu.onClick(CONFIRM_NO, Player::closeInventory);
		player.openInventory(inventory);
	}

	private static boolean canEnter(@NotNull final Player player, @NotNull final Track track) {
		final String permission = track.getOptions().getPermission();
		return permission == null || player.hasPermission(permission);
	}

	@NotNull
	private Component fee(@NotNull final Track track) {
		final double fee = track.getOptions().getFee();
		return fee > 0 && plugin.getBank().isAvailable()
				? Component.text(plugin.getBank().format(fee))
				: text(Message.MENU_FREE);
	}

	@NotNull
	private String authors(@NotNull final Track track) {
		return track.getAuthors().stream().map(this::name).collect(Collectors.joining(", "));
	}

	@NotNull
	private String name(@NotNull final UUID id) {
		final OfflinePlayer player = plugin.getServer().getOfflinePlayer(id);
		return player.getName() == null ? id.toString() : player.getName();
	}

	@NotNull
	private Component text(@NotNull final Message message, @NotNull final TagResolver... resolvers) {
		return plugin.getMessages().get(message, resolvers).decoration(TextDecoration.ITALIC, false);
	}

	/**
	 * A message of several lines, separated by {@code <br>}.
	 */
	@NotNull
	private List<Component> lines(@NotNull final Message message, @NotNull final TagResolver... resolvers) {
		return Arrays.stream(plugin.getMessages().template(message).split(LINE_BREAK))
				.map(line -> MiniMessage.miniMessage().deserialize(line, resolvers).decoration(TextDecoration.ITALIC, false))
				.toList();
	}

	@NotNull
	private static ItemStack item(@NotNull final Material material, @NotNull final Component name,
								  @NotNull final List<Component> lore) {
		final ItemStack item = new ItemStack(material);
		final ItemMeta meta = item.getItemMeta();
		meta.customName(name);
		meta.lore(lore);
		item.setItemMeta(meta);
		return item;
	}
}
