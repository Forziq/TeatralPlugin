package dev.egorgrigorciuc.forzammilmod;

/**
 * Keeps a looping sound going. Minecraft cannot loop a sound, so we play it again a couple of ticks before it
 * ends. Pure: no Bukkit types, so it can be unit tested.
 */
final class SoundLoop {

    enum Action { NONE, PLAY, STOP }

    /** Replay this many ticks before the sound ends, so there is no gap. */
    static final int REPLAY_EARLY_TICKS = 2;

    private final int periodTicks;
    private boolean playing;
    private long nextAt;

    SoundLoop(int lengthTicks) {
        this.periodTicks = Math.max(1, lengthTicks - REPLAY_EARLY_TICKS);
    }

    /** PLAY means start or replay now, STOP means stop now (only once), NONE means do nothing. */
    Action update(boolean want, long now) {
        if (want) {
            if (!playing || now >= nextAt) {
                playing = true;
                nextAt = now + periodTicks;
                return Action.PLAY;
            }
            return Action.NONE;
        }
        if (playing) {
            playing = false;
            return Action.STOP;
        }
        return Action.NONE;
    }

    boolean playing() {
        return playing;
    }
}
