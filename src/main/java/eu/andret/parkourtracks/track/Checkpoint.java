package eu.andret.parkourtracks.track;

import org.jetbrains.annotations.NotNull;

/**
 * A region that saves the player's progress, and the spot a player going back to it lands on.
 */
public record Checkpoint(@NotNull Cuboid area, @NotNull Spot spot) {
}
