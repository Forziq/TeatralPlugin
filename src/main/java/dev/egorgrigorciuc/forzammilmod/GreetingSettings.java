package dev.egorgrigorciuc.forzammilmod;

import org.bukkit.configuration.ConfigurationSection;

import java.util.Locale;
import java.util.function.Consumer;

/**
 * The greeting emote's id, names and sound, as Egor set them in config.yml. Plain values, so it can be tested
 * without a server. The animation inside the model is always called {@link #ANIMATION}, whatever the id is.
 */
record GreetingSettings(String id, String name, String nameRu, String sound, float volume, float pitch) {

    static final String ANIMATION = "greeting";
    static final String DEFAULT_ID = "greeting";
    static final String DEFAULT_NAME = "Greeting";
    static final String DEFAULT_NAME_RU = "Приветствие";
    static final String DEFAULT_SOUND = "forzammilmod:emote_whoosh";
    static final float MAX_VOLUME = 4f;
    static final float MIN_PITCH = 0.5f;
    static final float MAX_PITCH = 2f;

    static GreetingSettings defaults() {
        return new GreetingSettings(DEFAULT_ID, DEFAULT_NAME, DEFAULT_NAME_RU, DEFAULT_SOUND, 1f, 1f);
    }

    /** Reads the "greeting" section. A null section (or missing keys) gives the defaults. */
    static GreetingSettings from(ConfigurationSection section, Consumer<String> warn) {
        if (section == null) return defaults();
        return from(section.getString("id", DEFAULT_ID), section.getString("name", DEFAULT_NAME),
                section.getString("name-ru", DEFAULT_NAME_RU), section.getString("sound", DEFAULT_SOUND),
                section.getDouble("volume", 1.0), section.getDouble("pitch", 1.0), warn);
    }

    static GreetingSettings from(String id, String name, String nameRu, String sound,
                                 double volume, double pitch, Consumer<String> warn) {
        String cleanId = id == null ? "" : id.trim();
        if (cleanId.isEmpty()) {
            cleanId = DEFAULT_ID;
        } else if (!validId(cleanId)) {
            warn.accept("greeting.id \"" + cleanId + "\" is not allowed (use lowercase letters, digits and _, "
                    + "and not wave or stop). Using \"" + DEFAULT_ID + "\".");
            cleanId = DEFAULT_ID;
        }
        String cleanName = blank(name) ? DEFAULT_NAME : name.trim();
        String cleanRu = blank(nameRu) ? cleanName : nameRu.trim();
        String cleanSound = sound == null ? "" : sound.trim();
        return new GreetingSettings(cleanId, cleanName, cleanRu, cleanSound,
                clampVolume(volume), clampPitch(pitch));
    }

    static boolean validId(String id) {
        if (id == null || id.isEmpty()) return false;
        if (id.equals("wave") || id.equals("stop")) return false;
        return id.equals(id.toLowerCase(Locale.ROOT)) && id.matches("[a-z0-9_]+");
    }

    static float clampVolume(double v) {
        if (Double.isNaN(v) || v <= 0) return 1f;
        return (float) Math.min(v, MAX_VOLUME);
    }

    static float clampPitch(double p) {
        if (Double.isNaN(p)) return 1f;
        return (float) Math.max(MIN_PITCH, Math.min(p, MAX_PITCH));
    }

    boolean silent() {
        return sound.isEmpty();
    }

    /** Name for a language code ("ru" or "en"). */
    String nameFor(String lang) {
        return Lang.RU.equals(lang) ? nameRu : name;
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }
}
