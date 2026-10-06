package eu.andret.parkourtracks.config;

import org.jetbrains.annotations.NotNull;

/**
 * A medal of the server. The key identifies it, in commands and in the payout register: renaming the key makes a new
 * medal. The display name is MiniMessage.
 */
public record Medal(@NotNull String key, @NotNull String displayName) {
}
