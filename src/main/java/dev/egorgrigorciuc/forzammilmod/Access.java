package dev.egorgrigorciuc.forzammilmod;

import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
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

    /** Owner player by exact name, or the server console (console is trusted). */
    public static boolean isOwner(CommandSender s) {
        if (s instanceof Player p) return isOwner(p);
        return s instanceof ConsoleCommandSender;
    }
}
