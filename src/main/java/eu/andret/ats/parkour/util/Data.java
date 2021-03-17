/*
 * Copyright Andret (c) 2019. Copying and modifying allowed only keeping git link reference.
 */
package eu.andret.ats.parkour.util;

import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

/**
 * The class that's only purpose is to hold util.
 */
public final class Data {
	/**
	 * The table's name in database, that will keep times.
	 */
	public static final String TABLE_RECORDS = "ats_parkour_records";
	/**
	 * The name that will be shown on scoreboard.
	 */
	public static final String SCOREBOARD_NAME = "ParkourLand";
	/**
	 * List of potion effects that can be applied to a region.
	 */
	public static final List<PotionEffectType> ALLOWED_EFFECTS = new ArrayList<>();

	/**
	 * Private constructor.
	 */
	private Data() {
	}

	static {
		ALLOWED_EFFECTS.add(PotionEffectType.SPEED);
		ALLOWED_EFFECTS.add(PotionEffectType.SLOW);
		ALLOWED_EFFECTS.add(PotionEffectType.JUMP);
		ALLOWED_EFFECTS.add(PotionEffectType.CONFUSION);
		ALLOWED_EFFECTS.add(PotionEffectType.FIRE_RESISTANCE);
		ALLOWED_EFFECTS.add(PotionEffectType.WATER_BREATHING);
		ALLOWED_EFFECTS.add(PotionEffectType.INVISIBILITY);
		ALLOWED_EFFECTS.add(PotionEffectType.BLINDNESS);
		ALLOWED_EFFECTS.add(PotionEffectType.NIGHT_VISION);
	}
}
