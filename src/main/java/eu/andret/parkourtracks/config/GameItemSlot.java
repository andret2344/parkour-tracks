package eu.andret.parkourtracks.config;

import eu.andret.parkourtracks.game.GameItem;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

/**
 * Where a game item sits in the hotbar (0-8) and what it looks like. Items turned off in the config have none.
 */
public record GameItemSlot(@NotNull GameItem item, int slot, @NotNull Material material) {
}
