package dev.egorgrigorciuc.forzammilmod;

import java.util.ArrayList;
import java.util.List;

/**
 * Which commands the help list shows. Pure data and filtering, no Bukkit types. Hidden commands (/fmod debug)
 * are deliberately not in here. The text of each line is the lang key "help.cmd.{id}".
 */
final class HelpList {

    /** The greeting emote's id is configurable, so its command text carries this token. */
    static final String GREETING_TOKEN = "{greeting}";

    record Entry(String id, String command, boolean ownerOnly) {
    }

    static final List<Entry> ALL = List.of(
            new Entry("help", "/fmod help", false),
            new Entry("guide", "/fmod guide", false),
            new Entry("emote-wave", "/emote wave", false),
            new Entry("emote-greeting", "/emote " + GREETING_TOKEN, false),
            new Entry("emote-stop", "/emote stop", false),
            new Entry("music", "/fmod music play|stop|shuffle|loop|list", false),
            new Entry("vehicle-list", "/fmod vehicle list", false),
            new Entry("vehicle-spawn", "/fmod vehicle spawn <type>", true),
            new Entry("vehicle-remove", "/fmod vehicle remove", true),
            new Entry("pack", "/fmod pack", false),
            new Entry("version", "/fmod version", false),
            new Entry("update", "/fmod update", true),
            new Entry("reload", "/fmod reload", true));

    private HelpList() {
    }

    /** The entries a caller may use, in display order. */
    static List<Entry> visibleTo(boolean owner) {
        List<Entry> out = new ArrayList<>();
        for (Entry e : ALL) if (owner || !e.ownerOnly()) out.add(e);
        return out;
    }

    /** The command text with the configured greeting id filled in. */
    static String commandText(Entry e, String greetingId) {
        return e.command().replace(GREETING_TOKEN, greetingId);
    }

    static String langKey(Entry e) {
        return "help.cmd." + e.id();
    }
}
