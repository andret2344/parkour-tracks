package eu.andret.parkour.region;

import com.sk89q.worldedit.regions.CuboidRegion;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bukkit.World;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class EffectRegion extends AbstractRegion {
    private static final String KEY_EFFECTS_TO_ADD = "effectsToAdd";
    private static final String KEY_EFFECTS_TO_DEL = "effectsToDel";

    private final List<PotionEffectType> effectsToAdd = new ArrayList<>();
    private final List<PotionEffectType> effectsToDel = new ArrayList<>();

    public EffectRegion(CuboidRegion cuboidregion, List<PotionEffectType> effectsToAdd, List<PotionEffectType> effectsToDel) {
        this(cuboidregion);
        this.effectsToAdd.addAll(effectsToAdd);
        this.effectsToDel.addAll(effectsToDel);
    }

    public EffectRegion(World world, List<PotionEffectType> effectsToAdd, List<PotionEffectType> effectsToDel) {
        super(world);
        this.effectsToAdd.addAll(effectsToAdd);
        this.effectsToDel.addAll(effectsToDel);
    }

    public EffectRegion(CuboidRegion cuboidregion) {
        super(cuboidregion);
    }

    public EffectRegion(World world) {
        super(world);
    }

    public void addEffectToAdd(PotionEffectType effect) {
        effectsToAdd.add(effect);
    }

    public boolean delEffectToAdd(PotionEffectType effect) {
        return effectsToAdd.remove(effect);
    }

    public void addEffectToDel(PotionEffectType effect) {
        effectsToDel.add(effect);
    }

    public boolean delEffectToDel(PotionEffectType effect) {
        return effectsToDel.remove(effect);
    }

    @Override
    public Map<String, Object> toYmlStructure() {
        Map<String, Object> map = super.toYmlStructure();
        List<String> s = new ArrayList<>();
        for (PotionEffectType p : effectsToAdd) {
            s.add(p.getName());
        }
        map.put(KEY_EFFECTS_TO_ADD, s);
        s = new ArrayList<>();
        for (PotionEffectType p : effectsToDel) {
            s.add(p.getName());
        }
        map.put(KEY_EFFECTS_TO_DEL, s);
        return map;
    }

    @Override
    public void fromYmlStructure(Map<String, Object> structure) {
        super.fromYmlStructure(structure);
        for (String s : (List<String>) structure.get(KEY_EFFECTS_TO_ADD)) {
            effectsToAdd.add(PotionEffectType.getByName(s));
        }
        for (String s : (List<String>) structure.get(KEY_EFFECTS_TO_DEL)) {
            effectsToDel.add(PotionEffectType.getByName(s));
        }
    }
}
