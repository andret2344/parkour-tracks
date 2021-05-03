package eu.andret.ats.parkour.sample.provider;

import eu.andret.ats.parkour.api.RankProvider;
import lombok.Value;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Value
public class SampleRankProvider implements RankProvider {
	private static final Random RANDOM = new Random();

	@NotNull
	Map<UUID, Boolean> vips = new HashMap<>();

	@Override
	public boolean isVip(@NotNull final OfflinePlayer player) {
		if (vips.containsKey(player.getUniqueId())) {
			return vips.get(player.getUniqueId());
		}
		// Randomizes because no persistence system is required for demo. Keeping randomized data to nearest reload.
		final boolean value = RANDOM.nextBoolean();
		vips.put(player.getUniqueId(), value);
		return value;
	}
}
