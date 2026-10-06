package eu.andret.parkourtracks.game;

import eu.andret.parkourtracks.ParkourTracksPlugin;
import eu.andret.parkourtracks.config.Medal;
import eu.andret.parkourtracks.config.TimerDisplay;
import eu.andret.parkourtracks.economy.Bank;
import eu.andret.parkourtracks.economy.MedalPayouts;
import eu.andret.parkourtracks.message.Message;
import eu.andret.parkourtracks.result.ResultStore;
import eu.andret.parkourtracks.track.Checkpoint;
import eu.andret.parkourtracks.track.Cuboid;
import eu.andret.parkourtracks.track.SkipMode;
import eu.andret.parkourtracks.track.Track;
import eu.andret.parkourtracks.track.TrackEffect;
import eu.andret.parkourtracks.track.TrackRules;
import eu.andret.parkourtracks.track.TrackType;
import eu.andret.parkourtracks.track.WorldSpot;
import eu.andret.parkourtracks.util.Ticks;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

/**
 * The one place that decides who is in which game and what happens to them: entering, the run, checkpoints, walls,
 * the finish and leaving. Listeners and commands only report what happened.
 */
public final class GameManager {
	private static final int FULL_FOOD = 20;

	@NotNull
	private final ParkourTracksPlugin plugin;
	@NotNull
	private final Map<UUID, GameSession> sessions = new HashMap<>();
	@NotNull
	private final Set<UUID> ignoring = new HashSet<>();
	@NotNull
	private final NamespacedKey sessionKey;
	@NotNull
	private final NamespacedKey snapshotKey;
	@NotNull
	private final GameItems items;
	/**
	 * Set while the plugin itself teleports a player, so the teleport listener leaves that teleport alone.
	 */
	private boolean teleporting;
	/**
	 * Set while the plugin puts a player into a boat or takes them out, so the vehicle listener lets it happen.
	 */
	private boolean boating;

	public GameManager(@NotNull final ParkourTracksPlugin plugin) {
		this.plugin = plugin;
		sessionKey = new NamespacedKey(plugin, "session");
		snapshotKey = new NamespacedKey(plugin, "snapshot");
		items = new GameItems(plugin);
	}

	@NotNull
	public GameItems getItems() {
		return items;
	}

	/**
	 * Does what the game item stands for.
	 */
	public void use(@NotNull final Player player, @NotNull final GameItem item) {
		final GameSession session = sessions.get(player.getUniqueId());
		if (session == null) {
			return;
		}
		switch (item) {
			case BACK -> goBack(player, session);
			case RESTART -> restart(player, session);
			case HIDE -> toggleHiding(player, session);
			case EXIT -> leave(player, LeaveReason.EXIT);
		}
	}

	/**
	 * Hides the other players of the track from the player, or shows them again.
	 */
	public void toggleHiding(@NotNull final Player player, @NotNull final GameSession session) {
		session.setHiding(!session.isHiding());
		otherPlayers(session).forEach(other -> {
			if (session.isHiding()) {
				player.hidePlayer(plugin, other);
			} else {
				player.showPlayer(plugin, other);
			}
		});
		plugin.getMessages().send(player, session.isHiding() ? Message.PLAYERS_HIDDEN : Message.PLAYERS_SHOWN);
	}

	/**
	 * The online players in the same game as the session's player, without them.
	 */
	@NotNull
	private List<Player> otherPlayers(@NotNull final GameSession session) {
		return sessions.values()
				.stream()
				.filter(other -> other != session)
				.filter(other -> other.getTrack().equals(session.getTrack()))
				.map(other -> plugin.getServer().getPlayer(other.getPlayer()))
				.filter(Objects::nonNull)
				.toList();
	}

	@NotNull
	public Optional<GameSession> getSession(@NotNull final Player player) {
		return Optional.ofNullable(sessions.get(player.getUniqueId()));
	}

	@NotNull
	public Collection<GameSession> getSessions() {
		return Collections.unmodifiableCollection(sessions.values());
	}

	/**
	 * Whether the plugin is teleporting a player right now.
	 */
	public boolean isTeleporting() {
		return teleporting;
	}

	public boolean isBoating() {
		return boating;
	}

	/**
	 * Whether the entity is the boat of a player in a game.
	 */
	public boolean isGameBoat(@Nullable final Entity entity) {
		if (entity == null) {
			return false;
		}
		return sessions.values().stream().anyMatch(session -> entity.getUniqueId().equals(session.getBoat()));
	}

	public boolean isIgnoring(@NotNull final Player player) {
		return ignoring.contains(player.getUniqueId());
	}

	/**
	 * Turns ignoring tracks on or off; a player starting to ignore them leaves their game.
	 *
	 * @return whether the player ignores tracks now
	 */
	public boolean toggleIgnoring(@NotNull final Player player) {
		if (ignoring.remove(player.getUniqueId())) {
			return false;
		}
		ignoring.add(player.getUniqueId());
		if (sessions.containsKey(player.getUniqueId())) {
			leave(player, LeaveReason.NOT_A_PLAYER);
		}
		return true;
	}

	/**
	 * Whether the player can be a parkour player at all: in survival or adventure, not ignoring tracks.
	 */
	public boolean isEligible(@NotNull final Player player) {
		return (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE)
				&& !isIgnoring(player);
	}

	/**
	 * The running track whose region holds the location.
	 */
	@NotNull
	public Optional<Track> findRunningTrack(@NotNull final Location location) {
		final World world = location.getWorld();
		if (world == null) {
			return Optional.empty();
		}
		return plugin.getTrackRegistry().getTracks()
				.stream()
				.filter(Track::isRunning)
				.filter(track -> track.getWorld().equals(world.getName()))
				.filter(track -> track.getRegion().contains(location.getX(), location.getY(), location.getZ()))
				.findFirst();
	}

	// ======= Moving =======

	/**
	 * Handles a player moving from one point to another: entering a track, leaving it, and the run on it.
	 *
	 * @param sweep whether to check the straight path between the points, so a fast move does not skip a thin
	 *              region; off for a teleport, which jumps
	 */
	public void move(@NotNull final Player player, @NotNull final Location from, @NotNull final Location to,
					 final boolean sweep) {
		final GameSession session = sessions.get(player.getUniqueId());
		if (session == null) {
			enterIfInside(player, to);
			return;
		}
		final Track track = session.getTrack();
		if (!isInRegion(track, to)) {
			leave(player, LeaveReason.LEFT_REGION);
			return;
		}
		if (session.getPhase() == Phase.FINISHED) {
			return;
		}
		final Checkpoint spawn = Objects.requireNonNull(track.getSpawn());
		if (session.getPhase() == Phase.WAITING && !contains(spawn.area(), to)) {
			session.start();
		}
		for (final Hit hit : hits(track, from, to, sweep)) {
			if (!handle(player, session, hit)) {
				return;
			}
		}
		session.setPaused(track.getOptions().isPauseOnCheckpoints()
				&& track.getCheckpoints().stream().anyMatch(checkpoint -> contains(checkpoint.area(), to)));
	}

	/**
	 * Puts a player who got into a running track's region into its game: through the spawn as they are, from
	 * anywhere else onto the spawn.
	 */
	private void enterIfInside(@NotNull final Player player, @NotNull final Location to) {
		if (!isEligible(player)) {
			return;
		}
		findRunningTrack(to).ifPresent(track -> {
			final Checkpoint spawn = Objects.requireNonNull(track.getSpawn());
			final boolean throughSpawn = contains(spawn.area(), to);
			final GameSession session = join(player, track, throughSpawn ? Entry.SPAWN : Entry.SIDE);
			if (session == null) {
				turnAway(player, track);
				return;
			}
			if (!throughSpawn) {
				sendTo(player, session, spawnLocation(track));
				plugin.getMessages().send(player, Message.SENT_TO_SPAWN, track(track));
			} else if (track.getOptions().isBoat()) {
				sendTo(player, session, spawnLocation(track));
			}
		});
	}

	/**
	 * Where a teleport not made by the plugin should take the player, or {@code null} to let it be: out of the region
	 * it ends the game, inside it the player goes back to their last checkpoint instead, and into a running track it
	 * enters that track like walking in.
	 *
	 * @param jump whether the teleport is an allowed ender pearl, which moves the player along the track
	 */
	@Nullable
	public Location teleported(@NotNull final Player player, @NotNull final Location from, @NotNull final Location to,
							   final boolean jump) {
		final GameSession session = sessions.get(player.getUniqueId());
		if (session == null) {
			if (!isEligible(player)) {
				return null;
			}
			final Optional<Track> track = findRunningTrack(to);
			if (track.isEmpty()) {
				return null;
			}
			final boolean intoSpawn = contains(Objects.requireNonNull(track.get().getSpawn()).area(), to);
			if (join(player, track.get(), intoSpawn ? Entry.SPAWN : Entry.SIDE) == null) {
				return lobby(track.get()).orElse(from);
			}
			if (intoSpawn) {
				return null;
			}
			plugin.getMessages().send(player, Message.SENT_TO_SPAWN, track(track.get()));
			return spawnLocation(track.get());
		}
		if (!isInRegion(session.getTrack(), to)) {
			leave(player, LeaveReason.TELEPORT);
			return null;
		}
		if (session.getPhase() == Phase.FINISHED) {
			return null;
		}
		if (jump) {
			// Handled once the player has landed: sending them back from inside the teleport would teleport twice
			plugin.getServer().getScheduler().runTask(plugin, () -> {
				if (sessions.get(player.getUniqueId()) == session) {
					move(player, from, to, false);
				}
			});
			return null;
		}
		return backTarget(session);
	}

	/**
	 * Something the move went through, in the order it got there.
	 */
	private record Hit(double distance, @NotNull Kind kind, int checkpoint) {
		enum Kind {
			WALL,
			SPAWN,
			CHECKPOINT,
			FINISH
		}
	}

	@NotNull
	private static List<Hit> hits(@NotNull final Track track, @NotNull final Location from, @NotNull final Location to,
								  final boolean sweep) {
		final List<Hit> hits = new ArrayList<>();
		track.getWalls().forEach(wall -> entered(wall, from, to, sweep)
				.ifPresent(distance -> hits.add(new Hit(distance, Hit.Kind.WALL, -1))));
		entered(Objects.requireNonNull(track.getSpawn()).area(), from, to, sweep)
				.ifPresent(distance -> hits.add(new Hit(distance, Hit.Kind.SPAWN, -1)));
		IntStream.range(0, track.getCheckpoints().size())
				.forEach(index -> entered(track.getCheckpoints().get(index).area(), from, to, sweep)
						.ifPresent(distance -> hits.add(new Hit(distance, Hit.Kind.CHECKPOINT, index))));
		entered(Objects.requireNonNull(track.getFinish()).area(), from, to, sweep)
				.ifPresent(distance -> hits.add(new Hit(distance, Hit.Kind.FINISH, -1)));
		hits.sort(Comparator.comparingDouble(Hit::distance));
		return hits;
	}

	/**
	 * How far along the move it got into the area, when it was outside the area at the start.
	 */
	@NotNull
	private static OptionalDouble entered(@NotNull final Cuboid area, @NotNull final Location from,
										  @NotNull final Location to, final boolean sweep) {
		if (contains(area, from)) {
			return OptionalDouble.empty();
		}
		if (sweep) {
			return Segments.entry(area, from, to);
		}
		return contains(area, to) ? OptionalDouble.of(1) : OptionalDouble.empty();
	}

	/**
	 * @return whether the rest of the move still counts; not after the player was sent somewhere else
	 */
	private boolean handle(@NotNull final Player player, @NotNull final GameSession session, @NotNull final Hit hit) {
		final Track track = session.getTrack();
		return switch (hit.kind()) {
			case WALL -> {
				goBack(player, session);
				yield false;
			}
			case SPAWN -> {
				session.resetToSpawn();
				yield true;
			}
			case CHECKPOINT -> session.getPhase() != Phase.RUNNING || reach(player, session, hit.checkpoint());
			case FINISH -> {
				if (session.getPhase() == Phase.RUNNING && reach(player, session, track.getCheckpoints().size())) {
					finish(player, session);
				}
				yield false;
			}
		};
	}

	/**
	 * Passes the checkpoint at the index, the finish being the index after the last checkpoint.
	 *
	 * @return whether the checkpoint counts; not when a skip sent the player back
	 */
	private boolean reach(@NotNull final Player player, @NotNull final GameSession session, final int index) {
		final int last = session.getLastCheckpoint();
		if (index <= last) {
			return true;
		}
		final Track track = session.getTrack();
		final boolean finish = index == track.getCheckpoints().size();
		if (index > last + 1) {
			final SkipMode mode = track.getOptions().getSkipMode();
			if (mode != SkipMode.ALLOW) {
				plugin.getMessages().send(player, Message.CHECKPOINT_MISSED,
						Placeholder.unparsed("checkpoint", String.valueOf(last + 2)));
			}
			if (mode == SkipMode.FAIL) {
				goBack(player, session);
				return false;
			}
		}
		if (!finish) {
			session.reach(index);
			plugin.getMessages().send(player, Message.CHECKPOINT_REACHED,
					Placeholder.unparsed("checkpoint", String.valueOf(index + 1)),
					Placeholder.unparsed("count", String.valueOf(track.getCheckpoints().size())));
		}
		return true;
	}

	private void finish(@NotNull final Player player, @NotNull final GameSession session) {
		final Track track = session.getTrack();
		session.finish();
		if (track.getType() == TrackType.TRAINING) {
			plugin.getMessages().send(player, Message.FINISHED_TRAINING, track(track));
		} else {
			final int ticks = session.getTicks();
			plugin.getMessages().send(player, Message.FINISHED, track(track),
					Placeholder.unparsed("time", Ticks.format(ticks)));
			final double reward = track.getOptions().getReward();
			if (reward > 0 && plugin.getBank().deposit(player, reward)) {
				plugin.getMessages().send(player, Message.REWARD_PAID, track(track),
						Placeholder.unparsed("amount", plugin.getBank().format(reward)));
			}
			plugin.getResults().recordRun(track.getId(), player.getUniqueId(), ticks, Instant.now())
					.thenAcceptAsync(outcome -> announce(player.getUniqueId(), track, ticks, outcome), plugin::runOnMainThread)
					.exceptionally(ex -> {
						plugin.getLogger().severe("Could not save a run on " + track + ": " + ex.getMessage());
						return null;
					});
		}
		switch (track.getOptions().getAfterFinish()) {
			case SPAWN -> {
				session.resetToSpawn();
				sendTo(player, session, spawnLocation(track));
			}
			case LOBBY -> {
				final int delay = plugin.getSettings().finishDelaySeconds();
				if (delay == 0) {
					leave(player, LeaveReason.FINISHED);
					return;
				}
				plugin.getMessages().send(player, Message.FINISH_LOBBY_SOON,
						Placeholder.unparsed("seconds", String.valueOf(delay)));
				session.setFinishTask(plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin,
						() -> leave(player, LeaveReason.FINISHED), (long) delay * Ticks.PER_SECOND));
			}
		}
	}

	/**
	 * Tells a player who finished what the run achieved: a track record, a personal best, a better medal.
	 */
	private void announce(@NotNull final UUID id, @NotNull final Track track, final int ticks,
						  @NotNull final ResultStore.RunOutcome outcome) {
		plugin.getRecordSigns().refresh(track.getId());
		// The record, or someone's best, may have changed for everyone on the track
		sessions.values()
				.stream()
				.filter(session -> session.getTrack().equals(track))
				.toList()
				.forEach(session -> Optional.ofNullable(plugin.getServer().getPlayer(session.getPlayer()))
						.ifPresent(other -> refreshSidebar(other, session)));
		final Player player = plugin.getServer().getPlayer(id);
		if (player == null) {
			return;
		}
		if (outcome.isTrackRecord(ticks)) {
			plugin.getMessages().send(player, Message.NEW_TRACK_RECORD, track(track));
		} else if (outcome.isPersonalBest(ticks)) {
			plugin.getMessages().send(player, Message.NEW_PERSONAL_BEST, track(track));
		}
		final List<Medal> medals = plugin.getSettings().medals();
		final Optional<Medal> earned = TrackRules.bestMedal(medals, track.getMedals(), ticks);
		final Optional<Medal> before = outcome.previousBest().isPresent()
				? TrackRules.bestMedal(medals, track.getMedals(), outcome.previousBest().getAsInt())
				: Optional.empty();
		if (earned.isPresent() && (before.isEmpty() || medals.indexOf(earned.get()) < medals.indexOf(before.get()))) {
			plugin.getMessages().send(player, Message.MEDAL_EARNED,
					Placeholder.parsed("medal", earned.get().displayName()));
		}
		payMedals(player, track, ticks, outcome.paidMedals());
	}

	/**
	 * Pays the medal rewards the run is owed, each once ever; a failed payment is not noted, so it stays owed.
	 */
	private void payMedals(@NotNull final Player player, @NotNull final Track track, final int ticks,
						   @NotNull final Set<String> paid) {
		final Bank bank = plugin.getBank();
		if (!bank.isAvailable()) {
			return;
		}
		for (final MedalPayouts.Due due : MedalPayouts.due(plugin.getSettings().medals(), track.getMedals(), ticks, paid)) {
			if (!bank.deposit(player, due.amount())) {
				plugin.getLogger().warning("Could not pay " + player.getName() + " the reward of " + due.medal().key()
						+ " on " + track.getName());
				continue;
			}
			plugin.getResults().recordPayout(track.getId(), player.getUniqueId(), due.medal().key(), due.amount(), Instant.now());
			plugin.getMessages().send(player, Message.MEDAL_PAID, Placeholder.parsed("medal", due.medal().displayName()),
					Placeholder.unparsed("amount", bank.format(due.amount())));
		}
	}

	/**
	 * Sends the player back: to their last checkpoint, or to the spawn before the first one and on hardcore tracks,
	 * where the run starts over.
	 */
	public void goBack(@NotNull final Player player, @NotNull final GameSession session) {
		sendTo(player, session, backTarget(session));
	}

	/**
	 * Where going back takes the player, and puts the session there.
	 */
	@NotNull
	private Location backTarget(@NotNull final GameSession session) {
		final Track track = session.getTrack();
		if (session.getPhase() == Phase.FINISHED) {
			return spawnLocation(track);
		}
		if (track.getOptions().isHardcore() || session.getLastCheckpoint() == GameSession.SPAWN) {
			session.resetToSpawn();
			return spawnLocation(track);
		}
		return Objects.requireNonNull(track.getCheckpoints().get(session.getLastCheckpoint()).spot()
				.toLocation(world(track)));
	}

	/**
	 * Puts the player back on the spawn and starts their run over, as the restart item does.
	 */
	public void restart(@NotNull final Player player, @NotNull final GameSession session) {
		session.resetToSpawn();
		sendTo(player, session, spawnLocation(session.getTrack()));
	}

	/**
	 * Where a player who died on a track comes back to life.
	 */
	@NotNull
	public Location respawnLocation(@NotNull final GameSession session) {
		return backTarget(session);
	}

	// ======= Joining and leaving =======

	/**
	 * Sends a player to a running track's spawn and into its game, as a command or the menu does.
	 */
	public void enter(@NotNull final Player player, @NotNull final Track track) {
		if (sessions.containsKey(player.getUniqueId())) {
			leave(player, LeaveReason.TELEPORT);
		}
		final GameSession session = join(player, track, Entry.DIRECT);
		if (session != null) {
			sendTo(player, session, spawnLocation(track));
		}
	}

	/**
	 * Starts a game for the player: their state goes into a snapshot, they get the track's effects.
	 */
	/**
	 * Starts a game for the player, when they may enter and pay the fee: their state goes into a snapshot, they get
	 * the track's effects.
	 *
	 * @return the session, or {@code null} when the player was refused and told why
	 */
	@Nullable
	private GameSession join(@NotNull final Player player, @NotNull final Track track, @NotNull final Entry entry) {
		final String permission = track.getOptions().getPermission();
		if (permission != null && !player.hasPermission(permission)) {
			plugin.getMessages().send(player, Message.NO_TRACK_PERMISSION, track(track));
			return null;
		}
		final double fee = chargeFee(player, track, entry);
		if (fee < 0) {
			return null;
		}
		final PersistentDataContainer data = player.getPersistentDataContainer();
		// A snapshot left from before (a crash) is the player's real state; never overwrite it with game items
		if (!data.has(snapshotKey, PersistentDataType.STRING)) {
			data.set(snapshotKey, PersistentDataType.STRING, Snapshot.of(player).toString());
		}
		data.set(sessionKey, PersistentDataType.STRING, track.getId().toString());
		player.getInventory().clear();
		player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
		track.getEffects().forEach(effect -> applyEffect(player, effect));
		player.setHealth(Snapshot.maxHealth(player));
		player.setFoodLevel(FULL_FOOD);
		player.setSaturation(FULL_FOOD);
		player.setFlying(false);
		player.setAllowFlight(false);
		player.setLevel(0);
		player.setExp(0);
		player.setGliding(false);
		items.give(player);
		final GameSession session = new GameSession(player.getUniqueId(), track);
		session.setPaidFee(fee);
		sessions.put(player.getUniqueId(), session);
		// Those hiding the others hide the newcomer too
		sessions.values()
				.stream()
				.filter(other -> other != session && other.isHiding() && other.getTrack().equals(track))
				.map(other -> plugin.getServer().getPlayer(other.getPlayer()))
				.filter(Objects::nonNull)
				.forEach(hider -> hider.hidePlayer(plugin, player));
		if (entry != Entry.SIDE) {
			plugin.getMessages().send(player, Message.JOINED, track(track), displayName(track));
		}
		refreshSidebar(player, session);
		return session;
	}

	/**
	 * Takes the track's fee, on every way in but coming back after a disconnect; nothing without an economy.
	 *
	 * @return the fee taken, 0 for none, or -1 when the player cannot afford it and was told so
	 */
	private double chargeFee(@NotNull final Player player, @NotNull final Track track, @NotNull final Entry entry) {
		final double fee = track.getOptions().getFee();
		final Bank bank = plugin.getBank();
		if (fee <= 0 || entry == Entry.RETURN || !bank.isAvailable()) {
			return 0;
		}
		final TagResolver amount = Placeholder.unparsed("amount", bank.format(fee));
		if (!bank.withdraw(player, fee)) {
			plugin.getMessages().send(player, Message.FEE_TOO_HIGH, track(track), amount);
			return -1;
		}
		plugin.getMessages().send(player, Message.FEE_PAID, track(track), amount);
		return fee;
	}

	/**
	 * Sends a player refused at the track's edge to the lobby, so they do not stand in its region.
	 */
	private void turnAway(@NotNull final Player player, @NotNull final Track track) {
		lobby(track).ifPresent(lobby -> teleport(player, lobby));
	}

	private static void applyEffect(@NotNull final Player player, @NotNull final TrackEffect effect) {
		final NamespacedKey key = NamespacedKey.fromString(effect.type());
		final PotionEffectType type = key == null ? null : Registry.EFFECT.get(key);
		if (type != null) {
			player.addPotionEffect(new PotionEffect(type, PotionEffect.INFINITE_DURATION, effect.amplifier()));
		}
	}

	/**
	 * Ends the player's game and gives them back what they had.
	 */
	public void leave(@NotNull final Player player, @NotNull final LeaveReason reason) {
		final GameSession session = sessions.get(player.getUniqueId());
		if (session == null) {
			return;
		}
		// Hiding ends with the game, both ways
		otherPlayers(session).forEach(other -> {
			player.showPlayer(plugin, other);
			if (sessions.get(other.getUniqueId()).isHiding()) {
				other.showPlayer(plugin, player);
			}
		});
		removeBoat(player, session);
		plugin.getSidebar().hide(player);
		sessions.remove(player.getUniqueId());
		if (session.getFinishTask() != -1) {
			plugin.getServer().getScheduler().cancelTask(session.getFinishTask());
		}
		restore(player);
		if (reason == LeaveReason.STOPPED && session.getPaidFee() > 0 && plugin.getSettings().refundOnStop()
				&& plugin.getBank().deposit(player, session.getPaidFee())) {
			plugin.getMessages().send(player, Message.FEE_REFUNDED, track(session.getTrack()),
					Placeholder.unparsed("amount", plugin.getBank().format(session.getPaidFee())));
		}
		if (reason != LeaveReason.DISCONNECT) {
			player.getPersistentDataContainer().remove(sessionKey);
			plugin.getMessages().send(player, reason == LeaveReason.STOPPED ? Message.TRACK_STOPPED : Message.LEFT,
					track(session.getTrack()));
		}
		if (reason.isToLobby()) {
			lobby(session.getTrack()).ifPresent(lobby -> teleport(player, lobby));
		}
	}

	/**
	 * Gives the player back the state of their snapshot, if there is one.
	 */
	private void restore(@NotNull final Player player) {
		final PersistentDataContainer data = player.getPersistentDataContainer();
		final String snapshot = data.get(snapshotKey, PersistentDataType.STRING);
		if (snapshot == null) {
			return;
		}
		try {
			Snapshot.parse(snapshot).restore(player);
			data.remove(snapshotKey);
		} catch (final IllegalArgumentException ex) {
			// Kept, so an admin can still recover the items by hand
			plugin.getLogger().severe("Could not give " + player.getName() + " back their state: " + ex.getMessage());
		}
	}

	/**
	 * Ends every game on the track, as stopping it does.
	 */
	public void stop(@NotNull final Track track) {
		sessions.values()
				.stream()
				.filter(session -> session.getTrack().equals(track))
				.map(GameSession::getPlayer)
				.toList()
				.forEach(id -> Optional.ofNullable(plugin.getServer().getPlayer(id))
						.ifPresent(player -> leave(player, LeaveReason.STOPPED)));
	}

	/**
	 * A player joining the server, or online when the plugin starts: a snapshot left by a crash is given back, and a
	 * player inside a running track goes to its spawn, without paying when they were in that very game.
	 */
	public void arrive(@NotNull final Player player) {
		final PersistentDataContainer data = player.getPersistentDataContainer();
		restore(player);
		final String marker = data.get(sessionKey, PersistentDataType.STRING);
		data.remove(sessionKey);
		if (!isEligible(player)) {
			return;
		}
		findRunningTrack(player.getLocation()).ifPresent(track -> {
			final boolean returning = track.getId().toString().equals(marker);
			final GameSession session = join(player, track, returning ? Entry.RETURN : Entry.SIDE);
			if (session == null) {
				turnAway(player, track);
				return;
			}
			sendTo(player, session, spawnLocation(track));
			if (!returning) {
				plugin.getMessages().send(player, Message.SENT_TO_SPAWN, track(track));
			}
		});
	}

	/**
	 * Ends every game as if the players disconnected, so they get their state back and return without paying.
	 */
	public void shutdown() {
		List.copyOf(sessions.values()).forEach(session -> Optional.ofNullable(plugin.getServer().getPlayer(session.getPlayer()))
				.ifPresent(player -> leave(player, LeaveReason.DISCONNECT)));
		sessions.clear();
	}

	// ======= The timer =======

	/**
	 * Runs every tick: counts the time of running, not paused runs and shows it.
	 */
	public void tick() {
		final TimerDisplay display = plugin.getSettings().timerDisplay();
		for (final GameSession session : List.copyOf(sessions.values())) {
			final Player player = plugin.getServer().getPlayer(session.getPlayer());
			if (player == null) {
				continue;
			}
			checkSprint(player, session);
			checkBoat(player, session);
			if (session.getTrack().getType() == TrackType.TRAINING) {
				continue;
			}
			if (session.getPhase() == Phase.RUNNING && !session.isPaused()) {
				session.tick();
			}
			// On the spawn the run has not started: the experience bar shows zero, the action bar nothing
			if (display.showsActionBar() && session.getPhase() != Phase.WAITING) {
				player.sendActionBar(plugin.getMessages().get(Message.TIMER,
						Placeholder.unparsed("time", Ticks.format(session.getTicks()))));
			}
			if (display.showsXpBar()) {
				player.setLevel(session.getTicks() / Ticks.PER_SECOND);
				player.setExp((session.getTicks() % Ticks.PER_SECOND) / (float) Ticks.PER_SECOND);
			}
		}
	}

	/**
	 * On a sprint-forced track, a running player who stops sprinting outside the checkpoints for longer than the
	 * grace period goes back.
	 */
	private void checkSprint(@NotNull final Player player, @NotNull final GameSession session) {
		final Track track = session.getTrack();
		if (!track.getOptions().isSprintForced() || session.getPhase() != Phase.RUNNING) {
			return;
		}
		final Location location = player.getLocation();
		final boolean resting = contains(Objects.requireNonNull(track.getSpawn()).area(), location)
				|| contains(Objects.requireNonNull(track.getFinish()).area(), location)
				|| track.getCheckpoints().stream().anyMatch(checkpoint -> contains(checkpoint.area(), location));
		if (session.countNotSprinting(player.isSprinting() || resting) > plugin.getSettings().sprintGraceTicks()) {
			session.countNotSprinting(true);
			plugin.getMessages().send(player, Message.SPRINT_STOPPED);
			goBack(player, session);
		}
	}

	// ======= The sidebar =======

	/**
	 * Shows the player's results on the track in the sidebar, once they come from the database; not on training
	 * tracks, which keep no results, and not when the sidebar is turned off.
	 */
	private void refreshSidebar(@NotNull final Player player, @NotNull final GameSession session) {
		final Track track = session.getTrack();
		if (!plugin.getSettings().scoreboard() || track.getType() == TrackType.TRAINING) {
			return;
		}
		plugin.getResults().playerResult(track.getId(), player.getUniqueId())
				.thenCombine(plugin.getResults().ranked(track.getId(), 1), (result, record) -> {
					plugin.runOnMainThread(() -> {
						if (sessions.get(player.getUniqueId()) == session) {
							showSidebar(player, track, result, record);
						}
					});
					return null;
				});
	}

	private void showSidebar(@NotNull final Player player, @NotNull final Track track,
							 @NotNull final Optional<ResultStore.PlayerResult> result,
							 @NotNull final Optional<ResultStore.Ranked> record) {
		final Component none = plugin.getMessages().get(Message.VALUE_NONE);
		final Component nobody = plugin.getMessages().get(Message.SIGN_NOBODY);
		final Component best = result.<Component>map(found -> Component.text(Ticks.format(found.bestTicks()))).orElse(none);
		final Component recordTime = record.<Component>map(found -> Component.text(Ticks.format(found.ticks()))).orElse(none);
		final Component holder = record.<Component>map(found -> Component.text(playerName(found.player()))).orElse(nobody);
		final Component medal = result
				.flatMap(found -> TrackRules.bestMedal(plugin.getSettings().medals(), track.getMedals(), found.bestTicks()))
				.<Component>map(found -> MiniMessage.miniMessage().deserialize(found.displayName()))
				.orElse(none);
		final TagResolver[] resolvers = {
				track(track), displayName(track),
				Placeholder.component("time", best),
				Placeholder.component("medal", medal),
				Placeholder.unparsed("count", String.valueOf(result.map(ResultStore.PlayerResult::completions).orElse(0)))
		};
		final List<Component> lines = new ArrayList<>();
		lines.add(plugin.getMessages().get(Message.SCOREBOARD_BEST, resolvers));
		lines.add(plugin.getMessages().get(Message.SCOREBOARD_RECORD, track(track), Placeholder.component("time", recordTime),
				Placeholder.component("holder", holder)));
		lines.add(plugin.getMessages().get(Message.SCOREBOARD_MEDAL, resolvers));
		lines.add(plugin.getMessages().get(Message.SCOREBOARD_COMPLETIONS, resolvers));
		// An empty text in messages.yml leaves its line out
		lines.removeIf(line -> PlainTextComponentSerializer.plainText().serialize(line).isEmpty());
		plugin.getSidebar().show(player, plugin.getMessages().get(Message.SCOREBOARD_TITLE, resolvers), lines);
	}

	@NotNull
	private String playerName(@NotNull final UUID id) {
		final OfflinePlayer player = plugin.getServer().getOfflinePlayer(id);
		return player.getName() == null ? id.toString() : player.getName();
	}

	// ======= Boats =======

	/**
	 * Sends the player somewhere on their track: on a boat track, in a new boat standing still.
	 */
	private void sendTo(@NotNull final Player player, @NotNull final GameSession session, @NotNull final Location location) {
		if (!session.getTrack().getOptions().isBoat()) {
			teleport(player, location);
			return;
		}
		final boolean wasBoating = boating;
		boating = true;
		try {
			removeBoat(player, session);
			teleport(player, location);
			final Entity boat = location.getWorld().spawnEntity(location, session.getTrack().getOptions().getBoatType());
			// Never saved with the world, so neither a crash nor a restart leaves boats behind
			boat.setPersistent(false);
			boat.setVelocity(new Vector());
			boat.addPassenger(player);
			session.setBoat(boat.getUniqueId());
		} finally {
			boating = wasBoating;
		}
	}

	/**
	 * Takes the player out of their boat, and out of any other vehicle, and removes the boat.
	 */
	private void removeBoat(@NotNull final Player player, @NotNull final GameSession session) {
		// Called from sendTo too, which is boating already and stays so
		final boolean wasBoating = boating;
		boating = true;
		try {
			player.leaveVehicle();
			if (session.getBoat() != null) {
				Optional.ofNullable(plugin.getServer().getEntity(session.getBoat())).ifPresent(Entity::remove);
				session.setBoat(null);
			}
		} finally {
			boating = wasBoating;
		}
	}

	/**
	 * On a boat track, a player who is not in their boat (it was destroyed, or they got out somehow) goes back.
	 */
	private void checkBoat(@NotNull final Player player, @NotNull final GameSession session) {
		if (!session.getTrack().getOptions().isBoat() || session.getPhase() == Phase.FINISHED || player.isDead()) {
			return;
		}
		final Entity vehicle = player.getVehicle();
		// A removed boat can still hold the player when getting out was cancelled
		if (vehicle == null || !vehicle.isValid() || !vehicle.getUniqueId().equals(session.getBoat())) {
			goBack(player, session);
		}
	}

	// ======= Helpers =======

	/**
	 * Teleports the player, marking the teleport as the plugin's own.
	 */
	public void teleport(@NotNull final Player player, @NotNull final Location location) {
		teleporting = true;
		try {
			player.teleport(location);
		} finally {
			teleporting = false;
		}
	}

	/**
	 * The track's own lobby or the global one, when its world is loaded.
	 */
	@NotNull
	public Optional<Location> lobby(@Nullable final Track track) {
		final WorldSpot lobby = track != null && track.getLobby() != null
				? track.getLobby()
				: plugin.getTrackRegistry().getLobby();
		if (lobby == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(plugin.getServer().getWorld(lobby.world()))
				.map(world -> lobby.spot().toLocation(world));
	}

	@NotNull
	private Location spawnLocation(@NotNull final Track track) {
		return Objects.requireNonNull(track.getSpawn()).spot().toLocation(world(track));
	}

	@NotNull
	private World world(@NotNull final Track track) {
		return Objects.requireNonNull(plugin.getServer().getWorld(track.getWorld()), "World of " + track + " is not loaded");
	}

	private static boolean isInRegion(@NotNull final Track track, @NotNull final Location location) {
		return location.getWorld() != null
				&& location.getWorld().getName().equals(track.getWorld())
				&& contains(track.getRegion(), location);
	}

	private static boolean contains(@NotNull final Cuboid area, @NotNull final Location location) {
		return area.contains(location.getX(), location.getY(), location.getZ());
	}

	@NotNull
	private static TagResolver track(@NotNull final Track track) {
		return Placeholder.unparsed("track", track.getName());
	}

	@NotNull
	private static TagResolver displayName(@NotNull final Track track) {
		return Placeholder.component("display-name",
				MiniMessage.miniMessage().deserialize(track.getDisplayName()));
	}
}
