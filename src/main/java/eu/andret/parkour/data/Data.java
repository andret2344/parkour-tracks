package eu.andret.parkour.data;

import eu.andret.parkour.Parkour;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

public final class Data {
    public static final String url = Parkour.getInstance().getConfig().getString("connection.url");
    public static final String user = Parkour.getInstance().getConfig().getString("connection.user");
    public static final String pass = Parkour.getInstance().getConfig().getString("connection.pass");
    public static final String dbname = Parkour.getInstance().getConfig().getString("connection.dbname");
    public static final String recordstable = "ats_parkour_records";
    public static final String scoreboardName = "ParkourLand";
    public static final List<PotionEffectType> allowedEffects = new ArrayList<>();

    static {
        allowedEffects.add(PotionEffectType.SPEED);
        allowedEffects.add(PotionEffectType.SLOW);
        allowedEffects.add(PotionEffectType.JUMP);
        allowedEffects.add(PotionEffectType.CONFUSION);
        allowedEffects.add(PotionEffectType.FIRE_RESISTANCE);
        allowedEffects.add(PotionEffectType.WATER_BREATHING);
        allowedEffects.add(PotionEffectType.INVISIBILITY);
        allowedEffects.add(PotionEffectType.BLINDNESS);
        allowedEffects.add(PotionEffectType.NIGHT_VISION);
    }
}

//&aPrzeteleportowano na parkour: &2&lEASY1
