package eu.andret.parkour.util;

import eu.andret.parkour.ParkourPlugin;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * The Enum with medals.
 */
@AllArgsConstructor
@Getter
public enum Medal {
    /**
     * The best medal.
     */
    PLATINUM(100, ParkourPlugin.getInstance().getConfig().getString("medal.platinium")),
    /**
     * Second one.
     */
    GOLD(80, ParkourPlugin.getInstance().getConfig().getString("medal.gold")),
    /**
     * Third...
     */
    SILVER(50, ParkourPlugin.getInstance().getConfig().getString("medal.silver")),
    /**
     * Worst one.
     */
    BRONZE(20, ParkourPlugin.getInstance().getConfig().getString("medal.bronze")),
    /**
     * No medal.
     */
    NONE(ParkourPlugin.getInstance().getConfig().getString("medal.none"));

    /**
     * The price for getting exact medal.
     */
    private final int price;
    /**
     * The displayed name of medal.
     */
    private final String name;

    /**
     * @param id number of medal.
     * @return the medal if found, <code>null</code> otherwise.
     */
    public static Medal valueOf(int id) {
        for (Medal m : values()) {
            if (m.ordinal() == id) {
                return m;
            }
        }
        return Medal.NONE;
    }
}
