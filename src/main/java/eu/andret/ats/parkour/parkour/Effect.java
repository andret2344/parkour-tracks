/*
 * Copyright Andret (c) 2018-2022. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.parkour;

import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

public record Effect(@NotNull PotionEffectType effectType, int amplifier) {
	@NotNull
	public PotionEffect toPotionEffect() {
		return new PotionEffect(effectType, 99999999, amplifier);
	}
}
