package eu.andret.parkourtracks.game;

import eu.andret.parkourtracks.ParkourTracksPlugin;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * The edit lock on the world: while a track runs, no block in its region changes, whoever or whatever tries.
 */
public final class TrackGuard implements Listener {
	@NotNull
	private final ParkourTracksPlugin plugin;

	public TrackGuard(@NotNull final ParkourTracksPlugin plugin) {
		this.plugin = plugin;
	}

	/**
	 * Whether the block lies in the region of a running track.
	 */
	public boolean isLocked(@NotNull final Block block) {
		return plugin.getTrackRegistry().getTracks()
				.stream()
				.filter(track -> track.isRunning() && track.getWorld().equals(block.getWorld().getName()))
				.anyMatch(track -> track.getRegion().contains(block.getX(), block.getY(), block.getZ()));
	}

	private void lock(@NotNull final Cancellable event, @NotNull final Block block) {
		if (isLocked(block)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void breakBlock(@NotNull final BlockBreakEvent event) {
		lock(event, event.getBlock());
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void place(@NotNull final BlockPlaceEvent event) {
		lock(event, event.getBlock());
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void bucketEmpty(@NotNull final PlayerBucketEmptyEvent event) {
		lock(event, event.getBlock());
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void bucketFill(@NotNull final PlayerBucketFillEvent event) {
		lock(event, event.getBlock());
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void burn(@NotNull final BlockBurnEvent event) {
		lock(event, event.getBlock());
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void ignite(@NotNull final BlockIgniteEvent event) {
		lock(event, event.getBlock());
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void spread(@NotNull final BlockSpreadEvent event) {
		lock(event, event.getBlock());
	}

	/**
	 * Ice and snow melting, coral dying.
	 */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void fade(@NotNull final BlockFadeEvent event) {
		lock(event, event.getBlock());
	}

	/**
	 * Water freezing, snow falling, concrete powder hardening.
	 */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void form(@NotNull final BlockFormEvent event) {
		lock(event, event.getBlock());
	}

	/**
	 * Liquids flowing, and dragon eggs teleporting.
	 */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void flow(@NotNull final BlockFromToEvent event) {
		lock(event, event.getToBlock());
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void leaves(@NotNull final LeavesDecayEvent event) {
		lock(event, event.getBlock());
	}

	/**
	 * Endermen, falling blocks, trampled farmland, sheep eating grass.
	 */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void entityChange(@NotNull final EntityChangeBlockEvent event) {
		lock(event, event.getBlock());
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void entityExplode(@NotNull final EntityExplodeEvent event) {
		event.blockList().removeIf(this::isLocked);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void blockExplode(@NotNull final BlockExplodeEvent event) {
		event.blockList().removeIf(this::isLocked);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void pistonExtend(@NotNull final BlockPistonExtendEvent event) {
		lockPiston(event, event.getBlock(), event.getBlocks(), event.getDirection());
	}

	/**
	 * A retracting piston faces {@link BlockPistonRetractEvent#getDirection()}; the blocks it pulls move the other way.
	 */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void pistonRetract(@NotNull final BlockPistonRetractEvent event) {
		lockPiston(event, event.getBlock(), event.getBlocks(), event.getDirection().getOppositeFace());
	}

	/**
	 * Locks a piston that moves its head, or a block, into a locked block, or moves a locked block.
	 */
	private void lockPiston(@NotNull final Cancellable event, @NotNull final Block piston,
			@NotNull final List<Block> blocks, @NotNull final BlockFace movement) {
		final boolean locked = isLocked(piston.getRelative(movement))
				|| blocks.stream().anyMatch(block -> isLocked(block) || isLocked(block.getRelative(movement)));
		if (locked) {
			event.setCancelled(true);
		}
	}

	/**
	 * Item frames and paintings.
	 */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void hangingBreak(@NotNull final HangingBreakEvent event) {
		lock(event, event.getEntity().getLocation().getBlock());
	}
}
