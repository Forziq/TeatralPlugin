package dev.egorgrigorciuc.forzammilmod;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** One radio track. The sound event is {@code forzammilmod:music.<id>}. */
public record Track(String id, String en, String ru, long lengthMs) {

    public String name(String lang) {
        return Lang.RU.equals(lang) && ru != null && !ru.isBlank() ? ru : en;
    }

    public String sound() {
        return "forzammilmod:music." + id;
    }

    /** Parses the {@code tracks:} list of tracks.yml. Entries without an id or a positive length are skipped. */
    public static List<Track> parse(Reader reader) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(reader);
        List<Track> out = new ArrayList<>();
        for (Map<?, ?> m : yaml.getMapList("tracks")) {
            Object id = m.get("id");
            Object len = m.get("length_ms");
            if (id == null || !(len instanceof Number n) || n.longValue() <= 0) continue;
            String en = m.get("en") == null ? id.toString() : m.get("en").toString();
            String ru = m.get("ru") == null ? en : m.get("ru").toString();
            out.add(new Track(id.toString(), en, ru, n.longValue()));
        }
        return out;
    }
}
