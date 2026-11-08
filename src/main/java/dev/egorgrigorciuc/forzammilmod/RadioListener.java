package dev.egorgrigorciuc.forzammilmod;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class RadioListener implements Listener {

    private final RadioService radio;

    public RadioListener(RadioService radio) {
        this.radio = radio;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        radio.sendJoinInfo(e.getPlayer());
    }
}
