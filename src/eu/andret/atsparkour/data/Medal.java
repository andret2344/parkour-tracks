package eu.andret.atsparkour.data;

import eu.andret.atsparkour.atsParkour;

public enum Medal {
    PLATINUM(1, 100, atsParkour.getInstance().getConfig().getString("medal.platinium")),
    GOLD(2, 80, atsParkour.getInstance().getConfig().getString("medal.gold")),
    SILVER(3, 50, atsParkour.getInstance().getConfig().getString("medal.silver")),
    BRONZE(4, 20, atsParkour.getInstance().getConfig().getString("medal.bronze")),
    NONE(5, atsParkour.getInstance().getConfig().getString("medal.none"));

    private final int id, price;
    private final String name;

    Medal(int id, int price, String name) {
        this.id = id;
        this.price = price;
        this.name = name;
    }

    Medal(int id, String name) {
        this(id, 0, name);
    }

    public int getId() {
        return id;
    }

    public int getPrice() {
        return price;
    }

    public String getName() {
        return name;
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
