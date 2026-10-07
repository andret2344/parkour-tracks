package eu.andret.parkourtracks.selection;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import eu.andret.parkourtracks.track.Cuboid;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Reads selections from WorldEdit (or FAWE). The only class that touches the WorldEdit API.
 */
public final class WorldEditSelections implements Selections {
	@NotNull
	@Override
	public Selection get(@NotNull final Player player) {
		final LocalSession session = WorldEdit.getInstance().getSessionManager().get(BukkitAdapter.adapt(player));
		final World world = session.getSelectionWorld();
		if (world == null) {
			throw new SelectionException(SelectionException.Reason.INCOMPLETE);
		}
		final Region region;
		try {
			region = session.getSelection(world);
		} catch (final IncompleteRegionException _) {
			throw new SelectionException(SelectionException.Reason.INCOMPLETE);
		}
		if (!(region instanceof CuboidRegion)) {
			throw new SelectionException(SelectionException.Reason.NOT_CUBOID);
		}
		final BlockVector3 min = region.getMinimumPoint();
		final BlockVector3 max = region.getMaximumPoint();
		return new Selection(world.getName(), new Cuboid(min.x(), min.y(), min.z(), max.x(), max.y(), max.z()));
	}
}
