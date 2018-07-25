package eu.andret.parkour.region;

import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.AbstractWorld;
import lombok.Value;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Value
public class EffectRegion extends AbstractRegion {
    private final List<PotionEffectType> effectsToAdd = new ArrayList<>();
    private final List<PotionEffectType> effectsToRemove = new ArrayList<>();

    public EffectRegion(CuboidRegion cuboidregion, List<PotionEffectType> effectsToAdd, List<PotionEffectType> effectsToRemove) {
        super(cuboidregion);
        this.effectsToAdd.addAll(effectsToAdd);
        this.effectsToRemove.addAll(effectsToRemove);
    }

    public EffectRegion(AbstractWorld l, Map<String, Object> m) {
        super(l, m);
        for (String s : (List<String>) m.get("effectsToAdd")) {
            effectsToAdd.add(PotionEffectType.getByName(s));
        }
        for (String s : (List<String>) m.get("effectsToRemove")) {
            effectsToRemove.add(PotionEffectType.getByName(s));
        }
    }

    public void addAdditableEffect(PotionEffectType effect) {
        effectsToAdd.add(effect);
    }

    public boolean removeAdditableEffect(PotionEffectType effect) {
        return effectsToAdd.remove(effect);
    }

    public void addRemovableEffect(PotionEffectType effect) {
        effectsToRemove.add(effect);
    }

    public boolean removeRemovableEffects(PotionEffectType effect) {
        return effectsToRemove.remove(effect);
    }

    @Override
    public Map<String, Object> toYamlStructure() {
        Map<String, Object> map = super.toYamlStructure();
        List<String> s = new ArrayList<>();
        for (PotionEffectType p : effectsToAdd) {
            s.add(p.getName());
        }
        map.put("effectsToAdd", s);
        s = new ArrayList<>();
        for (PotionEffectType p : effectsToRemove) {
            s.add(p.getName());
        }
        map.put("effectsToRemove", s);
        return map;
    }
}
