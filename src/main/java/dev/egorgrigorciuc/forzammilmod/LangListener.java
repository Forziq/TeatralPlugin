package dev.egorgrigorciuc.forzammilmod;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLocaleChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class LangListener implements Listener {

    private final Lang lang;

    public LangListener(Lang lang) {
        this.lang = lang;
    }

    @EventHandler
    public void onLocale(PlayerLocaleChangeEvent e) {
        lang.setLocale(e.getPlayer().getUniqueId(), e.getLocale());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        lang.forget(e.getPlayer().getUniqueId());
    }
}
