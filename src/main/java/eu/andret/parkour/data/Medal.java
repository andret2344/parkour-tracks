package eu.andret.parkour.data;

import eu.andret.parkour.Parkour;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum Medal {
    PLATINUM(1, 100, Parkour.getInstance().getConfig().getString("medal.platinium")),
    GOLD(2, 80, Parkour.getInstance().getConfig().getString("medal.gold")),
    SILVER(3, 50, Parkour.getInstance().getConfig().getString("medal.silver")),
    BRONZE(4, 20, Parkour.getInstance().getConfig().getString("medal.bronze")),
    NONE(5, Parkour.getInstance().getConfig().getString("medal.none"));

    private final int id, price;
    private final String name;

    Medal(int id, String name) {
        this(id, 0, name);
    }

    public static Medal valueOf(int id) {
        for (Medal m : values()) {
            if (m.id == id) {
                return m;
            }
        }
        return Medal.NONE;
    }
}
