/*
 * Copyright Andret (c) 2019-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.util;

import lombok.experimental.UtilityClass;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * The class that's only purpose is to hold util.
 */
@UtilityClass
public final class Data {
	/**
	 * List of potion effects that can be applied to a region.
	 */
	@NotNull
	public static final List<PotionEffectType> ALLOWED_EFFECTS = List.of(
			PotionEffectType.SPEED,
			PotionEffectType.SLOW,
			PotionEffectType.JUMP,
			PotionEffectType.CONFUSION,
			PotionEffectType.FIRE_RESISTANCE,
			PotionEffectType.WATER_BREATHING,
			PotionEffectType.INVISIBILITY,
			PotionEffectType.BLINDNESS,
			PotionEffectType.NIGHT_VISION
	);

	/**
	 * List of all available signs variations.
	 */
	@NotNull
	public static final List<Material> SIGNS = List.of(
			Material.ACACIA_SIGN,
			Material.ACACIA_WALL_SIGN,
			Material.BIRCH_SIGN,
			Material.BIRCH_WALL_SIGN,
			Material.CRIMSON_SIGN,
			Material.CRIMSON_WALL_SIGN,
			Material.DARK_OAK_SIGN,
			Material.DARK_OAK_WALL_SIGN,
			Material.JUNGLE_SIGN,
			Material.JUNGLE_WALL_SIGN,
			Material.OAK_SIGN,
			Material.OAK_WALL_SIGN,
			Material.SPRUCE_SIGN,
			Material.SPRUCE_WALL_SIGN,
			Material.WARPED_SIGN,
			Material.WARPED_WALL_SIGN
	);

	/**
	 * List of all available levers variations.
	 */
	@NotNull
	public static final List<Material> LEVERS = List.of(Material.LEVER);

	/**
	 * List of all available doors variations.
	 */
	@NotNull
	public static final List<Material> DOORS = List.of(
			Material.ACACIA_DOOR,
			Material.BIRCH_DOOR,
			Material.CRIMSON_DOOR,
			Material.DARK_OAK_DOOR,
			Material.JUNGLE_DOOR,
			Material.OAK_DOOR,
			Material.SPRUCE_DOOR,
			Material.WARPED_DOOR
	);

	/**
	 * List of all available buttons variations.
	 */
	@NotNull
	public static final List<Material> BUTTONS = List.of(
			Material.STONE_BUTTON,
			Material.ACACIA_BUTTON,
			Material.BIRCH_BUTTON,
			Material.CRIMSON_BUTTON,
			Material.DARK_OAK_BUTTON,
			Material.JUNGLE_BUTTON,
			Material.OAK_BUTTON,
			Material.SPRUCE_BUTTON,
			Material.WARPED_BUTTON
	);

	/**
	 * List of all available trapdoors variations.
	 */
	@NotNull
	public static final List<Material> TRAPDOORS = List.of(
			Material.ACACIA_TRAPDOOR,
			Material.BIRCH_TRAPDOOR,
			Material.CRIMSON_TRAPDOOR,
			Material.DARK_OAK_TRAPDOOR,
			Material.JUNGLE_TRAPDOOR,
			Material.OAK_TRAPDOOR,
			Material.SPRUCE_TRAPDOOR,
			Material.WARPED_TRAPDOOR
	);

	/**
	 * List of all available chests variations.
	 */
	@NotNull
	public static final List<Material> CHESTS = List.of(Material.CHEST);

	/**
	 * List of all available gates variations.
	 */
	@NotNull
	public static final List<Material> GATES = List.of(
			Material.ACACIA_FENCE_GATE,
			Material.BIRCH_FENCE_GATE,
			Material.CRIMSON_FENCE_GATE,
			Material.DARK_OAK_FENCE_GATE,
			Material.JUNGLE_FENCE_GATE,
			Material.OAK_FENCE_GATE,
			Material.SPRUCE_FENCE_GATE,
			Material.WARPED_FENCE_GATE
	);

	/**
	 * List of all available boats variations.
	 */
	@NotNull
	public static final List<Material> BOATS = List.of(
			Material.ACACIA_BOAT,
			Material.BIRCH_BOAT,
			Material.DARK_OAK_BOAT,
			Material.JUNGLE_BOAT,
			Material.OAK_BOAT,
			Material.SPRUCE_BOAT
	);

	/**
	 * @return {@code List<Material>} of all available interactive blocks.
	 */
	@NotNull
	public static List<Material> getInteractiveMaterials() {
		final List<Material> result = new ArrayList<>();
		Stream.of(LEVERS, DOORS, BUTTONS, TRAPDOORS, CHESTS, GATES).forEach(result::addAll);
		return result;
	}
}
