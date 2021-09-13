/*
 * Copyright Andret (c) 2018-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.parkour;

import lombok.Value;
import org.bukkit.potion.PotionEffectType;

@Value
public class ParkourEffect {
	PotionEffectType effectType;
	int amplifier;
}
