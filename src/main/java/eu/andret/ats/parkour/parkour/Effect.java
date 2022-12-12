/*
 * Copyright Andret (c) 2018-2022. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.parkour;

import lombok.Value;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

@Value
public class Effect {
	@NotNull
	PotionEffectType effectType;
	int amplifier;
}
