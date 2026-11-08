package dev.egorgrigorciuc.forzammilmod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Pure clock: a player can start an emote only every few seconds. */
final class EmoteCooldown {

    static final long COOLDOWN_MILLIS = 3000L;

    private final long cooldownMillis;
    private final Map<UUID, Long> startedAt = new HashMap<>();

    EmoteCooldown() {
        this(COOLDOWN_MILLIS);
    }

    EmoteCooldown(long cooldownMillis) {
        this.cooldownMillis = cooldownMillis;
    }

    /** Whole seconds still to wait, rounded up. 0 means the player may start an emote. */
    int secondsLeft(UUID player, long nowMillis) {
        Long t = startedAt.get(player);
        if (t == null) return 0;
        long left = t + cooldownMillis - nowMillis;
        if (left <= 0) return 0;
        return (int) ((left + 999) / 1000);
    }

    void markStarted(UUID player, long nowMillis) {
        startedAt.put(player, nowMillis);
    }

    void forget(UUID player) {
        startedAt.remove(player);
    }
}
