/*
 * Copyright Andret (c) 2019. Copying and modifying allowed only keeping git link reference.
 */
package eu.andret.ats.parkour.region;

import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.bukkit.World;
import org.bukkit.potion.PotionEffectType;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = true)
public class EffectRegion extends AbstractRegion {
	private static final String KEY_EFFECTS_TO_ADD = "effectsToAdd";
	private static final String KEY_EFFECTS_TO_DEL = "effectsToDel";

	private final List<PotionEffectType> effectsToAdd = new ArrayList<>();
	private final List<PotionEffectType> effectsToDel = new ArrayList<>();

	public EffectRegion(final CuboidRegion cuboidregion, final List<PotionEffectType> effectsToAdd, final List<PotionEffectType> effectsToDel) {
		this(cuboidregion);
		this.effectsToAdd.addAll(effectsToAdd);
		this.effectsToDel.addAll(effectsToDel);
	}

	public EffectRegion(final World world, final List<PotionEffectType> effectsToAdd, final List<PotionEffectType> effectsToDel) {
		this(new CuboidRegion(new BukkitWorld(world), BlockVector3.ZERO, BlockVector3.ZERO));
		this.effectsToAdd.addAll(effectsToAdd);
		this.effectsToDel.addAll(effectsToDel);
	}

	public EffectRegion(final CuboidRegion cuboidregion) {
		super(cuboidregion);
	}

	public EffectRegion(final World world) {
		this(new CuboidRegion(new BukkitWorld(world), BlockVector3.ZERO, BlockVector3.ZERO));
	}

	public void addEffectToAdd(final PotionEffectType effect) {
		effectsToAdd.add(effect);
	}

	public boolean delEffectToAdd(final PotionEffectType effect) {
		return effectsToAdd.remove(effect);
	}

	public void addEffectToDel(final PotionEffectType effect) {
		effectsToDel.add(effect);
	}

	public boolean delEffectToDel(final PotionEffectType effect) {
		return effectsToDel.remove(effect);
	}

	@Override
	public JSONObject toJSON() {
		final JSONObject object = super.toJSON();
		final JSONArray jsonArrayToAdd = new JSONArray();
		for (final PotionEffectType p : effectsToAdd) {
			jsonArrayToAdd.put(p.getName());
		}
		object.put(KEY_EFFECTS_TO_ADD, jsonArrayToAdd);
		final JSONArray jsonArrayToDel = new JSONArray();
		for (final PotionEffectType p : effectsToDel) {
			jsonArrayToDel.put(p.getName());
		}
		object.put(KEY_EFFECTS_TO_DEL, jsonArrayToDel);
		return object;
	}

	@Override
	public void fromJSON(final JSONObject object) {
		super.fromJSON(object);
		object.getJSONArray(KEY_EFFECTS_TO_ADD)
				.toList()
				.stream()
				.map(String::valueOf)
				.map(PotionEffectType::getByName)
				.forEach(effectsToAdd::add);
		object.getJSONArray(KEY_EFFECTS_TO_DEL)
				.toList()
				.stream()
				.map(String::valueOf)
				.map(PotionEffectType::getByName)
				.forEach(effectsToDel::add);
	}
}
