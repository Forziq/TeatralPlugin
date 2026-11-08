package dev.egorgrigorciuc.forzammilmod;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LangTest {

    private static YamlConfiguration yaml(String s) {
        YamlConfiguration y = new YamlConfiguration();
        try {
            y.loadFromString(s);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return y;
    }

    private static YamlConfiguration resource(String path) {
        var in = LangTest.class.getClassLoader().getResourceAsStream(path);
        return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
    }

    private final Lang lang = new Lang(
            yaml("hi: 'Hello {name}'\nonly-en: 'English'\ncol: '&aGreen'\nn:\n  one: '{n} cat'\n  other: '{n} cats'\n"),
            yaml("hi: 'Привет, {name}'\nn:\n  one: '{n} кот'\n  few: '{n} кота'\n  many: '{n} котов'\n"),
            Logger.getLogger("test"));

    @Test
    void russianPlurals() {
        long[] ns = {0, 1, 2, 4, 5, 11, 12, 14, 21, 22, 25, 101, 111};
        String[] exp = {"many", "one", "few", "few", "many", "many", "many", "many", "one", "few", "many", "one", "many"};
        for (int i = 0; i < ns.length; i++) {
            assertEquals(exp[i], Lang.pluralCategory("ru", ns[i]), "ru " + ns[i]);
        }
    }

    @Test
    void englishPlurals() {
        assertEquals("one", Lang.pluralCategory("en", 1));
        assertEquals("other", Lang.pluralCategory("en", 0));
        assertEquals("other", Lang.pluralCategory("en", 2));
        assertEquals("other", Lang.pluralCategory("en", 21));
    }

    @Test
    void pluralLookupUsesN() {
        assertEquals("21 кот", lang.plural("ru", "n", 21, Map.of()));
        assertEquals("3 кота", lang.plural("ru", "n", 3, Map.of()));
        assertEquals("5 котов", lang.plural("ru", "n", 5, Map.of()));
        assertEquals("1 cat", lang.plural("en", "n", 1, Map.of()));
        assertEquals("5 cats", lang.plural("en", "n", 5, Map.of()));
    }

    @Test
    void placeholdersReplaced() {
        assertEquals("Hello Bob", lang.text("en", "hi", Map.of("name", "Bob")));
        assertEquals("Привет, Боб", lang.text("ru", "hi", Map.of("name", "Боб")));
    }

    @Test
    void russianFallsBackToEnglish() {
        assertEquals("English", lang.text("ru", "only-en", Map.of()));
    }

    @Test
    void missingEverywhereReturnsKey() {
        String out = lang.text("ru", "nope.key", Map.of());
        assertTrue(out.endsWith("nope.key"));
        assertTrue(out.startsWith("§c"));
        assertTrue(lang.text("en", "nope.key", Map.of()).endsWith("nope.key"));
    }

    @Test
    void colourCodesTranslated() {
        assertEquals("§aGreen", lang.text("en", "col", Map.of()));
    }

    @Test
    void localeChoice() {
        assertEquals("ru", Lang.languageForLocale("ru_ru"));
        assertEquals("en", Lang.languageForLocale("en_us"));
        assertEquals("en", Lang.languageForLocale("de_de"));
        assertEquals("en", Lang.languageForLocale(null));
    }

    @Test
    void missingKeyWarnsOnlyOnce() {
        Logger lg = Logger.getLogger("lang-warn-test");
        lg.setUseParentHandlers(false);
        java.util.concurrent.atomic.AtomicInteger count = new java.util.concurrent.atomic.AtomicInteger();
        lg.addHandler(new java.util.logging.Handler() {
            public void publish(java.util.logging.LogRecord r) { count.incrementAndGet(); }
            public void flush() {}
            public void close() {}
        });
        Lang l = new Lang(yaml("a: 'x'\n"), yaml("a: 'y'\n"), lg);
        l.text("en", "gone", Map.of());
        l.text("ru", "gone", Map.of());
        l.text("en", "gone", Map.of());
        assertEquals(1, count.get());
        l.text("en", "gone2", Map.of());
        assertEquals(2, count.get());
    }

    @Test
    void missingKeyIsRed() {
        assertEquals("\u00a7cnope", lang.text("en", "nope", Map.of()));
    }

    @Test
    void moreLocaleChoices() {
        assertEquals("ru", Lang.languageForLocale("ru_ua"));
        assertEquals("en", Lang.languageForLocale("uk_ua"));
        assertEquals("en", Lang.languageForLocale(""));
    }

    private static Set<String> baseKeys(YamlConfiguration y) {
        Set<String> out = new TreeSet<>();
        for (String k : y.getKeys(true)) {
            if (y.isConfigurationSection(k)) continue;
            out.add(k.replaceFirst("\\.(one|few|many|other)$", ""));
        }
        return out;
    }

    @Test
    void everyVehicleHasANameInBothLanguages() {
        for (String f : new String[] {"lang/en.yml", "lang/ru.yml"}) {
            YamlConfiguration y = resource(f);
            for (VehicleType t : VehicleType.values()) {
                assertTrue(y.isString("vehicle.name." + t.id), f + " missing vehicle.name." + t.id);
            }
        }
    }

    @Test
    void languageFilesHaveSameKeys() {
        assertEquals(baseKeys(resource("lang/en.yml")), baseKeys(resource("lang/ru.yml")));
    }

    @Test
    void pluralKeysHaveRequiredForms() {
        check(resource("lang/en.yml"), Set.of("one", "other"));
        check(resource("lang/ru.yml"), Set.of("one", "few", "many"));
    }

    private static void check(YamlConfiguration y, Set<String> required) {
        for (String k : y.getKeys(true)) {
            if (!y.isConfigurationSection(k)) continue;
            Set<String> kids = new TreeSet<>(y.getConfigurationSection(k).getKeys(false));
            boolean plural = kids.stream().anyMatch(c -> Set.of("one", "few", "many", "other").contains(c));
            if (plural) assertEquals(new TreeSet<>(required), kids, "plural shape of " + k);
        }
    }
}
