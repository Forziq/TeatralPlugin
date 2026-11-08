package dev.egorgrigorciuc.forzammilmod;

import org.bukkit.entity.Player;

public final class Access {

    public static final String OWNER_NAME = "Forzammil";

    private Access() {
    }

    public static boolean isOwnerName(String name) {
        return OWNER_NAME.equals(name);
    }

    public static boolean isOwner(Player p) {
        return p != null && isOwnerName(p.getName());
    }
}
