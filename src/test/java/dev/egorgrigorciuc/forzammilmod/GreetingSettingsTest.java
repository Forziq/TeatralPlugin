package dev.egorgrigorciuc.forzammilmod;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GreetingSettingsTest {

    private final List<String> warnings = new ArrayList<>();

    private GreetingSettings parse(String id, String name, String ru, String sound, double vol, double pitch) {
        return GreetingSettings.from(id, name, ru, sound, vol, pitch, warnings::add);
    }

    @Test
    void validValuesPass() {
        GreetingSettings g = parse("hello_2", "Hello", "Привет", "minecraft:entity.player.levelup", 0.5, 1.5);
        assertEquals("hello_2", g.id());
        assertEquals("Hello", g.name());
        assertEquals("Привет", g.nameRu());
        assertEquals("minecraft:entity.player.levelup", g.sound());
        assertEquals(0.5f, g.volume());
        assertEquals(1.5f, g.pitch());
        assertTrue(warnings.isEmpty());
    }

    @Test
    void badIdsFallBackAndWarn() {
        for (String bad : new String[]{"Hello", "two words", "wave", "stop", "a-b", "привет"}) {
            warnings.clear();
            assertEquals("greeting", parse(bad, "G", "Г", "", 1, 1).id(), bad);
            assertEquals(1, warnings.size(), bad);
        }
    }

    @Test
    void blankIdUsesDefaultWithoutWarning() {
        assertEquals("greeting", parse("  ", "G", "Г", "", 1, 1).id());
        assertTrue(warnings.isEmpty());
    }

    @Test
    void blankRussianNameUsesEnglishName() {
        GreetingSettings g = parse("greeting", "Hi there", "  ", "", 1, 1);
        assertEquals("Hi there", g.nameRu());
        assertEquals("Hi there", g.nameFor(Lang.RU));
        assertEquals("Hi there", g.nameFor(Lang.EN));
    }

    @Test
    void namePerLanguage() {
        GreetingSettings g = parse("greeting", "Greeting", "Приветствие", "", 1, 1);
        assertEquals("Greeting", g.nameFor(Lang.EN));
        assertEquals("Приветствие", g.nameFor(Lang.RU));
    }

    @Test
    void blankSoundIsSilent() {
        assertTrue(parse("greeting", "G", "Г", "", 1, 1).silent());
        assertTrue(parse("greeting", "G", "Г", "   ", 1, 1).silent());
        assertTrue(parse("greeting", "G", "Г", null, 1, 1).silent());
        assertFalse(parse("greeting", "G", "Г", "x:y", 1, 1).silent());
    }

    @Test
    void volumeAndPitchAreClamped() {
        assertEquals(1f, parse("greeting", "G", "Г", "", 0, 1).volume());
        assertEquals(1f, parse("greeting", "G", "Г", "", -3, 1).volume());
        assertEquals(1f, parse("greeting", "G", "Г", "", Double.NaN, 1).volume());
        assertEquals(GreetingSettings.MAX_VOLUME, parse("greeting", "G", "Г", "", 100, 1).volume());
        assertEquals(0.5f, parse("greeting", "G", "Г", "", 1, 0.1).pitch());
        assertEquals(2f, parse("greeting", "G", "Г", "", 1, 9).pitch());
        assertEquals(1f, parse("greeting", "G", "Г", "", 1, Double.NaN).pitch());
    }

    @Test
    void missingSectionGivesDefaults() {
        assertEquals(GreetingSettings.defaults(), GreetingSettings.from(null, warnings::add));
        assertEquals(GreetingSettings.defaults(), GreetingSettings.from(new YamlConfiguration(), warnings::add));
        assertTrue(warnings.isEmpty());
    }

    @Test
    void readsConfigSection() throws Exception {
        YamlConfiguration y = new YamlConfiguration();
        y.loadFromString("greeting:\n  id: hey\n  name: Hey\n  name-ru: Эй\n  sound: ''\n  volume: 2.0\n  pitch: 0.8\n");
        GreetingSettings g = GreetingSettings.from(y.getConfigurationSection("greeting"), warnings::add);
        assertEquals("hey", g.id());
        assertEquals("Эй", g.nameRu());
        assertTrue(g.silent());
        assertEquals(2f, g.volume());
        assertEquals(0.8f, g.pitch(), 1e-6);
    }

    @Test
    void bundledConfigMatchesDefaults() throws Exception {
        try (var in = getClass().getClassLoader().getResourceAsStream("config.yml")) {
            YamlConfiguration y = YamlConfiguration.loadConfiguration(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8));
            assertEquals(GreetingSettings.defaults(), GreetingSettings.from(y.getConfigurationSection("greeting"), warnings::add));
        }
    }
}
