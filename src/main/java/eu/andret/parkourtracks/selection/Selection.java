package eu.andret.parkourtracks.selection;

import eu.andret.parkourtracks.track.Cuboid;
import org.jetbrains.annotations.NotNull;

/**
 * A cuboid a player selected, in the world they selected it in.
 */
public record Selection(@NotNull String world, @NotNull Cuboid cuboid) {
}
