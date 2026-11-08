package dev.egorgrigorciuc.forzammilmod;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/** Message lookup by client language. Files are read from the jar, never copied to disk. */
public final class Lang {

    public static final String EN = "en";
    public static final String RU = "ru";

    private final YamlConfiguration en;
    private final YamlConfiguration ru;
    private final Logger log;
    private final Set<String> warned = ConcurrentHashMap.newKeySet();
    private final Map<UUID, String> cache = new ConcurrentHashMap<>();

    public Lang(YamlConfiguration en, YamlConfiguration ru, Logger log) {
        this.en = en;
        this.ru = ru;
        this.log = log;
    }

    public static Lang load(JavaPlugin plugin) {
        return new Lang(read(plugin, "lang/en.yml"), read(plugin, "lang/ru.yml"), plugin.getLogger());
    }

    private static YamlConfiguration read(JavaPlugin plugin, String path) {
        try (InputStream in = plugin.getResource(path)) {
            if (in == null) {
                plugin.getLogger().severe("Missing resource in jar: " + path);
                return new YamlConfiguration();
            }
            return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException e) {
            plugin.getLogger().severe("Could not read " + path + ": " + e.getMessage());
            return new YamlConfiguration();
        }
    }

    // ---- language choice ----

    public static String languageForLocale(String locale) {
        return locale != null && locale.toLowerCase(java.util.Locale.ROOT).startsWith("ru") ? RU : EN;
    }

    public String languageOf(CommandSender sender) {
        if (sender instanceof Player p) {
            return cache.computeIfAbsent(p.getUniqueId(), id -> languageForLocale(p.getLocale()));
        }
        return EN;
    }

    public void setLocale(UUID id, String locale) {
        cache.put(id, languageForLocale(locale));
    }

    public void forget(UUID id) {
        cache.remove(id);
    }

    // ---- plurals ----

    /** Returns one / few / many / other for the given language and count. */
    public static String pluralCategory(String lang, long n) {
        if (RU.equals(lang)) {
            long a = Math.abs(n);
            long m10 = a % 10;
            long m100 = a % 100;
            if (m10 == 1 && m100 != 11) return "one";
            if (m10 >= 2 && m10 <= 4 && (m100 < 12 || m100 > 14)) return "few";
            return "many";
        }
        return Math.abs(n) == 1 ? "one" : "other";
    }

    // ---- public API ----

    public String msg(CommandSender sender, String key, Object... pairs) {
        return text(languageOf(sender), key, toMap(pairs));
    }

    public String msg(CommandSender sender, String key, Map<String, ?> placeholders) {
        return text(languageOf(sender), key, placeholders);
    }

    public String plural(CommandSender sender, String key, long n, Object... pairs) {
        return plural(languageOf(sender), key, n, toMap(pairs));
    }

    public String plural(String lang, String key, long n, Map<String, ?> placeholders) {
        Map<String, Object> all = new HashMap<>(placeholders);
        all.putIfAbsent("n", n);
        return text(lang, key + "." + pluralCategory(lang, n), all);
    }

    /** Pure lookup, no sender needed. Falls back ru to en, then to the red key. */
    public String text(String lang, String key, Map<String, ?> placeholders) {
        String raw = RU.equals(lang) ? ru.getString(key) : null;
        if (raw == null) raw = en.getString(key);
        if (raw == null) {
            if (warned.add(key)) log.warning("Missing language key: " + key);
            return ChatColor.RED + key;
        }
        for (Map.Entry<String, ?> e : placeholders.entrySet()) {
            raw = raw.replace("{" + e.getKey() + "}", String.valueOf(e.getValue()));
        }
        return ChatColor.translateAlternateColorCodes('&', raw);
    }

    private static Map<String, Object> toMap(Object[] pairs) {
        if (pairs.length % 2 != 0) throw new IllegalArgumentException("placeholders must be key,value pairs");
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) map.put(String.valueOf(pairs[i]), pairs[i + 1]);
        return map;
    }

}
